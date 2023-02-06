package ru.yandex.market.tsum.core.notify.common.helloworld;

import ru.yandex.market.tsum.clients.notifications.telegram.TelegramClient;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 07.12.17
 */
public class BigTelegramNotification implements TelegramNotification {
    public static final int MESSAGES_COUNT = 3;

    @Override
    public String getTelegramMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < MESSAGES_COUNT; ++i) {
            stringBuilder.append(getOneMessage());
        }
        return stringBuilder.toString();
    }

    public String getOneMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < TelegramClient.MAX_MESSAGE_LENGTH; ++i) {
            stringBuilder.append("a");
        }
        return stringBuilder.toString();
    }
}
