package ru.yandex.market.jmf.timings.test;

import java.time.Duration;
import java.time.OffsetDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.timings.BadServiceTimeException;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.impl.TimingScriptServiceApi;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class TimingServiceTest {

    @Inject
    TxService txService;
    @Inject
    ServiceTimeTestUtils utils;
    @Inject
    TimingScriptServiceApi timingScriptServiceApi;

    @Test()
    public void empty() {
        Entity serviceTime = txService.doInNewTx(() -> utils.createServiceTime());

        assertThrows(BadServiceTimeException.class, () -> {
            txService.doInNewTx(() -> timingScriptServiceApi.startTime(serviceTime, "Europe/Moscow",
                    OffsetDateTime.now()));
        });


    }

    @Test
    public void startTime_inPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-13T15:00:00+03:00");

        assertEquals("Должны получить переданное время т.к. оно попадает в середину интервала обслуживания",
                "2019-02-13T15:00:00+03:00", result);
    }

    @Test
    public void startTime_startPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-13T14:00:00+03:00");

        assertEquals("Должны получить переданное время т.к. оно совпадает с временем начала интервала обслуживания",
                "2019-02-13T14:00:00+03:00", result);
    }

    @Test
    public void startTime_betweenPeriods_oneDay() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-13T13:30:00+03:00");

        assertEquals("Должны получить время начала обслуживания ближайшего интервала",
                "2019-02-13T14:00:00+03:00", result);
    }

    @Test
    public void startTime_betweenPeriods_differentDays() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-13T21:00:00+03:00");

        assertEquals("Должны получить время начала обслуживания ближайшего интервала",
                "2019-02-14T09:00:00+03:00", result);
    }

    @Test
    public void startTime_afterLastWeekPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-15T09:00:00+03:00");

        assertEquals("Должны получить время начала обслуживания ближайшего интервала",
                "2019-02-20T09:00:00+03:00", result);
    }

    @Test
    public void startTime_beforeFirstWeekPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeStartTime(st, "2019-02-11T09:00:00+03:00");

        assertEquals("Должны получить время начала обслуживания ближайшего интервала",
                "2019-02-13T09:00:00+03:00", result);
    }

    @Test
    public void endTime_inPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeEndTime(st, "2019-02-13T14:30:00+03:00", 30);

        assertEquals("Время окончания попадает в тот же интервал",
                "2019-02-13T15:00:00+03:00", result);
    }

    @Test
    public void endTime_longPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeEndTime(st, "2019-02-13T14:30:00+03:00", 1440);

        assertEquals("Время окончания попадает в тот же интервал",
                "2019-02-27T14:30:00+03:00", result);
    }

    @Test
    public void endTime_exception_period() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeEndTime(st, "2019-03-04T08:30:00+03:00", 30);

        assertEquals("Время окончания попадает в интервал исключения",
                "2019-03-04T10:30:00+03:00", result);
    }

    @Test
    public void endTime_exception_redefinedPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeEndTime(st, "2019-03-05T08:30:00+03:00", 150);

        assertEquals("Время окончания попадает в интервал исключения",
                "2019-03-07T09:30+03:00", result);
    }

    @Test
    public void duration_empty() {
        ServiceTime st = createSimpleServiceTime();

        Duration result = executeDuration(st, "2019-02-13T14:30:00+03:00", "2019-02-13T14:30:00+03:00");

        Assertions.assertEquals(Duration.ZERO, result, "Должны получиь 0 т.к. время начала интервала совпадает с " +
                "временем окончания");
    }

    @Test()
    public void duration_negative() {
        ServiceTime st = createSimpleServiceTime();

        assertThrows(BadServiceTimeException.class, () -> {
            // должны получить исключение т.к. время начала позже времени начала
            executeDuration(st, "2019-02-13T15:30:00+03:00", "2019-02-13T14:30:00+03:00");
        });
    }

    @Test
    public void duration_betweenPeriods() {
        ServiceTime st = createSimpleServiceTime();

        Duration result = executeDuration(st, "2019-02-13T13:30:00+03:00", "2019-02-13T13:40:00+03:00");

        Assertions.assertEquals(Duration.ZERO, result, "Должны получить 0 т.к. указанный промежуток " +
                "не пересекается ни с одним временм обслуживания");
    }

    @Test
    public void duration_inPeriod() {
        ServiceTime st = createSimpleServiceTime();

        Duration result = executeDuration(st, "2019-02-13T15:30:00+03:00", "2019-02-13T16:30:00+03:00");

        Assertions.assertEquals(Duration.ofHours(1), result, "Должны получить 1 час т.к. указанный промежуток " +
                "полностью приходится на время обслуживания");
    }

    @Test
    public void duration_inDifferentPeriod() {
        ServiceTime st = createSimpleServiceTime();

        Duration result = executeDuration(st, "2019-02-13T10:30:00+03:00", "2019-02-13T16:30:00+03:00");

        Assertions.assertEquals(Duration.ofHours(5), result, "Должны получиь 5 часов т.к. 2.5 приходятся " +
                "на первый интервал обслуживания и 2.5 на второй");
    }

    @Test
    public void duration_outPeriod() {
        ServiceTime st = createSimpleServiceTime();

        Duration result = executeDuration(st, "2019-02-13T13:30:00+03:00", "2019-02-13T20:30:00+03:00");

        Assertions.assertEquals(Duration.ofHours(4), result, "Должны получить 4 т.к. в указанный " +
                "интервал есть один промежуток обслуживания продолжительностью 4 часа");
    }

    @Test
    public void serviceEndTime_24x7() {
        ServiceTime st = createServiceTime24x7();

        OffsetDateTime result = executeServiceEndTime(st, "2019-01-01T08:59:59+03:00");

        assertNull(result, "Должны получить null т.к. 24x7");

    }

    @Test
    public void serviceEndTime_beforePeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeServiceEndTime(st, "2019-03-07T08:59:59+03:00");

        assertEquals("Должны получить переданное время т.к. не попадает в интервал обслуживания",
                "2019-03-07T08:59:59+03:00", result);

    }

    @Test
    public void serviceEndTime_inPeriod() {
        ServiceTime st = createSimpleServiceTime();

        OffsetDateTime result = executeServiceEndTime(st, "2019-03-07T12:59:59+03:00");

        assertEquals("Должны получить конец периода, т.к. попадает в интервал обслуживания",
                "2019-03-07T13:00:00+03:00", result);
    }

    @Test
    public void serviceEndTime_betweenPeriods() {
        ServiceTime st = createServiceTimeWithEndOfDayPeriods();

        OffsetDateTime result = executeServiceEndTime(st, "2019-03-10T13:00:01+03:00");

        assertEquals("Должны получить переданное время т.к. не попадает в интервал обслуживания",
                "2019-03-10T13:00:01+03:00", result);
    }

    @Test
    public void serviceEndTime_inEndOfDay() {
        ServiceTime st = createServiceTimeWithEndOfDayPeriods();

        OffsetDateTime result = executeServiceEndTime(st, "2019-03-10T23:59:01+03:00");

        assertEquals("Должны получить конец следующего периода, т.к. попадает в конец дня,"
                + " а следующий начинается с начала дня", "2019-03-11T13:00:00+03:00", result);
    }

    @Test
    public void serviceEndTime_inEndOfDayPeriod() {
        ServiceTime st = createServiceTimeWithEndOfDayPeriods();

        OffsetDateTime result = executeServiceEndTime(st, "2019-03-11T23:58:59+03:00");

        assertEquals("Должны получить начало следующего дня, т.к. попадает в интервал до конца дня,"
                + " а следующий не начинается с начала дня", "2019-03-12T00:00:00+03:00", result);
    }

    private void assertEquals(String message, String expected, OffsetDateTime actual) {
        Assertions.assertEquals(OffsetDateTime.parse(expected), actual, message);
    }

    private OffsetDateTime executeStartTime(ServiceTime st, String time) {
        return txService.doInNewTx(() -> timingScriptServiceApi.startTime(st, "Europe/Moscow", time));
    }

    private OffsetDateTime executeEndTime(ServiceTime st, String time, long durationMinuts) {
        return txService.doInNewTx(() -> timingScriptServiceApi.endTime(st, "Europe/Moscow", time,
                Duration.ofMinutes(durationMinuts)));
    }

    private Duration executeDuration(ServiceTime st, String start, String end) {
        return txService.doInNewTx(() -> timingScriptServiceApi.duration(st, "Europe/Moscow", start, end));
    }

    private OffsetDateTime executeServiceEndTime(ServiceTime st, String time) {
        return txService.doInNewTx(() -> timingScriptServiceApi.serviceEndTime(st, "Europe/Moscow", time));
    }

    private ServiceTime createSimpleServiceTime() {
        return txService.doInNewTx(() -> {
            ServiceTime st = utils.createServiceTime();

            utils.createPeriod(st, "wednesday", "09:00", "13:00");
            utils.createPeriod(st, "wednesday", "14:00", "18:00");
            utils.createPeriod(st, "thursday", "09:00", "13:00");

            // Добавляем исключение в день, когда не было времени обслуживания
            utils.createException(st, "2019-03-04", "10:00", "12:00");

            // Добавляем исклчение в среду, переопределяя период
            utils.createException(st, "2019-03-06", "10:00", "12:00");

            return st;
        });
    }

    private ServiceTime createServiceTimeWithEndOfDayPeriods() {
        return txService.doInNewTx(() -> {
            ServiceTime st = utils.createServiceTime();

            utils.createPeriod(st, "monday", "00:00", "13:00");
            utils.createPeriod(st, "monday", "22:00", "23:59");
            utils.createPeriod(st, "sunday", "09:00", "13:00");
            utils.createPeriod(st, "sunday", "22:00", "23:59");

            return st;
        });
    }

    private ServiceTime createServiceTime24x7() {
        return txService.doInNewTx(() -> {
            ServiceTime st = utils.createServiceTime();

            utils.createPeriod(st, "monday", "00:00", "23:59");
            utils.createPeriod(st, "tuesday", "00:00", "23:59");
            utils.createPeriod(st, "wednesday", "00:00", "23:59");
            utils.createPeriod(st, "thursday", "00:00", "23:59");
            utils.createPeriod(st, "friday", "00:00", "23:59");
            utils.createPeriod(st, "saturday", "00:00", "23:59");
            utils.createPeriod(st, "sunday", "00:00", "23:59");

            return st;
        });
    }

}
