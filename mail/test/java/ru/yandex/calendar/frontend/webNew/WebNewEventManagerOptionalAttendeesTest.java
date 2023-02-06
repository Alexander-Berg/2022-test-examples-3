package ru.yandex.calendar.frontend.webNew;

import lombok.val;
import org.joda.time.Duration;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.dto.out.WebEventInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class WebNewEventManagerOptionalAttendeesTest extends WebNewEventManagerTestBase {
    @Test
    public void getOptionalAttendees() {
        val users = getUsers();
        val eventId = createEvent(Cf.list(), users);
        val attendees = eventActions.getOptionalAttendees(uid, eventId, Option.empty()).getAttendees();
        validateAttendees(users, attendees);
    }

    @Test
    public void changeAttendeeTypeFromRequiredToOptionalInSingleEvent() {
        val data = consBaseData();
        data.setAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();

        data.setAttendeeEmails(Option.of(Cf.list()));
        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.empty(), Option.empty());

        val event = getEvent(user.getUid(), eventId);

        checkThatAttendeeHasBecomeOptional(event);
    }

    @Test
    public void changeAttendeeTypeFromOptionalToRequiredInSingleEvent() {
        val data = consBaseData();
        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();

        data.setOptionalAttendeeEmails(Option.of(Cf.list()));
        data.setAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.empty(), Option.empty());

        val event = getEvent(user.getUid(), eventId);

        checkThatAttendeeHasBecomeRequired(event);
    }

    @Test
    public void changeAttendeeTypeFromRequiredToOptionalInRepeatingEventForMainInstAndFuture() {
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(user2.getEmail()), Cf.list());
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();


        data.setAttendeeEmails(Option.of(Cf.list()));
        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.of(true), Option.empty());

        val event = getEvent(user.getUid(), eventId);

        checkThatAttendeeHasBecomeOptional(event);
    }

    @Test
    public void changeAttendeeTypeFromRequiredToOptionalInRepeatingEventForSingleInst() {
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(user2.getEmail()), Cf.list());
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();

        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        data.setAttendeeEmails(Option.of(Cf.list()));
        val instanceStartTs = data.getStartTs().get().getDateTime().plusDays(7).minusHours(3).toLocalDateTime();
        val recurrenceEventId = eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.of(false), Option.of(instanceStartTs))
                .asModified().getShowEventId();

        val event = getEvent(user.getUid(), recurrenceEventId);

        checkThatAttendeeHasBecomeOptional(event);
    }

    @Test
    public void changeAttendeeTypeFromRequiredToOptionalInRepeatingEventForRecurrenceInst() {
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(user2.getEmail()), Cf.list());
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();

        data.setName(Option.of("Some test name"));
        val instanceStartTs = data.getStartTs().get().getDateTime().plusDays(7).minusHours(3).toLocalDateTime();
        val recurrenceEventId = eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.of(false), Option.of(instanceStartTs))
                .asModified().getShowEventId();

        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        data.setAttendeeEmails(Option.of(Cf.list()));
        data.setStartTs(Option.of(WebDateTime.localDateTime(instanceStartTs)));
        data.setEndTs(Option.of(WebDateTime.localDateTime(instanceStartTs.plusHours(1))));
        eventActions.updateEvent(user.getUid(), Option.of(recurrenceEventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.empty(), Option.empty());

        val event = getEvent(user.getUid(), recurrenceEventId);

        checkThatAttendeeHasBecomeOptional(event);
    }

    @Test
    public void changeAttendeeTypeFromRequiredToOptionalInRepeatingEventForThisAndFuture() {
        val data = consBaseData(Duration.ZERO, Duration.ZERO, true, NOW, Cf.list(user2.getEmail()), Cf.list());
        val eventId = createEvent(user.getUid(), data).asModified().getShowEventId();

        data.setName(Option.of("Some test name"));
        val instanceStartTs = data.getStartTs().get().getDateTime().plusDays(7).minusHours(3).toLocalDateTime();
        val recurrenceEventId = eventActions.updateEvent(user.getUid(), Option.of(eventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.of(false), Option.of(instanceStartTs))
                .asModified().getShowEventId();

        data.setOptionalAttendeeEmails(Option.of(Cf.list(user2.getEmail())));
        data.setAttendeeEmails(Option.of(Cf.list()));
        data.setStartTs(Option.of(WebDateTime.localDateTime(instanceStartTs)));
        data.setEndTs(Option.of(WebDateTime.localDateTime(instanceStartTs.plusHours(1))));
        val newMasterEventId = eventActions.updateEvent(user.getUid(), Option.of(recurrenceEventId), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), data, Option.empty(), Option.of(true), Option.empty())
                .asModified().getShowEventId();

        val event = getEvent(user.getUid(), newMasterEventId);

        checkThatAttendeeHasBecomeOptional(event);
    }

    private void checkThatAttendeeHasBecomeOptional(WebEventInfo event) {
        assertThat(event.getAttendees().size()).isEqualTo(0);
        assertThat(event.getTotalAttendees()).isEqualTo(0);
        assertThat(event.getOptionalAttendees().size()).isEqualTo(1);
        assertThat(event.getTotalOptionalAttendees()).isEqualTo(1);
    }

    private void checkThatAttendeeHasBecomeRequired(WebEventInfo event) {
        assertThat(event.getAttendees().size()).isEqualTo(1);
        assertThat(event.getTotalAttendees()).isEqualTo(1);
        assertThat(event.getOptionalAttendees().size()).isEqualTo(0);
        assertThat(event.getTotalOptionalAttendees()).isEqualTo(0);
    }
}
