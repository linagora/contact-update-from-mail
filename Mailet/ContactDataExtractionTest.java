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

import org.apache.james.MemoryJamesServerMain;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailets.TemporaryJamesServer;
import org.apache.james.mailets.configuration.CommonProcessors;
import org.apache.james.mailets.configuration.MailetConfiguration;
import org.apache.james.mailets.configuration.MailetContainer;
import org.apache.james.mailets.configuration.ProcessorConfiguration;
import org.apache.james.modules.protocols.ImapGuiceProbe;
import org.apache.james.modules.protocols.SmtpGuiceProbe;
import org.apache.james.transport.mailets.amqp.AmqpRule;
import org.apache.james.transport.matchers.All;
import org.apache.james.util.docker.DockerContainer;
import org.apache.james.util.docker.Images;
import org.apache.james.util.docker.RateLimiters;
import org.apache.james.utils.DataProbeImpl;
import org.apache.james.utils.SMTPMessageSender;
import org.apache.james.utils.TestIMAPClient;
import org.apache.mailet.base.test.FakeMail;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.Optional;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.apache.james.mailets.configuration.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ContactDataExtractionTest {
    public static final String SENDER = "sender@" + DEFAULT_DOMAIN;
    public static final String TO = "to@" + DEFAULT_DOMAIN;
    public static final String TO2 = "to2@" + DEFAULT_DOMAIN;
    public static final String CC = "cc@" + DEFAULT_DOMAIN;
    public static final String CC2 = "cc2@" + DEFAULT_DOMAIN;
    public static final String BCC = "bcc@" + DEFAULT_DOMAIN;
    public static final String BCC2 = "bcc2@" + DEFAULT_DOMAIN;
    public static final String Text = "This is my email";

    private static final String EXCHANGE_NAME = "myExchange";
    private static final String ROUTING_KEY = "myRoutingKey";

    public DockerContainer rabbit = DockerContainer.fromName(Images.RABBITMQ)
            .waitingFor(new HostPortWaitStrategy().withRateLimiter(RateLimiters.TWENTIES_PER_SECOND));
    public AmqpRule amqpRule = new AmqpRule(rabbit, EXCHANGE_NAME, ROUTING_KEY);
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public RuleChain chain = RuleChain.outerRule(rabbit).around(amqpRule).around(folder);
    @Rule
    public TestIMAPClient testIMAPClient = new TestIMAPClient();
    @Rule
    public SMTPMessageSender messageSender = new SMTPMessageSender(DEFAULT_DOMAIN);

    private TemporaryJamesServer jamesServer;

    @Before
    public void setup() throws Exception {
        MailetContainer.Builder mailetContainer = TemporaryJamesServer.defaultMailetContainerConfiguration()
                .postmaster(SENDER)
                .putProcessor(ProcessorConfiguration.transport()
                        .addMailet(MailetConfiguration.builder()
                                .matcher(All.class)
                                .mailet(ContactDataExtractor.class)
                                .addProperty(ContactDataExtractor.URI_PARAMETER_NAME, amqpRule.getAmqpUri())
                                .addProperty(ContactDataExtractor.EXCHANGE_PARAMETER_NAME, EXCHANGE_NAME)
                                .addProperty(ContactDataExtractor.ROUTING_KEY_PARAMETER_NAME, ROUTING_KEY))
                        .addMailetsFrom(CommonProcessors.deliverOnlyTransport()));

        jamesServer = TemporaryJamesServer.builder()
                .withBase(MemoryJamesServerMain.SMTP_AND_IMAP_MODULE)
                .withMailetContainer(mailetContainer)
                .build(folder.newFolder());
        jamesServer.start();

        jamesServer.getProbe(DataProbeImpl.class)
                .fluent()
                .addDomain(DEFAULT_DOMAIN)
                .addUser(SENDER, PASSWORD)
                .addUser(TO, PASSWORD)
                .addUser(TO2, PASSWORD)
                .addUser(CC, PASSWORD)
                .addUser(CC2, PASSWORD)
                .addUser(BCC, PASSWORD)
                .addUser(BCC2, PASSWORD);
    }

    @After
    public void tearDown() throws Exception {
        jamesServer.shutdown();
    }

    @Test
    public void recipientsAndTextShouldBePublishedToAmqpWhenSendingEmail() throws Exception {
        MimeMessageBuilder message = MimeMessageBuilder.mimeMessageBuilder()
                .setSender(SENDER)
                .addToRecipient(TO, "John To2 <" + TO2 + ">")
                .addCcRecipient(CC, "John Cc2 <" + CC2 + ">")
                .addBccRecipient(BCC, "John Bcc2 <" + BCC2 + ">")
                .setSubject("Contact collection Rocks")
                .setText(Text);
        messageSender.connect(LOCALHOST_IP, jamesServer.getProbe(SmtpGuiceProbe.class).getSmtpPort())
                .authenticate(SENDER, PASSWORD)
                .sendMessage(FakeMail.builder()
                        .name("name")
                        .mimeMessage(message)
                        .sender(SENDER)
                        .recipients(TO, TO2, CC, CC2, BCC, BCC2));


        testIMAPClient.connect(LOCALHOST_IP, jamesServer.getProbe(ImapGuiceProbe.class).getImapPort())
                .login(TO, PASSWORD)
                .select(TestIMAPClient.INBOX)
                .awaitMessage(awaitAtMostOneMinute);

        Optional<String> actual = amqpRule.readContent();
        assertThat(actual).isNotEmpty();
        //String expected = SENDER + "\n" + "[" + TO + ", John To2 <" + TO2 + ">" + "] [" + CC + ", John Cc2 <" + CC2 + ">" + "] [" + BCC + ", John Bcc2 <"
        //        + BCC2 + ">" + "]" + "\n" + Text;
        String expected = "{\"sender\":\"" + SENDER + "\",\"recipients\":\"[" + TO + ", John To2 <" + TO2 + ">" + "] [" + CC + ", John Cc2 <" + CC2 + ">" + "] [" + BCC + ", John Bcc2 <"
                + BCC2 + ">" + "]\"," + "\"text\":\"" + Text + "\"}";
        assertThat(actual).isNotEmpty();
        assertThatJson(actual.get()).isEqualTo(expected);
    }

}
