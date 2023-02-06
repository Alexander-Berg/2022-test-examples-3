package ru.yandex.calendar.logic.notification.xiva;

import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.webNew.WebNewContextConfiguration;
import ru.yandex.calendar.logic.event.CreateInfo;
import ru.yandex.calendar.logic.event.ModificationInfo;
import ru.yandex.calendar.logic.notification.xiva.content.XivaNotificationType;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;

@ContextConfiguration(classes = {
        WebNewContextConfiguration.class,
        TestBaseContextConfiguration.class,
        NotificationTestConfiguration.class
})
public class XivaNotificationNeedSendCheckerDeleteTest extends NotificationDataTestBase {
    @Autowired
    private XivaNotificationNeedSendChecker needSendChecker;
    @Autowired
    private NotificationPreparedDataBuilder preparedDataBuilder;

    @Test
    public void deleteAbsence() {
        CreateInfo createdEvent =  eventManager.createAbsence(dayInFutureInsideZone);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.CREATED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void deleteSingleEventInPast() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInPast);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void deleteSingleEventInFutureInsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureInsideZone);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void deleteSingleEventInFutureOutsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureOutsideZone);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void deleteSeriesEventInPastWithOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        ModificationInfo modificationInfo = eventManager.deleteSeriesEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    public void deleteSeriesEventInPastWithoutOccurenceInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithDueTsInPast);
        ModificationInfo modificationInfo = eventManager.deleteSeriesEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertFalse(isNeedSend);
    }

    @Test
    public void deleteSeriesEventInFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInFutureInsideZone, dailyRepetitionWithoutDueTs);
        ModificationInfo modificationInfo = eventManager.deleteSeriesEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteSeriesEventWithRecurrenceInsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithOccurenceInZone);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone);
        DateTime newStartTs = instanceStartTs.plusHours(1);
        ModificationInfo occurenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        ModificationInfo deleteModificationInfo = eventManager.deleteSeriesEvent(createdEvent.getEventId());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(occurenceModificationInfo.getUpdatedEvent().get(), Optional.of(deleteModificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteOccurenceInPast() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .minusDays(1);
        ModificationInfo modificationInfo = eventManager.deleteOccurence(createdEvent.getEventId(), instanceStartTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteOccurenceInFutureInsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(1);
        ModificationInfo modificationInfo = eventManager.deleteOccurence(createdEvent.getEventId(), instanceStartTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteOccurenceInFutureOutsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1);
        ModificationInfo modificationInfo = eventManager.deleteOccurence(createdEvent.getEventId(), instanceStartTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteRecurrenceInPast() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .minusDays(1);
        DateTime newStartTs = instanceStartTs.plusHours(1);
        ModificationInfo recurrenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(recurrenceModificationInfo.getNewEventId().get());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteRecurrenceInFutureInsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(1);
        DateTime newStartTs = instanceStartTs.plusHours(1);
        ModificationInfo recurrenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(recurrenceModificationInfo.getNewEventId().get());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }

    @Test
    @Ignore
    public void deleteRecurrenceInFutureOutsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1);
        DateTime newStartTs = instanceStartTs.plusHours(1);
        ModificationInfo recurrenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        ModificationInfo modificationInfo = eventManager.deleteSingleEvent(recurrenceModificationInfo.getNewEventId().get());
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.DELETED);

        boolean isNeedSend = needSendChecker.isNotificationNeedSend(
                Option.of(eventManager.defaultOrganizer.getUid()),
                eventManager.defaultCreator.getUid(),
                preparedData
        );

        Assert.assertTrue(isNeedSend);
    }
}
