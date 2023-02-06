package ru.yandex.market.tsum.core.notify.common.helloworld;

import ru.yandex.market.tsum.core.notify.common.ContextBuilder;
import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;

import java.util.Map;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 22.03.17
 */
public class HelloWorldNotificationTelegram implements TelegramNotification {
    private final String telegramResourcePath = "helloWorldTelegram.md";

    private Map<String, Object> getTelegramContext() {
        return ContextBuilder.create().with("world", "telegram").build();
    }

    private Map<String, Object> getStartrekContext() {
        return ContextBuilder.create().with("world", "startrek").build();
    }

    @Override
    public String getTelegramMessage() {
        return NotificationUtils.render(getResourceTemplate(telegramResourcePath), getTelegramContext());
    }
}
