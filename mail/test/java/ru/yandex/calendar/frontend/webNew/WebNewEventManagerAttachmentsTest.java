package ru.yandex.calendar.frontend.webNew;

import lombok.val;
import org.joda.time.Duration;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.webNew.dto.in.WebEventData;
import ru.yandex.calendar.frontend.webNew.dto.inOut.EventAttachmentData;
import ru.yandex.calendar.frontend.webNew.dto.out.WebEventInfo;
import ru.yandex.calendar.logic.ics.TestDateTimes;

import static org.assertj.core.api.Assertions.assertThat;

public class WebNewEventManagerAttachmentsTest extends WebNewEventManagerTestBase {
    @Test
    public void createEventWithAttachment() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        val data = consBaseData();
        data.setAttachments(Option.of(Cf.list(attachment)));

        val createdEvent = eventActions.createEvent(uid, Option.empty(), Option.empty(), data);

        long eventId = createdEvent.asModified().getShowEventId();
        WebEventInfo actual = getEvent(uid, eventId);
        assertThat(actual.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualAttachments = actual.getAttachments().get();
        assertThat(actualAttachments.size()).isEqualTo(1);
        assertThat(actualAttachments.single()).isEqualTo(attachment);
    }

    @Test
    public void updateEventAddAttachment() {
        val data = consBaseData();
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        data.setAttachments(Option.of(Cf.list(attachment)));

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                data,
                Option.empty(),
                Option.empty(),
                Option.empty());

        assertThat(updatedEvent.asModified().getShowEventId()).isEqualTo(eventId);
        WebEventInfo actual = getEvent(uid, eventId);
        assertThat(actual.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualAttachments = actual.getAttachments().get();
        assertThat(actualAttachments.size()).isEqualTo(1);
        assertThat(actualAttachments.single()).isEqualTo(attachment);
    }

    @Test
    public void updateEventChangeAttachment() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        val data = consBaseData();
        data.setAttachments(Option.of(Cf.list(attachment)));
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        EventAttachmentData newAttachment = new EventAttachmentData("new external id", "new file name", 1);
        data.setAttachments(Option.of(Cf.list(newAttachment)));

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                data,
                Option.empty(),
                Option.empty(),
                Option.empty());

        assertThat(updatedEvent.asModified().getShowEventId()).isEqualTo(eventId);
        WebEventInfo actual = getEvent(uid, eventId);
        assertThat(actual.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualAttachments = actual.getAttachments().get();
        assertThat(actualAttachments.size()).isEqualTo(1);
        assertThat(actualAttachments.single()).isEqualTo(newAttachment);
    }

    @Test
    public void updateEventChangeAttachmentInRecurrenceEvent() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(), Cf.list());
        data.setAttachments(Option.of(Cf.list(attachment)));
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        val recurrenceEvent = testManager.createDefaultRecurrence(uid, eventId, NOW.plusDays(1));
        long newEventId = recurrenceEvent.getId();

        WebEventData newEventData = consBaseData();
        newEventData.setStartTs(wrapDate(TestDateTimes.plusHours(recurrenceEvent.getStartTs(), 1).toDateTime()));
        newEventData.setEndTs(wrapDate(TestDateTimes.plusHours(recurrenceEvent.getEndTs(), 1).toDateTime()));
        EventAttachmentData newAttachment = new EventAttachmentData("new external id", "new file name", 1);
        newEventData.setAttachments(Option.of(Cf.list(newAttachment)));

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(newEventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                newEventData,
                Option.empty(),
                Option.empty(),
                Option.empty());

        WebEventInfo actualOldEvent = getEvent(uid, eventId);
        assertThat(actualOldEvent.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualOldAttachments = actualOldEvent.getAttachments().get();
        assertThat(actualOldAttachments.size()).isEqualTo(1);
        assertThat(actualOldAttachments.single()).isEqualTo(attachment);
        assertThat(updatedEvent.asModified().getShowEventId()).isEqualTo(newEventId);
        WebEventInfo actualNewEvent = getEvent(uid, newEventId);
        assertThat(actualNewEvent.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualNewAttachments = actualNewEvent.getAttachments().get();
        assertThat(actualNewAttachments.size()).isEqualTo(1);
        assertThat(actualNewAttachments.single()).isEqualTo(newAttachment);
    }

    @Test
    public void updateEventChangeAttachmentInRecurrenceEventWithFuture() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(), Cf.list());
        data.setAttachments(Option.of(Cf.list(attachment)));
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        EventAttachmentData newAttachment = new EventAttachmentData("new external id", "new file name", 2);
        data.setAttachments(Option.of(Cf.list(newAttachment)));

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                data,
                Option.empty(),
                Option.of(true),
                Option.empty());

        assertThat(updatedEvent.asModified().getShowEventId()).isEqualTo(eventId);
        WebEventInfo actualEvent = getEvent(uid, eventId);
        assertThat(actualEvent.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualAttachments = actualEvent.getAttachments().get();
        assertThat(actualAttachments.size()).isEqualTo(1);
        assertThat(actualAttachments.single()).isEqualTo(newAttachment);
    }

    @Test
    public void updateEventChangeAttachmentInRecurrenceEventWithoutFuture() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(), Cf.list());
        data.setAttachments(Option.of(Cf.list(attachment)));
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        EventAttachmentData newAttachment = new EventAttachmentData("new external id", "new file name", 2);
        data.setAttachments(Option.of(Cf.list(newAttachment)));
        val instanceStartTs = data.getStartTs().get().getDateTime().plusDays(7).minusHours(3).toLocalDateTime();

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                data,
                Option.empty(),
                Option.of(false),
                Option.of(instanceStartTs));

        WebEventInfo actualEvent = getEvent(uid, eventId);
        assertThat(actualEvent.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualAttachments = actualEvent.getAttachments().get();
        assertThat(actualAttachments.size()).isEqualTo(1);
        assertThat(actualAttachments.single()).isEqualTo(attachment);
        assertThat(updatedEvent.asModified().getShowEventId()).isNotEqualTo(eventId);
        WebEventInfo actualNewEvent = getEvent(uid, updatedEvent.asModified().getShowEventId());
        assertThat(actualNewEvent.getAttachments().isPresent()).isTrue();
        ListF<EventAttachmentData> actualNewAttachments = actualNewEvent.getAttachments().get();
        assertThat(actualNewAttachments.size()).isEqualTo(1);
        assertThat(actualNewAttachments.single()).isEqualTo(newAttachment);
    }

    @Test
    public void updateEventChangeAttachmentInRecurrenceEventNotRemoveOwnFutureAttachments() {
        EventAttachmentData attachment = new EventAttachmentData("external id", "file name", 1);
        EventAttachmentData attachmentToRemove = new EventAttachmentData("to remove", "to remove name", 2);
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(), Cf.list());
        data.setAttachments(Option.of(Cf.list(attachment, attachmentToRemove)));
        long eventId = createEvent(uid, data).asModified().getShowEventId();

        val updatedEventData = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(), Cf.list());
        EventAttachmentData ownAttachment = new EventAttachmentData("own id", "own file name", 3);
        updatedEventData.setAttachments(Option.of(Cf.list(attachment, attachmentToRemove, ownAttachment)));
        val instanceStartTs = data.getStartTs().get().getDateTime().plusDays(7).minusHours(3).toLocalDateTime();

        val updatedEvent = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                updatedEventData,
                Option.empty(),
                Option.of(false),
                Option.of(instanceStartTs));

        EventAttachmentData newAttachment = new EventAttachmentData("new", "new file name", 4);
        data.setAttachments(Option.of(Cf.list(attachment, newAttachment)));
        val seriesUpdate = eventActions.updateEvent(
                uid,
                Option.of(eventId),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                Option.empty(),
                data,
                Option.empty(),
                Option.of(true),
                Option.empty());

        assertThat(seriesUpdate.asModified().getShowEventId()).isEqualTo(eventId);
        WebEventInfo actualEvent = getEvent(uid, eventId);
        assertThat(actualEvent.getAttachments().isPresent()).isTrue();
        assertThat(actualEvent.getAttachments().get()).isEqualTo(Cf.list(attachment, newAttachment));
        assertThat(updatedEvent.asModified().getShowEventId()).isNotEqualTo(eventId);
        WebEventInfo actualNewEvent = getEvent(uid, updatedEvent.asModified().getShowEventId());
        assertThat(actualNewEvent.getAttachments().isPresent()).isTrue();
        assertThat(actualNewEvent.getAttachments().get()).isEqualTo(Cf.list(attachment, ownAttachment, newAttachment));
    }
}
