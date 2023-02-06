package ru.yandex.market.checkout.util;

import javax.annotation.Nonnull;

import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.checkout.checkouter.monitoring.EmailSenderService;

/**
 * @author : poluektov
 * date: 2019-07-01.
 */
@TestComponent("emailSenderService")
public class StubEmailSenderServiceImpl implements EmailSenderService {

    @Override
    public void sendNotification(@Nonnull String subject, @Nonnull String text, @Nonnull String recipient) {
        return;
    }
}
