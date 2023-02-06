package ru.yandex.market.replenishment.autoorder.service;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("unittest")
@TestPropertySource(locations = "classpath:functional-test.properties")
public class MailSenderServiceTest {

    private static final String EMAIL_FROM = "robot-autoorder@yandex-team.ru";

    @Test
    public void testSendIsOk() throws IOException, MessagingException {
        final String emailTo = "email@email.test";
        final String subject = "subject";
        final String body = "body 1\n" + "line 2\n" + "line 3\n";

        final Session session = Session.getDefaultInstance(System.getProperties());
        JavaMailSender javaMailSender = Mockito.mock(JavaMailSender.class);
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage(session));

        ArgumentCaptor<MimeMessage> argument = ArgumentCaptor.forClass(MimeMessage.class);
        MailSenderService mailSenderService = new MailSenderService(EMAIL_FROM, javaMailSender);
        mailSenderService.send(emailTo, subject, body, false);
        Mockito.verify(javaMailSender, times(1)).send(argument.capture());
        MimeMessage actualMessage = argument.getValue();
        MailSenderExecutorTest.checkMessageReceiversContain(actualMessage, emailTo);
        assertEquals(subject, actualMessage.getSubject());

        Object content = actualMessage.getContent();
        assertNotNull(content);
        assertEquals(body, content);
    }
}
