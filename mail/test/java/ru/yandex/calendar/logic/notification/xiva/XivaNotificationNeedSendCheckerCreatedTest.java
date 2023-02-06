package ru.yandex.calendar.logic.notification.xiva;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.webNew.WebNewContextConfiguration;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.notification.xiva.content.XivaNotificationType;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;

@ContextConfiguration(classes = {
        WebNewContextConfiguration.class,
        TestBaseContextConfiguration.class,
        NotificationTestConfiguration.class
})
public class XivaNotificationNeedSendCheckerCreatedTest extends NotificationDataTestBase {
    @Autowired
    private XivaNotificationNeedSendChecker needSendChecker;
    @Autowired
    private NotificationPreparedDataBuilder preparedDataBuilder;

    @Test
    public void absenceEvent() {
        CreateInfo createdEvent =  eventManager.createAbsence(dayInFutureInsideZone);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void singleEventInPast() {
        CreateInfo createdEvent =  eventManager.createSingleEvent(dayInPast);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void singleEventInFutureOutsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureOutsideZone);
        NotificationPreparedData preparedData = preparedDataBuilder.build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void singleEventInFutureInsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureInsideZone);
        NotificationPreparedData preparedData = preparedDataBuilder.build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void seriesEventInPastWithOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void seriesEventInPastWithoutOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithDueTsInPast);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void seriesEventInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInFutureInsideZone, dailyRepetitionWithoutDueTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.empty(), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }
}
