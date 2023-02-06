package ru.yandex.calendar.logic.mailer.change;

import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.bolts.function.Function0;
import ru.yandex.calendar.logic.beans.generated.MailerEvent;
import ru.yandex.calendar.logic.mailer.model.MailerAttendees;
import ru.yandex.calendar.logic.mailer.model.MailerParticipant;
import ru.yandex.calendar.logic.sending.param.EventMessageChanged;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class MailerEventChangeTest {
    @Test
    public void eventChanges() {
        var before = new MailerEvent();
        before.setUrl("Url");
        before.setName("Name");
        before.setLocation("Location");
        before.setDescription("Description");

        before.setIsAllDay(false);
        before.setStartTs(MoscowTime.instant(2017, 7, 6, 20, 0));
        before.setEndTs(MoscowTime.instant(2017, 7, 6, 21, 0));

        before.setOrganizer(new MailerParticipant.Resource(1, new Email("1@y-t.ru"), Decision.YES));
        before.setAttendees(new MailerAttendees(Cf.list()));

        before.setExternalId("ExternalId");
        before.setRecurrenceIdNull();
        before.setRepetitionNull();

        MailerEvent after = before.copy();

        Function0<SetF<EventMessageChanged.Field>> changed = () ->
                MailerEventChange.updated(before, after).findChanged().getFields().unique();

        assertThat(changed.apply()).isEmpty();

        final var expected = Cf.hashSet();

        after.setName("Renamed");
        expected.add(EventMessageChanged.Field.NAME);
        assertThat(changed.apply()).isEqualTo(expected);

        after.setOrganizerNull();
        expected.addAll(EventMessageChanged.Field.ORGANIZER, EventMessageChanged.Field.LOCATION);
        assertThat(changed.apply()).isEqualTo(expected);

        after.setAttendees(new MailerAttendees(Cf.list(
                new MailerParticipant.User(PassportUid.cons(1), new Email("1@y-t.ru"), Decision.YES))));

        expected.addAll(EventMessageChanged.Field.GUESTS);
        assertThat(changed.apply()).isEqualTo(expected);

        after.setIsAllDay(true);
        expected.addAll(EventMessageChanged.Field.TIME);
        assertThat(changed.apply()).isEqualTo(expected);
    }

    @Test
    public void attendeesChanges() {
        final var before = Cf.list(
                new MailerParticipant.User(PassportUid.cons(1), new Email("attendeee-1@y-t.ru"), Decision.YES),
                new MailerParticipant.User(PassportUid.cons(2), new Email("attendeee-2@y-t.ru"), Decision.MAYBE),
                new MailerParticipant.External(new Email("external-1@y-t.ru"), "External 1", Decision.UNDECIDED),
                new MailerParticipant.Resource(1, new Email("resource-1@y-t.ru"), Decision.YES));

        final var after = Cf.list(
                new MailerParticipant.User(PassportUid.cons(1), new Email("attendeee-1@y-t.ru"), Decision.NO),
                new MailerParticipant.External(new Email("external-2@y-t.ru"), "External 2", Decision.UNDECIDED),
                new MailerParticipant.Resource(1, new Email("resource-1@y-t.ru"), Decision.YES));

        var eventBefore = new MailerEvent();
        eventBefore.setAttendees(new MailerAttendees(before));

        var eventAfter = new MailerEvent();
        eventAfter.setAttendees(new MailerAttendees(after));

        var change = MailerEventChange.created(eventBefore);

        assertThat(change.getCurrentUserAttendees()).isEqualTo(before.filter(p -> p.getId().isAnyUser()));
        assertThat(change.getNewUserAttendees()).isEmpty();
        assertThat(change.getRemovedUserAttendees()).isEmpty();

        change = MailerEventChange.updated(eventBefore, eventAfter);

        assertThat(change.getRemovedUserAttendees()).isEqualTo(before.subList(1, 3));
        assertThat(change.getCurrentUserAttendees()).isEqualTo(after.subList(0, 1));
        assertThat(change.getNewUserAttendees()).isEqualTo(after.subList(1, 2));
    }
}
