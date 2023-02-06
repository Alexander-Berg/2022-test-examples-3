package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;
import java.util.Arrays;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.MailSenderExecutor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@Slf4j
public class MailSenderExecutorTest extends FunctionalTest {

    @Autowired
    private MailSenderExecutor mailSenderExecutor;

    @Autowired
    private JavaMailSender javaMailSender;

    @Test
    @DbUnitDataSet(before = "MailSenderExecutorTest.before.csv",
            after = "MailSenderExecutorTest_loadSentMailsAndLoggedErrors.after.csv")
    public void loadSentMailsAndLoggedErrors() {
        final String exceptionTo = "name2@yandex-team.ru";
        setTestTime(LocalDateTime.of(2021, 5, 30, 12, 0, 0));
        doThrow(new MailSendException("Test error"))
                .when(javaMailSender)
                .send(argThat((MimeMessage message) -> checkMessageReceiversContain(message, exceptionTo)));
        mailSenderExecutor.load();
    }

    @Test
    @DbUnitDataSet(before = "MailSenderExecutorTest_isNeedToSendMailReturnFalse.before.csv")
    public void isNeedToSendMailReturnFalse() {
        assertFalse(mailSenderExecutor.isNeedToSendMail());
    }

    @Test
    @DbUnitDataSet(before = "MailSenderExecutorTest.before.csv")
    public void isNeedToSendMailReturnTrue() {
        assertTrue(mailSenderExecutor.isNeedToSendMail());
    }

    public static boolean checkMessageReceiversContain(MimeMessage message, String receiver) {
        if (message == null) {
            return false;
        }

        try {
            final Address[] recipients = message.getRecipients(Message.RecipientType.TO);
            return Arrays.stream(recipients).anyMatch(r -> r.toString().contains(receiver));
        } catch (Throwable err) {
            log.error(err.getMessage(), err);
            return false;
        }
    }
}
