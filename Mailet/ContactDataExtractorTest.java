package org.apache.james.transport.mailets;


import com.rabbitmq.client.*;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.mailet.*;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContactDataExtractorTest {

    private static final String EXCHANGE_NAME = "exchangeName";
    private static final String ROUTING_KEY = "routingKey";
    private static final String AMQP_URI = "amqp://host";

    private ContactDataExtractor mailet;
    private MailetContext mailetContext;
    private FakeMailetConfig mailetConfig;

    private MimeMessage message;

    private static final String SENDER = "sender@james.org";
    private static final String TO = "to@james.org";
    private static final String TOBis = "tobis@james.org";
    private static final String CC = "copy@james.org";
    private static final String BCC = "blind@james.org";
    private static final String Text = "This is my text";



    @BeforeEach
    public void setUp() throws Exception {
        message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setSubject("prova");
        message.setText(Text);
        message.saveChanges();


        mailet = new ContactDataExtractor();
        Logger logger = mock(Logger.class);
        mailetContext = FakeMailContext.builder()
                .logger(logger)
                .build();
        mailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailetContext)
                .setProperty("uri", AMQP_URI)
                .setProperty("exchange", EXCHANGE_NAME)
                .setProperty("routing_key", ROUTING_KEY)
                .build();

    }

    @Test
    void initShouldThrowWhenNoUriParameter() {
        FakeMailetConfig customMailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailetContext)
                .build();
        assertThatThrownBy(() -> mailet.init(customMailetConfig))
                .isInstanceOf(MailetException.class);
    }

    @Test
    void initShouldThrowWhenNoExchangeParameter() {
        FakeMailetConfig customMailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailetContext)
                .setProperty("uri", AMQP_URI)
                .build();
        assertThatThrownBy(() -> mailet.init(customMailetConfig))
                .isInstanceOf(MailetException.class);
    }

    @Test
    void initShouldThrowWhenInvalidUri() throws IOException, MessagingException {
        FakeMailetConfig customMailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailetContext)
                .setProperty("uri", "bad-uri")
                .build();
        assertThatThrownBy(() -> mailet.init(customMailetConfig))
                .isInstanceOf(MailetException.class);
    }

    @Test
    void getMailetInfoShouldReturnInfo() {
        assertThat(mailet.getMailetInfo()).isEqualTo("ContactDataExtractor");
    }

    @Test
    void initShouldIntializeEmptyRoutingKeyWhenAllParametersButRoutingKey() throws MessagingException, IOException {
        FakeMailetConfig customMailetConfig = FakeMailetConfig.builder()
                .mailetName("Test")
                .mailetContext(mailetContext)
                .setProperty("uri", AMQP_URI)
                .setProperty("exchange", EXCHANGE_NAME)
                .build();
        mailet.init(customMailetConfig);

        assertThat(mailet.routingKey).isEmpty();
    }

    @Test
    void initShouldNotThrowWithAllParameters() throws MessagingException {
        mailet.init(mailetConfig);
    }

    //
    @Test
    public void serviceShouldNotUseConnectionWhenNoSender() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO, TO);
        Connection connection = mock(Connection.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .recipient(TO)
                .build();

        mailet.service(mail);

        verifyZeroInteractions(connection);
    }

    @Test
    public void serviceShouldNotUseConnectionWhenNoTo() throws Exception {
        mailet.init(mailetConfig);
        Connection connection = mock(Connection.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .build();

        mailet.service(mail);

        verifyZeroInteractions(connection);
    }

    @Test
    public void serviceShouldNotUseConnectionWhenNoText() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO, TO);
        Connection connection = mock(Connection.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .sender(SENDER)
                .recipient(TO)
                .build();

        mailet.service(mail);

        verifyZeroInteractions(connection);
    }

    @Test
    public void serviceShouldNotFailWhenTimeoutException() throws Exception {
        mailet.init(mailetConfig);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenThrow(new TimeoutException());
        mailet.setConnectionFactory(connectionFactory);

        mailet.service(mail);
    }

    @Test
    public void serviceShouldNotFailWhenIOException() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO, TO);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenThrow(new IOException());
        mailet.setConnectionFactory(connectionFactory);

        mailet.service(mail);
    }

    @Test
    public void serviceShouldNotFailWhenAlreadyClosedException() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO, TO);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        ShutdownSignalException shutdownSignalException = new ShutdownSignalException(false, false, new AMQP.Channel.Close.Builder().build(), "reference");
        when(connectionFactory.newConnection()).thenThrow(new AlreadyClosedException(shutdownSignalException));
        mailet.setConnectionFactory(connectionFactory);

        mailet.service(mail);
    }

    @Test
    public void serviceShouldPublishMessage() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO, TO);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldUnscrambleRecipients() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO,"=?UTF-8?Q?Fr=c3=a9d=c3=a9ric_RECIPIENT?= <frecipient@example.com>");

        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + "Frédéric RECIPIENT <frecipient@example.com>" + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + "Frédéric RECIPIENT <frecipient@example.com>" +  "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenMultiPartMail() throws Exception {
        message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setRecipients(Message.RecipientType.TO, TO);
        message.setSubject("prova");
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart bp = new MimeBodyPart();
        bp.setText(Text);
        mp.addBodyPart(bp);
        bp = new MimeBodyPart();
        bp.setText("Questo è un part interno2");
        mp.addBodyPart(bp);
        bp = new MimeBodyPart();
        MimeMessage message2 = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        bp.setContent(message2, "message/rfc822");
        mp.addBodyPart(bp);
        message.setContent(mp);
        message.saveChanges();

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenMultiPartExtMail() throws Exception {
        message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setRecipients(Message.RecipientType.TO, TO);
        message.setSubject("prova");
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart bp = new MimeBodyPart();
        bp.setText(Text);
        mp.addBodyPart(bp);
        bp = new MimeBodyPart();
        bp.setText("Questo è un part interno2");
        mp.addBodyPart(bp);
        bp = new MimeBodyPart();
        MimeMessage message2 = new MimeMessage(Session.getDefaultInstance(new Properties()));
        bp.setContent(message2, "message/rfc822");
        mp.addBodyPart(bp);

        MimeMultipart mpext = new MimeMultipart();
        bp = new MimeBodyPart();
        bp.setContent(mp);
        mpext.addBodyPart(bp);

        message.setContent(mpext);
        message.saveChanges();

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenHTMLMail() throws Exception {
        message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setRecipients(Message.RecipientType.TO, TO);
        message.setSubject("prova");
        message.setContent("<p>Questa è una prova<br />di html</p>",
                "text/html");
        message.saveChanges();

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "]" + "\n" + "Questa è una prova\ndi html";
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "]\"," + "\"text\":\"" + "Questa è una prova\ndi html" + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenMultiPartHTMLMail() throws Exception {
        message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        message.setRecipients(Message.RecipientType.TO, TO);
        message.setSubject("prova");
        MimeMultipart mp = new MimeMultipart();
        MimeBodyPart bp = new MimeBodyPart();
        MimeMessage message2 = new MimeMessage(Session.getDefaultInstance(new Properties()));
        bp.setContent(message2, "message/rfc822");
        mp.addBodyPart(bp);
        bp = new MimeBodyPart();
        bp.setContent("<p>Questa è una prova<br />di html</p>", "text/html");
        mp.addBodyPart(bp);
        message.setContent(mp);
        message.saveChanges();

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "]" + "\n" + "Questa è una prova\ndi html";
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "]\"," + "\"text\":\"" + "Questa è una prova\ndi html" + "\"}";


        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenTOAndCC() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
                //Found by mail.getMessage().getRecipients(Message.RecipientType.TO)
                .addToRecipient(TO)
                .addCcRecipient(CC)
                .setSubject("Contact collection Rocks")
                .setText(Text);

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                //Found by mail.getRecipients()
                .recipient(TO)
                .recipient(CC)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "] [" + CC + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "] [" + CC + "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenTOAndBCC() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
                //Found by mail.getMessage().getRecipients(Message.RecipientType.TO)
                .addToRecipient(TO)
                .addBccRecipient(BCC)
                .setSubject("Contact collection Rocks")
                .setText(Text);

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                //Found by mail.getRecipients()
                .recipient(TO)
                .recipient(BCC)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "] [" + BCC + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "] [" + BCC + "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishMessageWhenTOAndCCAndBCC() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
                //Found by mail.getMessage().getRecipients(Message.RecipientType.TO)
                .addToRecipient(TO)
                .addCcRecipient(CC)
                .addBccRecipient(BCC)
                .setSubject("Contact collection Rocks")
                .setText(Text);

        mailet.init(mailetConfig);
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                //Found by mail.getRecipients()
                .recipient(TO)
                .recipient(CC)
                .recipient(BCC)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + "] [" + CC + "] [" + BCC + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO +  "] [" + CC + "] [" + BCC + "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

    @Test
    public void serviceShouldPublishTwoTO() throws Exception {
        mailet.init(mailetConfig);
        message.setRecipients(Message.RecipientType.TO,"Nom Prenom <" + TO + ">" + ", User2 " + "<" + TOBis + ">");

        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        when(connection.createChannel()).thenReturn(channel);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(connectionFactory.newConnection()).thenReturn(connection);
        mailet.setConnectionFactory(connectionFactory);
        FakeMail mail = FakeMail.builder()
                .name("mail")
                .mimeMessage(message)
                .sender(SENDER)
                .recipient(TO)
                .recipient(TOBis)
                .build();
        AMQP.BasicProperties expectedProperties = new AMQP.BasicProperties();

        mailet.service(mail);

        //String expected = SENDER + "\n" + "[" + TO + ", " + TOBis + "]" + "\n" + Text;
        //String expected = SENDER + "\n" + "[" + "Nom Prenom <" + TO + ">" + ", User2 " + "<" + TOBis + ">" + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + "Nom Prenom <" + TO + ">" + ", User2 " + "<" + TOBis + ">" + "]\"," + "\"text\":\"" + Text + "\"}";

        ArgumentCaptor<AMQP.BasicProperties> basicPropertiesCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel).basicPublish(eq(EXCHANGE_NAME), eq(ROUTING_KEY), basicPropertiesCaptor.capture(), eq(expected.getBytes(StandardCharsets.UTF_8)));
        assertThat(basicPropertiesCaptor.getValue()).isEqualToComparingFieldByField(expectedProperties);
    }

}