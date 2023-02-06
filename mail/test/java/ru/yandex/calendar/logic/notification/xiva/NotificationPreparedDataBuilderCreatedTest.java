package ru.yandex.calendar.logic.notification.xiva;

import java.util.Optional;

import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.calendar.frontend.webNew.WebNewContextConfiguration;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.notification.xiva.content.XivaNotificationType;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;

@ContextConfiguration(classes = {
        WebNewContextConfiguration.class,
        TestBaseContextConfiguration.class,
        NotificationTestConfiguration.class
})
public class NotificationPreparedDataBuilderCreatedTest extends NotificationDataTestBase {
    @Autowired
    private NotificationPreparedDataBuilder preparedDataBuilder;

    @Test
    public void singleEventInPast() {
        CreateInfo createdEvent =  eventManager.createSingleEvent(dayInPast);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInPast.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }

    @Test
    public void singleEventInFutureOutsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureOutsideZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureOutsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }

    @Test
    public void singleEventInFutureInsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureInsideZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureInsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }

    @Test
    public void seriesEventInPastWithOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertTrue(preparedData.getCurrentStartTs().isAfterNow());
        long delta = preparedData.getCurrentStartTs().minus(Instant.now().getMillis()).getMillis();
        Assert.assertTrue(delta < 24 * 60 * 60 * 1000);
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }

    @Test
    public void seriesEventInPastWithoutOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithDueTsInPast);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertTrue(preparedData.getCurrentStartTs().isBeforeNow());
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }

    @Test
    public void seriesEventInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInFutureInsideZone, dailyRepetitionWithoutDueTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureInsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isEmpty());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.CREATED);
    }
}
