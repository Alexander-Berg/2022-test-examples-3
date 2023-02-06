package ru.yandex.market.jmf.module.mail.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.module.mail.ContentType;
import ru.yandex.market.jmf.module.mail.MailSenderService;
import ru.yandex.market.jmf.module.mail.MailSendingProperties;

@Transactional
@SpringJUnitConfig(InternalModuleMailTestConfiguration.class)
public class MailSenderServiceTest {

    @Inject
    MailSenderService mailSenderService;

    @Test
    public void sendMail() throws MessagingException {
        var to = List.of("yamarkettestov@yandex.ru");
        var copyTo = List.of("copy@yandex.ru", "copy2@yandex.ru");
        String subject = "subject";
        String text = "<B>text</B>";
        MimeMessage message = mailSenderService.send(new MailSendingProperties(null, null, to, null,
                copyTo, List.of(), null, subject, ContentType.HTML, text,
                List.of(), Map.of()));
        List<String> recipients = Arrays.stream(message.getRecipients(Message.RecipientType.TO))
                .map(Address::toString)
                .toList();
        List<String> recipientsCC = Arrays.stream(message.getRecipients(Message.RecipientType.CC))
                .map(Address::toString)
                .toList();
        Assertions.assertNotNull(recipients);
        Assertions.assertEquals(to, recipients);
        Assertions.assertEquals(copyTo, recipientsCC);
        Assertions.assertEquals(subject, message.getSubject());
    }
}
