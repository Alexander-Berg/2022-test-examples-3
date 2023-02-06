package ru.yandex.market.checkout.checkouter.monitoring;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import ru.yandex.market.checkout.application.AbstractWebTestBase;

/**
 * @author : poluektov
 * date: 20.07.18.
 * <p>
 * Для отладки
 */
public class EmailMonitoringServiceTest extends AbstractWebTestBase {

    @Test
    @Disabled("Если раскоментить этот тест, то каждый запуск ф-тестов будет отправлять письмо. " +
            "Юзать только для дебага с локальной машинки.")
    public void testSendNotification() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("outbound-relay.yandex.net");
        EmailSenderService emailSenderService = new EmailSenderServiceImpl(mailSender);
        emailSenderService.sendNotification("Чак аутер хочет тикет!", "Сделай его!",
                "marketpayreport-bugs@yandex-team.ru");
    }
}
