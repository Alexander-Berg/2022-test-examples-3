package ru.yandex.market.tsum.pipe.engine.definition.job;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.NotificationEvents;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.JobNotificationEventMeta;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.events.NotificationEventMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 12.02.18
 */
public class NotificationsTest {
    private static JobNotificationEventMeta superClassEvent = new JobNotificationEventMeta(
        "superClassEvent", "", NotificationEventMeta.DefaultMessages.newBuilder().build(), ""
    );
    private static JobNotificationEventMeta subClassEvent = new JobNotificationEventMeta(
        "subClassEvent", "", NotificationEventMeta.DefaultMessages.newBuilder().build(), ""
    );

    @Test
    public void extractNotificationEvents() {
        List<JobNotificationEventMeta> notificationEvents = Job.extractNotificationEvents(SubClass.class);
        Assert.assertEquals(Arrays.asList(subClassEvent, superClassEvent), notificationEvents);
    }

    private static class SuperClass implements JobExecutor {
        @NotificationEvents
        public static List<JobNotificationEventMeta> getNotificationEvents() {
            return Collections.singletonList(superClassEvent);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("b77958a0-8091-4dbc-90da-1f64c109e552");
        }

        @Override
        public void execute(JobContext context) throws Exception {

        }
    }

    private static class SubClass extends SuperClass {
        @NotificationEvents
        public static List<JobNotificationEventMeta> getNotificationEvents() {
            return Collections.singletonList(subClassEvent);
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("428b26f4-904f-4995-980a-b9026d8d492f");
        }
    }
}