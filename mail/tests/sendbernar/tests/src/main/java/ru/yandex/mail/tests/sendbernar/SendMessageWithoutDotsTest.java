package ru.yandex.mail.tests.sendbernar;

import ru.yandex.mail.common.credentials.AccountWithScope;

public class SendMessageWithoutDotsTest extends SendMessageTest {
    @Override
    AccountWithScope mainUser() {
        return Accounts.sendMessageWithoutDots;
    }
}
