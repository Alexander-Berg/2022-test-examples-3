package ru.yandex.calendar.logic.sending.param;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.repetition.RepetitionInstanceInfo;
import ru.yandex.calendar.logic.ics.exp.EventInstanceParameters;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.dates.DateTimeManager;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.InstantInterval;

/**
 * @author ssytnik
 */
public class EventMessageInfoTest extends AbstractConfTest {
    @Autowired
    TestManager testManager;
    @Autowired
    DateTimeManager dateTimeManager;
    @Autowired
    private EventMessageInfoCreator eventMessageInfoCreator;
    @Autowired
    private EventDbManager eventDbManager;

    @Test
    public void create() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-15400");
        PassportUid uid = user.getUid();

        Event event = testManager.createEventWithDailyRepetition(uid);
        event.setDescription("");
        event.setLocation("");
        event.setSid(PassportSid.CALENDAR);
        event.setCreationSource(ActionSource.WEB);

        DateTimeZone tz = dateTimeManager.getTimeZoneForUid(uid);

        Instant eventInstanceStart = new DateTime(event.getStartTs(), tz).plusDays(3).toInstant();
        EventMessageInfo emi = eventMessageInfoCreator.create(
                eventDbManager.getEventWithRelationsByEvent(event),
                RepetitionInstanceInfo.noRepetition(new InstantInterval(event.getStartTs(), event.getEndTs())),
                new EventInstanceParameters(eventInstanceStart, eventInstanceStart, Option.empty()),
                user.getEmail(), Option.of(uid), Language.RUSSIAN, tz);

        final LocalDate expectedEltLocalDate = new LocalDate(eventInstanceStart, tz);
        final LocalDate actualEltLocalDate = emi.getEventStartTs().toLocalDate();
        Assert.A.equals(expectedEltLocalDate, actualEltLocalDate);
    }
}
