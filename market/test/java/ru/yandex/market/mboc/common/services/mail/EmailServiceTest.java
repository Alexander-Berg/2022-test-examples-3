package ru.yandex.market.mboc.common.services.mail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thymeleaf.TemplateEngine;

import static ru.yandex.market.mboc.common.services.mail.JavaMailSenderMock.MESSAGE_COMPARATOR;

public class EmailServiceTest {

    private EmailService emailService;
    private JavaMailSenderMock senderMockSpy;

    private String fromEmail;
    private String fromPersonal;
    private List<String> to;
    private List<String> cc;
    private String subject;

    @Before
    public void setUp() {
        senderMockSpy = Mockito.spy(new JavaMailSenderMock());
        fromEmail = "test-email@email.test";
        fromPersonal = "Test Name";
        emailService = new EmailService(senderMockSpy, Mockito.mock(TemplateEngine.class), fromEmail, fromPersonal);
        to = Arrays.asList("to1@email.ru", "to2@email.ru");
        cc = Arrays.asList("cc1@email.ru", "cc2@email.ru");
        subject = "Test subject";
    }

    @Test
    public void mailTest() throws MessagingException {
        String text = "Test text";
        Assert.assertTrue(emailService.mail(to, cc, subject, text, false, Collections.emptyList()));

        Assertions.assertThat(senderMockSpy.getMimeMessages())
            .usingElementComparator(MESSAGE_COMPARATOR)
            .containsExactlyInAnyOrder(
                senderMockSpy.createTestMimeMessage(fromEmail, fromPersonal, to, cc, subject, text, false));

    }

    @Test
    public void mailTestHtml() throws MessagingException {
        String text = "<b>bold test text</b>";
        Assert.assertTrue(emailService.mail(to, cc, subject, text, true, Collections.emptyList()));

        Assertions.assertThat(senderMockSpy.getMimeMessages())
            .usingElementComparator(MESSAGE_COMPARATOR)
            .containsExactlyInAnyOrder(
                senderMockSpy.createTestMimeMessage(fromEmail, fromPersonal, to, cc, subject, text, false));
    }


    @Test
    public void mailTestFailure() throws MessagingException {
        Mockito.doThrow(new RuntimeException())
            .when(senderMockSpy)
            .send(Mockito.any(MimeMessage.class));

        Assert.assertFalse(emailService.mail(to, subject, "text"));
    }
}
