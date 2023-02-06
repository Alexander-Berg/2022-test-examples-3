package ru.yandex.reminders.logic.flight;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.commune.bazinga.scheduler.TaskCategory;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.logic.flight.shift.FlightShift;
import ru.yandex.reminders.logic.update.ActionInfo;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;

/**
 * @author Eugene Voytitsky
 */
public class FlightReminderManagerScheduleFlightShiftSendingTest {

    private static final DateTimeZone TZ = DateTimeZone.forID("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZone(TZ);

    @Test
    public void scheduleFlightShiftSmsSending1() {
        // case: 1). delay because of night rule, 2). no additional 30 min delay, 3). task scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2013-11-02 06:00:00", "2013-11-01 15:00:00"), actionInfo("2013-11-01 01:30:00"));

        Assert.equals(parse("2013-11-01 08:00:00"), sendTs);

        verify(mock).schedule(any(OnetimeTask.class), any(TaskCategory.class), eq(sendTs), anyInt(), anyBoolean());
    }

    @Test
    public void scheduleFlightShiftSmsSending2() {
        // case: 1). no delay because of night rule, 2). additional 30 min delay, 3). task scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2014-02-11 06:40:00", "2014-02-11 05:40:00"), actionInfo(parse("2014-02-10 23:50:00")));

        Assert.equals(parse("2014-02-11 00:20:00"), sendTs);

        verify(mock).schedule(any(OnetimeTask.class), any(TaskCategory.class), eq(sendTs), anyInt(), anyBoolean());
    }

    @Test
    public void scheduleFlightShiftSmsSending3() {
        // case: 1). no delay because of night rule, 2). no additional 30 min delay, 3). task scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2014-02-11 06:40:00", "2014-02-11 05:40:00"), actionInfo(parse("2014-02-10 22:50:00")));

        Assert.equals(parse("2014-02-10 22:50:00"), sendTs);

        verify(mock).schedule(any(OnetimeTask.class), any(TaskCategory.class), eq(sendTs), anyInt(), anyBoolean());
    }

    @Test
    public void scheduleFlightShiftSmsSending4() {
        // case: 1). no delay because of night rule, 2). additional 30 min delay, 3). task scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2013-11-01 20:00:00", "2013-11-01 21:00:00"), actionInfo(parse("2013-11-01 10:30:00")));

        Assert.equals(parse("2013-11-01 11:00:00"), sendTs);

        verify(mock).schedule(any(OnetimeTask.class), any(TaskCategory.class), eq(sendTs), anyInt(), anyBoolean());
    }

    @Test
    public void scheduleFlightShiftSmsSending5() {
        // case: 1). no delay because of night rule, 2). no additional 30 min delay, 3). task scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant now = parse("2013-11-01 18:30:00");
        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2013-11-01 20:00:00", "2013-11-01 21:00:00"), actionInfo(now));

        Assert.equals(now, sendTs);

        verify(mock).schedule(any(OnetimeTask.class), any(TaskCategory.class), eq(sendTs), anyInt(), anyBoolean());
    }

    @Test
    public void scheduleFlightShiftSmsSending6() {
        // case: actual flight time is in the past:
        // 1). no delay because of night rule, 2). no additional 30 min delay, 3). task wasn't scheduled
        BazingaTaskManager mock = mock(BazingaTaskManager.class);

        FlightReminderManager manager = new FlightReminderManager(mock);

        Instant now = parse("2013-12-01 10:30:00");
        Instant sendTs = manager.scheduleFlightShiftSmsSending(
                flightShift("2013-11-01 20:00:00", "2013-11-01 21:00:00"), actionInfo(now));

        Assert.equals(now, sendTs);

        verifyNoMoreInteractions(mock);
    }

    private Instant parse(String s) {
        return FORMATTER.parseDateTime(s).toInstant();
    }

    private FlightShift flightShift(String planned, String actual) {
        return new FlightShift("DONT_MATTER", 123, TZ, parse(planned), parse(actual));
    }

    private ActionInfo actionInfo(String now) {
        return actionInfo(parse(now));
    }

    private ActionInfo actionInfo(Instant now) {
        return new ActionInfo(now, "foo", Option.<String>none());
    }
}
