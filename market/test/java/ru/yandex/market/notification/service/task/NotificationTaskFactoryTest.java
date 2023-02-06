package ru.yandex.market.notification.service.task;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.service.task.job.CyclicNotificationTask;
import ru.yandex.market.notification.service.task.job.DecoratedNotificationTask;
import ru.yandex.market.notification.service.task.job.DelayedNotificationTask;
import ru.yandex.market.notification.service.task.job.NamedNotificationTask;
import ru.yandex.market.notification.service.task.job.NotificationTask;
import ru.yandex.market.notification.service.task.job.SafeNotificationTask;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.notification.service.task.NotificationTaskFactory.cyclic;
import static ru.yandex.market.notification.service.task.NotificationTaskFactory.delayed;
import static ru.yandex.market.notification.service.task.NotificationTaskFactory.named;
import static ru.yandex.market.notification.service.task.NotificationTaskFactory.safe;
import static ru.yandex.market.notification.service.task.NotificationTaskFactory.toRunnable;

/**
 * Unit-тесты для {@link NotificationTaskFactory}.
 *
 * @author Vladislav Bauer
 */
public class NotificationTaskFactoryTest {

    @Test
    public void testFactoryMethods() {
        final NotificationTask task = () -> {};

        assertThat(cyclic(task), instanceOf(CyclicNotificationTask.class));
        assertThat(delayed(task, 0), instanceOf(DelayedNotificationTask.class));
        assertThat(named(task, ""), instanceOf(NamedNotificationTask.class));
        assertThat(safe(task), instanceOf(SafeNotificationTask.class));
        assertThat(toRunnable(task), allOf(instanceOf(Runnable.class), instanceOf(SafeNotificationTask.class)));
    }

    @Test
    public void testNamed() throws Exception {
        final String threadName = "I Am Legend";
        final AtomicReference<String> result = new AtomicReference<>();

        final NotificationTask task = named(() -> result.set(Thread.currentThread().getName()), threadName);
        task.execute();

        assertThat(result.get(), startsWith(threadName));
    }

    @Test
    public void testSafe() throws Exception {
        final AtomicBoolean result = new AtomicBoolean(false);

        safe(() -> { fail(); result.set(true);}).execute();
        assertThat(result.get(), equalTo(false));

        safe(() -> result.set(true)).execute();
        assertThat(result.get(), equalTo(true));
    }

    @Test
    public void testToRunnable() {
        final AtomicBoolean result = new AtomicBoolean(false);

        toRunnable(() -> { fail(); result.set(true);}).run();
        assertThat(result.get(), equalTo(false));

        toRunnable(() -> result.set(true)).run();
        assertThat(result.get(), equalTo(true));
    }

    @Test
    public void testDecoratedTaskChain() {
        final String taskName = "I Am Your Father";

        final NotificationTask originalTask = () -> {};
        final DecoratedNotificationTask namedTask = (DecoratedNotificationTask) named(originalTask, taskName);
        final DecoratedNotificationTask safeTask = (DecoratedNotificationTask) safe(namedTask);

        assertThat(safeTask.toString(), equalTo(taskName));
        assertThat(safeTask.getInternalTask(), equalTo(namedTask));
        assertThat(namedTask.getInternalTask(), equalTo(originalTask));
    }

}
