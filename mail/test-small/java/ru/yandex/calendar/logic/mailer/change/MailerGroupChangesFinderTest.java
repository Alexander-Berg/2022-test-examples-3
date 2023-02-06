package ru.yandex.calendar.logic.mailer.change;

import lombok.val;
import org.joda.time.DateTimeConstants;
import org.joda.time.Instant;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.generated.MailerEvent;
import ru.yandex.calendar.logic.beans.generated.MailerEventFields;
import ru.yandex.calendar.logic.beans.generated.MailerEventHelper;
import ru.yandex.calendar.logic.mailer.model.MailerAttendees;
import ru.yandex.calendar.logic.mailer.model.MailerGroup;
import ru.yandex.calendar.util.base.Cf2;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class MailerGroupChangesFinderTest {

    @Test
    public void createRepeatingWithRecurrence() {
        val master = createEvent(0, Option.empty());
        val recurrence = createEvent(0, Option.of(1));

        val changes = MailerGroupChangesFinder.changes(
                Option.empty(), new MailerGroup(Cf.list(recurrence, master)), false);

        assertThat(changes.getForMail().isCreated()).isTrue();
        assertThat(changes.getForMail().getCurrentOrPrevious()).isEqualTo(master);

        assertThat(changes.getCreate()).hasSize(2);
        assertThat(isSame(changes.getCreate().first(), recurrence)).isTrue();
        assertThat(isSame(changes.getCreate().last(), master)).isTrue();

        assertThat(changes.getDelete()).isEmpty();
        assertThat(changes.getUpdate()).isEmpty();
    }

    @Test
    public void replaceRecurrenceByMaster() {
        val recurrence = createEvent(1, Option.of(1));
        val master = createEvent(0, Option.empty());

        val changes = MailerGroupChangesFinder.changes(
                Option.of(new MailerGroup(Cf.list(recurrence))), new MailerGroup(Cf.list(master)), false);

        assertThat(changes.getForMail().isCreated()).isTrue();
        assertThat(changes.getForMail().getCurrentOrPrevious()).isEqualTo(master);

        assertThat(isSame(master, changes.getCreate().single())).isTrue();
        assertThat(changes.getDelete().single().asTuple()).isEqualTo(recurrence.getId());
        assertThat(changes.getUpdate()).isEmpty();
    }

    @Test
    public void acceptOutdatedOccurrence() {
        val master = createEvent(1, Option.empty());
        val recurrence = createEvent(0, Option.of(1));

        val changes = MailerGroupChangesFinder.changes(
                Option.of(new MailerGroup(Cf.list(master))), new MailerGroup(Cf.list(recurrence)), false);

        assertThat(changes.getForMail().isUpdated()).isTrue();
        assertThat(changes.getForMail().getCurrentOrPrevious()).isEqualTo(recurrence);
        assertThat(changes.getForMail().getPrevious().toOptional()).contains(master);

        assertThat(isSame(recurrence, changes.getCreate().single())).isTrue();
        assertThat(changes.getUpdate()).isEmpty();
        assertThat(changes.getDelete()).isEmpty();
    }

    @Test
    public void cancelMasterWithRecurrences() {
        val master = createEvent(0, Option.empty());
        val recurrence = createEvent(1, Option.of(1));

        val cancelled = createEvent(2, Option.empty());

        val changes = MailerGroupChangesFinder.changes(
                Option.of(new MailerGroup(Cf.list(master, recurrence))), new MailerGroup(Cf.list(cancelled)), true);

        assertThat(changes.getForMail().isCancelled()).isTrue();
        assertThat(changes.getForMail().getPrevious().toOptional()).contains(cancelled);

        assertThat(changes.getDelete()).hasSize(2);
        assertThat(changes.getUpdate()).isEmpty();
        assertThat(changes.getCreate()).isEmpty();
    }

    @Test
    public void cancelRecurrence() {
        val master = createEvent(0, Option.empty());
        val recurrence1 = createEvent(1, Option.of(1));
        val recurrence2 = createEvent(2, Option.of(2));

        val cancelled2 = createEvent(4, Option.of(2));

        val changes = MailerGroupChangesFinder.changes(
                Option.of(new MailerGroup(Cf.list(master, recurrence1, recurrence2))),
                new MailerGroup(Cf.list(cancelled2)), true);

        assertThat(changes.getForMail().isCancelled()).isTrue();
        assertThat(changes.getForMail().getPrevious().toOptional()).contains(cancelled2);

        assertThat(changes.getDelete().single().asTuple()).isEqualTo(recurrence2.getId());
        assertThat(changes.getUpdate()).isEmpty();
        assertThat(changes.getCreate()).isEmpty();
    }

    @Test
    public void ignoreStaleEvents() {
        Tuple2List<Option<Integer>, Boolean> cases = Cf2.join(
                Cf.list(Option.empty(), Option.of(1)), Cf.list(false, true));

        cases.forEach((recurId, cancel) -> assertThat(
                MailerGroupChangesFinder.changes(
                        Option.of(new MailerGroup(Cf.list(createEvent(1, recurId)))),
                        new MailerGroup(Cf.list(createEvent(0, recurId))), cancel).getForMail().isIgnored()
                ).withFailMessage(recurId + " " + cancel + " was not ignored").isTrue());
    }


    private static MailerEvent createEvent(int sequence, Option<Integer> recurId) {
        var event = new MailerEvent();
        event.setUid(PassportUid.cons(1));
        event.setExternalId("UUID");
        event.setInstanceId(recurId.map(i -> (long) i * DateTimeConstants.MILLIS_PER_DAY).getOrElse(0L));

        event.setTimezoneId(MoscowTime.TZ.getID());

        event.setSequence(sequence);
        event.setDtstamp(Instant.now());
        event.setRecurrenceId(Option.when(event.getInstanceId() > 0, event::getInstanceId).map(Instant::new));

        event.setAttendees(new MailerAttendees(Cf.list()));

        return event;
    }

    private static boolean isSame(MailerEvent first, MailerEvent second) {
        first = first.copy();
        first.unsetField(MailerEventFields.CREATED);
        first.unsetField(MailerEventFields.MODIFIED);

        second = second.copy();
        second.unsetField(MailerEventFields.CREATED);
        second.unsetField(MailerEventFields.MODIFIED);

        return MailerEventHelper.INSTANCE.findChanges(first, second).cardinality() == 0;
    }
}
