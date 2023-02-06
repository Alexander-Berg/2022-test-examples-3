package ru.yandex.market.tsum.core.notify.common.helloworld;

import ru.yandex.market.tsum.core.notify.common.ContextBuilder;
import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.clients.notifications.email.EmailNotification;
import ru.yandex.market.tsum.clients.startrek.StartrekCommentNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;

import java.util.Map;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 14.03.17
 */
public class HelloWorldNotification implements TelegramNotification, StartrekCommentNotification, EmailNotification {
    private final String startrekResourcePath = "helloWorldStatrek.txt";
    private final String telegramResourcePath = "helloWorldTelegram.md";
    private final String emailResourcePath = "helloWorldEmail.md";

    private Map<String, Object> getContext(String world) {
        return ContextBuilder.create().with("world", world).build();
    }

    @Override
    public String getTelegramMessage() {
        return NotificationUtils.render(
            getResourceTemplate(telegramResourcePath), getContext("telegram")
        );
    }

    @Override
    public String getStartrekComment() {
        return NotificationUtils.render(getResourceTemplate(startrekResourcePath), getContext("startrek"));
    }

    @Override
    public String getEmailMessage() {
        return NotificationUtils.render(getResourceTemplate(emailResourcePath), getContext("email"));
    }

    @Override
    public String getEmailSubject() {
        return "Test hello world subject";
    }
}
