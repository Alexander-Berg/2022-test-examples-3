package ru.yandex.market.tsum.core.notify.common;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.market.tsum.clients.notifications.Notification;
import ru.yandex.market.tsum.clients.notifications.NotificationTarget;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotification;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramNotificationTarget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 21.02.18
 */
public class NotificationCenterTest {
    @Test
    public void notifyTest() throws Exception {
        BazingaTaskManager taskManager = Mockito.mock(BazingaTaskManager.class);
        NotificationCenter notificationCenter = new NotificationCenter(taskManager);

        notificationCenter.notify((List<Notification>) null, (NotificationTarget[]) null);
        Mockito.verify(taskManager, Mockito.never()).schedule(Mockito.any());

        notificationCenter.notify((Notification) null, (NotificationTarget[]) null);
        Mockito.verify(taskManager, Mockito.never()).schedule(Mockito.any());

        notificationCenter.notify(null, (List<NotificationTarget>) null);
        Mockito.verify(taskManager, Mockito.never()).schedule(Mockito.any());

        notificationCenter.notify((TelegramNotification) () -> "message", new TelegramNotificationTarget(123));
        Mockito.verify(taskManager, Mockito.only()).schedule(Mockito.any());
        Mockito.reset(taskManager);

        notificationCenter.notify(
            Arrays.asList((TelegramNotification) () -> "message1", (TelegramNotification) () -> "message2"),
            new TelegramNotificationTarget(123)
        );
        Mockito.verify(taskManager, Mockito.times(2)).schedule(Mockito.any());
        Mockito.reset(taskManager);

        notificationCenter.notify(
            Collections.singletonList((TelegramNotification) () -> "message"),
            new TelegramNotificationTarget(123),
            new TelegramNotificationTarget(456)
        );
        Mockito.verify(taskManager, Mockito.only()).schedule(Mockito.any());
        Mockito.reset(taskManager);
    }
}