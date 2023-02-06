package ru.yandex.calendar.frontend.webNew;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.ReadableDuration;
import org.joda.time.ReadableInstant;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0;
import ru.yandex.calendar.CalendarRequest;
import ru.yandex.calendar.CalendarRequestHandle;
import ru.yandex.calendar.RemoteInfo;
import ru.yandex.calendar.frontend.webNew.actions.WebNewActionsContextConfiguration;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.test.generic.TestBaseContextConfiguration;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

/**
 * @author dbrylev
 */
@ContextConfiguration(classes = {
        TestBaseContextConfiguration.class,
        WebNewContextConfiguration.class,
        WebNewActionsContextConfiguration.class,
})
public abstract class WebNewTestBase extends AbstractConfTest {

    @Autowired
    protected TestManager testManager;

    private CalendarRequestHandle handle;

    protected TestUserInfo user;
    protected PassportUid uid;

    protected TestUserInfo user2;
    protected PassportUid uid2;

    protected ReadableInstant now() {
        return Instant.now();
    }

    @Before
    public void setup() {
        testManager.prepareResourceMaster();
        user = testManager.prepareRandomYaTeamUser(1000);
        user2 = testManager.prepareRandomYaTeamUser(2000);

        uid = user.getUid();
        uid2 = user2.getUid();

        handle = CalendarRequest.push(
                new RemoteInfo(Option.empty(), Option.empty()), ActionSource.WEB, "test", "test", now().toInstant(), true);
    }

    @After
    public void teardown() {
        handle.popSafely();
    }

    protected <R> R runWithActionSource(ActionSource source, Function0<R> action) {
        CalendarRequestHandle handle = CalendarRequest.push(
                new RemoteInfo(Option.empty(), Option.empty()), source, "test", "test", now().toInstant(), true);
        try {
            return action.apply();
        } finally {
            handle.popSafely();
        }
    }

    protected Event createUserEvent(ReadableInstant start, TestUserInfo user) {
        return createUserEvent(start, Duration.standardHours(1), user, Option.empty());
    }

    protected Event createUserEvent(ReadableInstant start, ReadableDuration duration, TestUserInfo user) {
        return createUserEvent(start, duration, user, Option.empty());
    }

    protected Event createUserEvent(
            ReadableInstant start, TestUserInfo organizer, Option<Email> external, TestUserInfo... attendees)
    {
        return createUserEvent(start, Duration.standardHours(1), organizer, external, attendees);
    }

    protected Event createUserEvent(
            ReadableInstant start, ReadableDuration duration,
            TestUserInfo user, Option<Email> external, TestUserInfo... attendees)
    {
        // TODO: strange fix, but it works!! `uid` replaced by `user.getUid()`
        Event event = testManager.createDefaultEvent(
                user.getUid(), "Event", start.toInstant(), start.toInstant().plus(duration));

        testManager.addUserParticipantToEvent(event.getId(), user.getUid(), Decision.YES, true);

        if (external.isPresent()) {
            testManager.addExternalUserParticipantToEvent(event.getId(), external.get(), Decision.UNDECIDED, false);
        }
        Cf.x(attendees).forEach(a ->
                testManager.addUserParticipantToEvent(event.getId(), a.getUid(), Decision.MAYBE, false));

        testManager.updateEventTimeIndents(event);

        return event;
    }

    protected static RepetitionData consDailyRepetitionData() {
        return RepetitionData.fromRepetition(TestManager.createDailyRepetitionTemplate(), MoscowTime.TZ);
    }
}
