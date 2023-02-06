package ru.yandex.calendar.logic.notification.xiva;

import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

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
public class NotificationPreparedDataBuilderUpdatedTest extends NotificationDataTestBase {
    @Autowired
    private NotificationPreparedDataBuilder preparedDataBuilder;

    @Test
    public void moveSingleEventFromZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureInsideZone);
        ModificationInfo modificationInfo = eventManager.moveSingleEventTo(createdEvent, dayInFutureOutsideZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureOutsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), dayInFutureInsideZone.toInstant());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveSingleEventToZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureOutsideZone);
        ModificationInfo modificationInfo = eventManager.moveSingleEventTo(createdEvent, dayInFutureInsideZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureInsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), dayInFutureOutsideZone.toInstant());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveSingleEventOutsideZone() {
        CreateInfo createdEvent = eventManager.createSingleEvent(dayInFutureOutsideZone);
        ModificationInfo modificationInfo = eventManager.moveSingleEventTo(createdEvent, dayFurtherInFutureOutsideZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayFurtherInFutureOutsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), dayInFutureOutsideZone.toInstant());
        Assert.assertEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    @Ignore
    public void moveDueTsFromPastToFuture() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithDueTsInPast);
        ModificationInfo modificationInfo = eventManager.changeRepetition(createdEvent, dailyRepetitionWithDueTsInFuture);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertTrue(false);
    }

    @Test
    @Ignore
    public void moveDueTsFromFutureToPast() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithDueTsInFuture);
        ModificationInfo modificationInfo = eventManager.changeRepetition(createdEvent, dailyRepetitionWithDueTsInPast);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertTrue(false);
    }

    @Test
    public void changeRepetitionWithDisappearedOccurenceInZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithOccurenceInZone);
        ModificationInfo modificationInfo = eventManager.changeRepetition(createdEvent, weeklyRepetitionWithoutOccurenceInZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        long currentDelta = preparedData.getCurrentStartTs().minus(Instant.now().getMillis()).getMillis();
        Assert.assertTrue(currentDelta > (XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS) * 24 * 60 * 60 * 1000);
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        long oldDelta = preparedData.getOldStartTs().get().minus(Instant.now().getMillis()).getMillis();
        Assert.assertTrue(oldDelta < (XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS) * 24 * 60 * 60 * 1000);
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void changeRepetitionWithAppearedOccurenceInZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithoutOccurenceInZone);
        ModificationInfo modificationInfo = eventManager.changeRepetition(createdEvent, weeklyRepetitionWithOccurenceInZone);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        long currentDelta = preparedData.getCurrentStartTs().minus(Instant.now().getMillis()).getMillis();
        Assert.assertTrue(currentDelta < (XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS) * 24 * 60 * 60 * 1000);
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        long oldDelta = preparedData.getOldStartTs().get().minus(Instant.now().getMillis()).getMillis();
        Assert.assertTrue(oldDelta > (XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS) * 24 * 60 * 60 * 1000);
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveOccurenceInsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(1);
        DateTime newStartTs = instanceStartTs.plusHours(1);

        ModificationInfo modificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), newStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveOccurenceOutsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1);
        DateTime newStartTs = instanceStartTs.plusHours(1);

        ModificationInfo modificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, newStartTs);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), newStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveOccurenceToZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithoutOccurenceInZone);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone);

        ModificationInfo modificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, dayInFutureInsideZone);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureInsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveOccurenceFromZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithOccurenceInZone);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone);

        ModificationInfo modificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, dayInFutureOutsideZone);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), dayInFutureOutsideZone.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveRecurrenceInsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(1);
        DateTime occurenceStartTs = instanceStartTs.plusHours(1);
        DateTime recurrenceStartTs = occurenceStartTs.plusHours(1);

        ModificationInfo occurenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, occurenceStartTs);
        ModificationInfo recurrenceModificationInfo = eventManager.moveRecurrenceTo(occurenceModificationInfo.getNewEvent().get(), recurrenceStartTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(occurenceModificationInfo.getNewEvent().get(), Optional.of(recurrenceModificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), recurrenceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), occurenceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveRecurrenceOutsideZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1);

        DateTime occurenceStartTs = instanceStartTs.plusHours(1);
        DateTime recurrenceStartTs = occurenceStartTs.plusHours(1);

        ModificationInfo occurenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, occurenceStartTs);
        ModificationInfo recurrenceModificationInfo = eventManager.moveRecurrenceTo(occurenceModificationInfo.getNewEvent().get(), recurrenceStartTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(occurenceModificationInfo.getNewEvent().get(), Optional.of(recurrenceModificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), recurrenceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), occurenceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveRecurrenceToZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithoutOccurenceInZone);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone);

        DateTime occurenceStartTs = instanceStartTs.plusHours(1);
        DateTime recurrenceStartTs = dayInFutureInsideZone;

        ModificationInfo occurenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, occurenceStartTs);
        ModificationInfo recurrenceModificationInfo = eventManager.moveRecurrenceTo(occurenceModificationInfo.getNewEvent().get(), recurrenceStartTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(occurenceModificationInfo.getNewEvent().get(), Optional.of(recurrenceModificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), recurrenceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), occurenceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void moveRecurrenceFromZone() {
        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, weeklyRepetitionWithOccurenceInZone);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone);

        DateTime occurenceStartTs = instanceStartTs.plusHours(1);
        DateTime recurrenceStartTs = dayInFutureOutsideZone;

        ModificationInfo occurenceModificationInfo = eventManager.moveOccurenceTo(createdEvent, instanceStartTs, occurenceStartTs);
        ModificationInfo recurrenceModificationInfo = eventManager.moveRecurrenceTo(occurenceModificationInfo.getNewEvent().get(), recurrenceStartTs);

        NotificationPreparedData preparedData = preparedDataBuilder
                .build(occurenceModificationInfo.getNewEvent().get(), Optional.of(recurrenceModificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), recurrenceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), occurenceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), eventManager.defaultEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void changeSeriesEventOccurenceNameOutsideZone() {
        String newEventName = "new_event_name";

        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(XivaNotificationNeedSendChecker.DEFAULT_ALLOWABLE_OFFSET_IN_DAYS + 1);

        ModificationInfo modificationInfo = eventManager.changeOccurenceEventName(createdEvent, instanceStartTs, newEventName);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), instanceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), newEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }

    @Test
    public void changeSeriesEventOccurenceNameInsideZone() {
        String newEventName = "new_event_name";

        CreateInfo createdEvent = eventManager.createSeriesEvent(dayInPast, dailyRepetitionWithoutDueTs);
        DateTime instanceStartTs = createdEvent
                .getEventAndRepetition()
                .getClosestInterval(Instant.now()).get()
                .getStart()
                .toDateTime(eventManager.defaultTimeZone)
                .plusDays(1);

        ModificationInfo modificationInfo = eventManager.changeOccurenceEventName(createdEvent, instanceStartTs, newEventName);
        NotificationPreparedData preparedData = preparedDataBuilder
                .build(createdEvent.getEventAndRepetition(), Optional.of(modificationInfo), XivaNotificationType.UPDATED);

        Assert.assertEquals(preparedData.getCurrentStartTs(), instanceStartTs.toInstant());
        Assert.assertTrue(preparedData.getOldStartTs().isPresent());
        Assert.assertEquals(preparedData.getOldStartTs().get(), instanceStartTs.toInstant());
        Assert.assertNotEquals(preparedData.getEventId(), createdEvent.getEventId());
        Assert.assertEquals(preparedData.getEventName(), newEventName);
        Assert.assertEquals(preparedData.getType(), XivaNotificationType.UPDATED);
    }
}
