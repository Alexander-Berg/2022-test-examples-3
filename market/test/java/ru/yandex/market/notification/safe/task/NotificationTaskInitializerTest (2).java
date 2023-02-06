package ru.yandex.market.notification.safe.task;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.service.task.NotificationTaskRunner;
import ru.yandex.market.notification.service.task.job.NotificationTask;
import ru.yandex.market.notification.service.task.job.StoppableNotificationTask;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit-тесты для {@link NotificationTaskInitializer}.
 *
 * @author Vladislav Bauer
 */
public class NotificationTaskInitializerTest {

    @Test
    public void testInitializer() throws Exception {
        final NotificationTask task = () -> {};
        final Collection<NotificationTask> tasks = Collections.singleton(task);

        final NotificationTaskRunner taskRunner = mock(NotificationTaskRunner.class);
        final NotificationTaskInitializer initializer = new NotificationTaskInitializer(taskRunner, tasks);

        initializer.afterPropertiesSet();
        initializer.destroy();

        verify(taskRunner, times(1)).submit(any(StoppableNotificationTask.class));
        verify(taskRunner, times(1)).shutdown();
    }

}
