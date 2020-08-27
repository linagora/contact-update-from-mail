/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;

import org.apache.james.core.MailAddress;
import org.apache.james.mime4j.util.MimeUtil;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fge.lambdas.Throwing;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ContactDataExtractor extends GenericMailet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactDataExtractor.class);

    public static final String URI_PARAMETER_NAME = "uri";
    public static final String EXCHANGE_PARAMETER_NAME = "exchange";
    public static final String ROUTING_KEY_PARAMETER_NAME = "routing_key";

    public static final String ROUTING_KEY_DEFAULT_VALUE = "";

    private String exchange;
    private ConnectionFactory connectionFactory;
    @VisibleForTesting
    String routingKey;

    private final HashMap<String, String> charMap = new HashMap<>();


    @Override
    public void init() throws MailetException {
        String uri = getInitParameter(URI_PARAMETER_NAME);
        if (Strings.isNullOrEmpty(uri)) {
            throw new MailetException("No value for " + URI_PARAMETER_NAME
                    + " parameter was provided.");
        }
        exchange = getInitParameter(EXCHANGE_PARAMETER_NAME);
        if (Strings.isNullOrEmpty(exchange)) {
            throw new MailetException("No value for " + EXCHANGE_PARAMETER_NAME
                    + " parameter was provided.");
        }
        routingKey = getInitParameter(ROUTING_KEY_PARAMETER_NAME, ROUTING_KEY_DEFAULT_VALUE);

        connectionFactory = new ConnectionFactory();
        try {
            connectionFactory.setUri(uri);
        } catch (Exception e) {
            throw new MailetException("Invalid " + URI_PARAMETER_NAME
                    + " parameter was provided: " + uri, e);
        }

        initEntityTable();

    }

    @VisibleForTesting
    void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void service(Mail mail) throws MailetException {
        try {
            Optional<Stream<byte[]>> payload = extractContacts(mail);
            payload.ifPresent(this::sendContent);
        } catch (Exception e) {
            LOGGER.error("Error while extracting contacts", e);
        }
    }

    Optional<Stream<byte[]>> extractContacts(Mail mail) throws MessagingException, IOException {
        //String sender = mail.getMaybeSender().toString();
        Optional<String> sender = mail.getMaybeSender().asOptional().map(MailAddress::asString);
        String senderB = "";
        if (sender.isPresent()) {
            senderB = sender.get();
        }
        System.out.println("sender : " + senderB);
        //Collection<MailAddress> temp = mail.getRecipients();
        String to = "";
        String cc = "";
        String bcc = "";
        /* if (!temp.isEmpty()) {
            to = mail.getRecipients().toString();
        }*/

        //to = Arrays.toString(mail.getMessage().getRecipients(Message.RecipientType.TO));
        to = MimeUtil.unscrambleHeaderValue(Arrays.toString(InternetAddress.parseHeader(mail.getMessage().getHeader(Message.RecipientType.TO.toString(), ","), false)));
        if (mail.getMessage().getHeader(Message.RecipientType.CC.toString()) != null) {
            cc = MimeUtil.unscrambleHeaderValue(Arrays.toString(InternetAddress.parseHeader(mail.getMessage().getHeader(Message.RecipientType.CC.toString(), ","), false)));
            to += " " + cc;
        }
        if (mail.getMessage().getHeader(Message.RecipientType.BCC.toString()) != null) {
            bcc = MimeUtil.unscrambleHeaderValue(Arrays.toString(InternetAddress.parseHeader(mail.getMessage().getHeader(Message.RecipientType.BCC.toString(), ","), false)));
            to += " " + bcc;
        }
        //String toB = Arrays.toString(mail.getMessage().getRecipients(Message.RecipientType.TO));
        System.out.println("to : " + to);
        String text = getTextFromMessage(mail.getMessage()).trim();
        System.out.println("text : " + text);
        if (!senderB.isEmpty() && !to.equals("null") && !text.isEmpty()) {
            //Stream<byte[]> result = Stream.of((senderB + "\n" + to + "\n" + text).getBytes(StandardCharsets.UTF_8));
            Stream<byte[]> result = Stream.of(("{\"sender\":\"" + senderB + "\",\"recipients\":\"" + to + "\"," + "\"text\":\"" + text + "\"}").getBytes(StandardCharsets.UTF_8));
            return Optional.of(result);
        } else {
            return Optional.empty();
        }
    }

    private void sendContent(Stream<byte[]> content) {
        try {
            trySendContent(content);
        } catch (IOException e) {
            LOGGER.error("IOException while writing to AMQP: {}", e.getMessage(), e);
        } catch (TimeoutException e) {
            LOGGER.error("TimeoutException while writing to AMQP: {}", e.getMessage(), e);
        } catch (AlreadyClosedException e) {
            LOGGER.error("AlreadyClosedException while writing to AMQP: {}", e.getMessage(), e);
        }
    }

    private void trySendContent(Stream<byte[]> content) throws IOException, TimeoutException {
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclarePassive(exchange);
            sendContentOnChannel(channel, content);
        }
    }

    private void sendContentOnChannel(Channel channel, Stream<byte[]> content) throws IOException {
        content.forEach(
                Throwing.consumer(message ->
                        channel.basicPublish(exchange,
                                routingKey,
                                new AMQP.BasicProperties(),
                                message)));
    }

    @Override
    public String getMailetInfo() {
        return "ContactDataExtractor";
    }


    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            Multipart mimeMultipart = (Multipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        } else if (message.isMimeType("text/html")) {
            result = html2Text(message.getContent().toString());
        }
        return result;
    }

    private String getTextFromMimeMultipart(
            Multipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + html2Text(html);
            } else if (bodyPart.getContent() instanceof Multipart) {
                result = result + getTextFromMimeMultipart((Multipart) bodyPart.getContent());
            }
        }
        return result;
    }

    public String html2Text(String html) {
        return decodeEntities(html
                .replaceAll("\\<([bB][rR]|[dD][lL])[ ]*[/]*[ ]*\\>", "\n")
                .replaceAll("\\</([pP]|[hH]5|[dD][tT]|[dD][dD]|[dD][iI][vV])[ ]*\\>", "\n")
                .replaceAll("\\<[lL][iI][ ]*[/]*[ ]*\\>", "\n* ")
                .replaceAll("\\<[dD][dD][ ]*[/]*[ ]*\\>", " - ")
                .replaceAll("\\<.*?\\>", ""));
    }

    public String decodeEntities(String data) {
        StringBuffer buffer = new StringBuffer();
        StringBuilder res = new StringBuilder();
        int lastAmp = -1;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);

            if (c == '&' && lastAmp == -1) {
                lastAmp = buffer.length();
            } else if (c == ';' && (lastAmp > -1)) { // && (lastAmp > (buffer.length() - 7))) { // max: &#xxxx;
                if (charMap.containsKey(buffer.toString())) {
                    res.append(charMap.get(buffer.toString()));
                } else {
                    res.append("&").append(buffer.toString()).append(";");
                }
                lastAmp = -1;
                buffer = new StringBuffer();
            } else if (lastAmp == -1) {
                res.append(c);
            } else {
                buffer.append(c);
            }
        }
        return res.toString();
    }

    private void initEntityTable() {
        for (int index = 11; index < 32; index++) {
            charMap.put("#0" + index, String.valueOf((char) index));
        }
        for (int index = 32; index < 128; index++) {
            charMap.put("#" + index, String.valueOf((char) index));
        }
        for (int index = 128; index < 256; index++) {
            charMap.put("#" + index, String.valueOf((char) index));
        }

        // A complete reference is here:
        // http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references

        charMap.put("#09", "\t");
        charMap.put("#10", "\n");
        charMap.put("#13", "\r");
        charMap.put("#60", "<");
        charMap.put("#62", ">");

        charMap.put("lt", "<");
        charMap.put("gt", ">");
        charMap.put("amp", "&");
        charMap.put("nbsp", " ");
        charMap.put("quot", "\"");

        charMap.put("Ouml", "Ö");
        charMap.put("Oacute", "Ó");
        charMap.put("iquest", "¿");
        charMap.put("yuml", "ÿ");
        charMap.put("cent", "¢");
        charMap.put("deg", "°");
        charMap.put("aacute", "á");
        charMap.put("uuml", "ü");
        charMap.put("Otilde", "Õ");
        charMap.put("Iacute", "Í");
        charMap.put("frac12", "½");
        charMap.put("atilde", "ã");
        charMap.put("ordf", "ª");
        charMap.put("sup2", "²");
        charMap.put("sup3", "³");
        charMap.put("frac14", "¼");
        charMap.put("ucirc", "û");
        charMap.put("brvbar", "¦");
        charMap.put("reg", "®");
        charMap.put("sup1", "¹");
        charMap.put("THORN", "Þ");
        charMap.put("ordm", "º");
        charMap.put("eth", "ð");
        charMap.put("Acirc", "Â");
        charMap.put("aring", "å");
        charMap.put("Uacute", "Ú");
        charMap.put("oslash", "ø");
        charMap.put("eacute", "é");
        charMap.put("agrave", "à");
        charMap.put("Ecirc", "Ê");
        charMap.put("laquo", "«");
        charMap.put("Igrave", "Ì");
        charMap.put("Agrave", "À");
        charMap.put("macr", "¯");
        charMap.put("Ucirc", "Û");
        charMap.put("igrave", "ì");
        charMap.put("ouml", "ö");
        charMap.put("iexcl", "¡");
        charMap.put("otilde", "õ");
        charMap.put("ugrave", "ù");
        charMap.put("Aring", "Å");
        charMap.put("Ograve", "Ò");
        charMap.put("Ugrave", "Ù");
        charMap.put("ograve", "ò");
        charMap.put("acute", "´");
        charMap.put("ecirc", "ê");
        charMap.put("euro", "€");
        charMap.put("uacute", "ú");
        charMap.put("shy", "\\u00AD");
        charMap.put("cedil", "¸");
        charMap.put("raquo", "»");
        charMap.put("Atilde", "Ã");
        charMap.put("Iuml", "Ï");
        charMap.put("iacute", "í");
        charMap.put("ocirc", "ô");
        charMap.put("curren", "¤");
        charMap.put("frac34", "¾");
        charMap.put("Euml", "Ë");
        charMap.put("szlig", "ß");
        charMap.put("pound", "£");
        charMap.put("not", "¬");
        charMap.put("AElig", "Æ");
        charMap.put("times", "×");
        charMap.put("Aacute", "Á");
        charMap.put("Icirc", "Î");
        charMap.put("para", "¶");
        charMap.put("uml", "¨");
        charMap.put("oacute", "ó");
        charMap.put("copy", "©");
        charMap.put("Eacute", "É");
        charMap.put("Oslash", "Ø");
        charMap.put("divid", "÷");
        charMap.put("aelig", "æ");
        charMap.put("euml", "ë");
        charMap.put("Ocirc", "Ô");
        charMap.put("yen", "¥");
        charMap.put("ntilde", "ñ");
        charMap.put("Ntilde", "Ñ");
        charMap.put("thorn", "þ");
        charMap.put("yacute", "ý");
        charMap.put("Auml", "Ä");
        charMap.put("Yacute", "Ý");
        charMap.put("ccedil", "ç");
        charMap.put("micro", "µ");
        charMap.put("Ccedil", "Ç");
        charMap.put("sect", "§");
        charMap.put("icirc", "î");
        charMap.put("middot", "·");
        charMap.put("Uuml", "Ü");
        charMap.put("ETH", "Ð");
        charMap.put("egrave", "è");
        charMap.put("iuml", "ï");
        charMap.put("plusmn", "±");
        charMap.put("acirc", "â");
        charMap.put("auml", "ä");
        charMap.put("Egrave", "È");
    }
}
