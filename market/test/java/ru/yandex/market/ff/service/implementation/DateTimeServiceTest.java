package ru.yandex.market.ff.service.implementation;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.util.dateTime.DateTimePeriod;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateTimeServiceTest {

    @Test
    void checkNearestWithdrawPeriodInterval() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDate localDate = dateTimeService.localDateNow();

        Pair<Instant, Instant> instantInstantPair = dateTimeService.nearestWithdrawPeriod();
        LocalDate localDateMin = instantInstantPair.getLeft().minus(5, ChronoUnit.DAYS)
                .atZone(TimeZoneUtil.DEFAULT_OFFSET).toLocalDate();
        Duration between = Duration.between(instantInstantPair.getLeft(), instantInstantPair.getRight());

        assertEquals(localDate, localDateMin);
        assertEquals(7, between.toDays());
    }

    @Test
    void isTwoPeriodsOverlapsCase1Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsOverlapsCase2Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 12, 30);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 13, 30);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsOverlapsCase3Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 30);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 30);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsOverlapsCase4Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 30);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 30);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 11, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsNotOverlapsCase1Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 13, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 14, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertFalse(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }


    @Test
    void isTwoPeriodsNotOverlapsCase2Test() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 13, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 11, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 12, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertFalse(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsOverlapsOneInsideAnotherCaseTest() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 11, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 15, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 14, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period1, period2));
    }

    @Test
    void isTwoPeriodsOverlapsOneInsideAnotherSymmetricCaseTest() {
        DateTimeServiceImpl dateTimeService = new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class));
        LocalDateTime from1 = LocalDateTime.of(2021, 1, 1, 11, 0);
        LocalDateTime to1 = LocalDateTime.of(2021, 1, 1, 15, 0);
        DateTimePeriod period1 = new DateTimePeriod(from1, to1);

        LocalDateTime from2 = LocalDateTime.of(2021, 1, 1, 12, 0);
        LocalDateTime to2 = LocalDateTime.of(2021, 1, 1, 14, 0);
        DateTimePeriod period2 = new DateTimePeriod(from2, to2);

        assertTrue(dateTimeService.isTwoPeriodsOverlaps(period2, period1));
    }

    @Test
    void checkNearestCalendaringWithdrawPeriodInterval() {
        DateTimeServiceImpl dateTimeService =
                Mockito.spy(new DateTimeServiceImpl(mock(ConcreteEnvironmentParamService.class)));
        LocalDateTime now = LocalDateTime.parse("2022-03-15T14:00:00");
        when(dateTimeService.localDateTimeNow()).thenReturn(now);

        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.IN_PROGRESS, now.minusDays(2),
                Instant.parse("2022-03-17T21:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.IN_PROGRESS, now.plusHours(4),
                Instant.parse("2022-03-17T21:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.IN_PROGRESS, now.plusDays(1),
                Instant.parse("2022-03-17T21:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.IN_PROGRESS, now.plusDays(7),
                Instant.parse("2022-03-17T21:00:00Z"));

        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.READY_TO_WITHDRAW, now.minusDays(2),
                Instant.parse("2022-03-15T11:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.READY_TO_WITHDRAW, now.plusHours(4),
                Instant.parse("2022-03-15T11:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.READY_TO_WITHDRAW, now.plusDays(1),
                Instant.parse("2022-03-16T11:00:00Z"));
        assertCalendaringWithdrawCorrectPeriod(dateTimeService, RequestStatus.READY_TO_WITHDRAW, now.plusDays(7),
                Instant.parse("2022-03-17T21:00:00Z"));
    }

    private void assertCalendaringWithdrawCorrectPeriod(DateTimeService dateTimeService,
                                                        RequestStatus status,
                                                        LocalDateTime slotDate,
                                                        Instant periodFrom) {
        Pair<Instant, Instant> interval =
                dateTimeService.nearestCalendaringWithdrawPeriod(status, slotDate);
        assertEquals(interval.getLeft(), periodFrom);
        assertEquals(interval.getRight(), Instant.parse("2022-04-14T20:59:59.999999999Z"));
    }
}
