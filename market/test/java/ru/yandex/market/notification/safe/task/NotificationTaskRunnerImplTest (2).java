package ru.yandex.market.notification.safe.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.service.task.NotificationTaskFactory;
import ru.yandex.market.notification.service.task.NotificationTaskRunner;
import ru.yandex.market.notification.service.task.job.NotificationTask;
import ru.yandex.market.notification.service.task.job.StoppableNotificationTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты {@link NotificationTaskRunnerImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationTaskRunnerImplTest {

    @Test
    public void testSubmit() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final NotificationTask task = () -> executed.set(true);

        final ExecutorService executorService = createMockedExecutorService(task);
        final NotificationTaskRunner taskRunner = new NotificationTaskRunnerImpl(executorService);
        taskRunner.submit(task);

        verify(executorService, times(1)).submit(any(Runnable.class));

        assertThat(executed.get(), equalTo(true));
    }

    @Test
    public void testShutdown() {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final StoppableNotificationTask task = NotificationTaskFactory.cyclic(() -> executed.set(true));
        task.stop();

        final ExecutorService executorService = createMockedExecutorService(task);
        final NotificationTaskRunner taskRunner = new NotificationTaskRunnerImpl(executorService);
        taskRunner.submit(task);

        assertThat(executed.get(), equalTo(false));

        taskRunner.shutdown();

        verify(executorService, times(1)).submit(any(Runnable.class));
        verify(executorService, times(1)).shutdown();

        assertThat(task.isStopped(), equalTo(true));
    }


    private ExecutorService createMockedExecutorService(final NotificationTask task) {
        final ExecutorService executorService = mock(ExecutorService.class);
        when(executorService.isTerminated()).thenReturn(false);

        doAnswer(invocation -> { task.execute(); return null; })
            .when(executorService)
            .submit(any(Runnable.class));

        return executorService;
    }

}
