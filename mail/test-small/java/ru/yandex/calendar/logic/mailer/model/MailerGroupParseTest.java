package ru.yandex.calendar.logic.mailer.model;

import lombok.val;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsVTimeZones;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.parameter.IcsPartStat;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsRRule;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.Freq;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.IcsRecur;
import ru.yandex.calendar.logic.ics.iv5j.ical.type.recur.type.IcsRecurRulePartByDay;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ParticipantKind;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class MailerGroupParseTest {
    @Test
    public void repeatingEvent() {
        Function<ListF<Email>, Tuple2List<Email, ParticipantId>> participantIds = emails -> emails.zipWith(e -> {
            switch (e.getDomain().getDomain()) {
                case "resources": return ParticipantId.resourceId(Long.parseLong(e.getLocalPart()));
                case "users": return ParticipantId.yandexUid(PassportUid.cons(Long.parseLong(e.getLocalPart())));
                default: return ParticipantId.invitationIdForExternalUser(e);
            }
        });

        val tzs = IcsVTimeZones.cons(Cf.list(), MoscowTime.TZ, false);

        val untilDate = new LocalDate(2031, 1, 1);

        var vevent = new IcsVEvent();
        vevent = vevent.withSummary("Master");
        vevent = vevent.withUid("UID");

        vevent = vevent.withDtStart(MoscowTime.dateTime(2030, 7, 6, 21, 30));
        vevent = vevent.withDtEnd(MoscowTime.dateTime(2030, 7, 6, 22, 0));

        vevent = vevent.addProperty(new IcsRRule(new IcsRecur(Freq.WEEKLY)
                .withInterval(2).withUntilDate(untilDate).withPart(new IcsRecurRulePartByDay("TU,MO"))));

        vevent = vevent.withOrganizer(new Email("1@users"));
        vevent = vevent.addAttendee(new Email("2@users"), IcsPartStat.TENTATIVE);
        vevent = vevent.addAttendee(new Email("3@resources"), IcsPartStat.ACCEPTED);
        vevent = vevent.addAttendee(new Email("4@external"), IcsPartStat.NEEDS_ACTION);

        vevent = vevent.withDtStamp(MoscowTime.instant(2030, 7, 6, 22, 30));
        vevent = vevent.withSequenece(37);

        val group = MailerGroup.fromIcs(PassportUid.cons(1),
                vevent.makeCalendar(), tzs, participantIds, Instant.now());
        val event = group.getEvents().single();

        assertThat(event.getStartTs()).isEqualTo(vevent.getStart(tzs));
        assertThat(event.getEndTs()).isEqualTo(vevent.getEnd(tzs));
        assertThat(event.getIsAllDay()).isFalse();

        vevent.getSummary().ifPresent(s -> assertThat(event.getName()).isEqualTo(s));

        assertThat(event.getSequence()).isEqualTo(37);
        vevent.getDtStampInstant(tzs).ifPresent(d -> assertThat(event.getDtstamp()).isEqualTo(d));

        assertThat(event.getOrganizer().map(MailerParticipant::getId).toOptional())
                .contains(ParticipantId.yandexUid(PassportUid.cons(1)));

        assertThat(event.getAttendees().attendees.map(p -> p.getId().getKind()))
                .containsExactly(ParticipantKind.YANDEX_USER, ParticipantKind.RESOURCE, ParticipantKind.EXTERNAL_USER);

        assertThat(event.getAttendees().attendees.map(p -> p.decision))
                .containsExactly(Decision.MAYBE, Decision.YES, Decision.UNDECIDED);

        assertThat(event.getRepetition().get().type).isEqualTo(RegularRepetitionRule.WEEKLY);
        assertThat(event.getRepetition().get().each).isEqualTo(2);

        assertThat(event.getRepetition().get().due.toOptional()).contains(untilDate);
        assertThat(event.getRepetition().get().weeklyDays.toOptional()).contains(Cf.list("mon", "tue"));
    }
}
