package ru.yandex.market.tsum.core.notify.common;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.clients.startrek.NotificationUtils;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 21.03.17
 */
public class NotificationUtilsTest {
    @Test
    public void wrapWithHeaderAndFooter() throws Exception {
        String text = "text";
        String telegramHeader = "Telegram header";
        String telegramFooter = "Telegram footer";
        TelegramNotificationTarget telegramNotificationTarget = new TelegramNotificationTarget(111, telegramHeader, telegramFooter);
        Assert.assertEquals(
            telegramHeader + "\n" + text + "\n" + telegramFooter,
            NotificationUtils.wrapWithHeaderAndFooter(text, telegramNotificationTarget)
        );
    }

    @Test
    public void wrapWithHeader() throws Exception {
        String text = "text";
        String telegramHeader = "Telegram header";
        String telegramFooter = "";
        TelegramNotificationTarget telegramNotificationTarget = new TelegramNotificationTarget(111, telegramHeader, telegramFooter);
        Assert.assertEquals(
            telegramHeader + "\n" + text,
            NotificationUtils.wrapWithHeaderAndFooter(text, telegramNotificationTarget)
        );
    }

    @Test
    public void wrapWithFooter() throws Exception {
        String text = "text";
        String telegramHeader = "";
        String telegramFooter = "Telegram footer";
        TelegramNotificationTarget telegramNotificationTarget = new TelegramNotificationTarget(111, telegramHeader, telegramFooter);
        Assert.assertEquals(
            text + "\n" + telegramFooter,
            NotificationUtils.wrapWithHeaderAndFooter(text, telegramNotificationTarget)
        );
    }
}