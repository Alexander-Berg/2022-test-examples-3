package ru.yandex.market.tpl.core.domain.routing.schedule;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.FAKE_DS_ID;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RoutingRequestWaveServiceTest {

    public static final int SORTING_CENTER_ID = 1;
    private final RoutingScheduleRuleRepository routingScheduleRuleRepository;
    private final RoutingRequestWaveService routingRequestWaveService;
    private final OrderGenerateService orderGenerateService;
    private final DsRepository dsRepository;
    private final Clock clock;

    @Test
    void shouldIsLastTrue_whenRequestExactlyAtSchedule() {
        //given
        //Schedule
        //21:00  sameDay = false
        //22:00  sameDay = false
        prepareScheduleRules(LocalTime.of(21,0), false);
        LocalTime lastWaveTime = LocalTime.of(22, 0);

        prepareScheduleRules(lastWaveTime, false);

        LocalDate routingDate = LocalDate.of(2021, 5, 19);

        Instant expectedLastRequestDateTime =
                LocalDateTime.of(routingDate.minusDays(1), lastWaveTime).atZone(MOSCOW_ZONE).toInstant();


        //then
        //For Shift 2021.05.19 Request in 2021.05.18 22:00 -  is last wave
        assertTrue(routingRequestWaveService.isLastScheduledWave(SORTING_CENTER_ID, routingDate,
                expectedLastRequestDateTime));
    }

    @Test
    void shouldIsLastTrue_whenRequestAfterLast() {
        //given
        prepareScheduleRules(LocalTime.of(21,0), false);
        LocalTime lastWaveTime = LocalTime.of(22, 0);
        prepareScheduleRules(lastWaveTime, false);

        LocalDate routingDate = LocalDate.of(2021, 5, 19);

        Instant expectedLastRequestDateTime =
                LocalDateTime.of(routingDate.minusDays(1), lastWaveTime.plusHours(1)).atZone(MOSCOW_ZONE).toInstant();


        //then
        assertTrue(routingRequestWaveService.isLastScheduledWave(SORTING_CENTER_ID, routingDate,
                expectedLastRequestDateTime));
    }

    @Test
    void shouldIsLastTrue_whenRequestAfterLast_SameDay() {
        //given
        //Schedule
        //21:00  sameDay = false
        //01:00  sameDay = true
        prepareScheduleRules(LocalTime.of(21,0), false);
        LocalTime lastWaveTime = LocalTime.of(1, 0);
        prepareScheduleRules(lastWaveTime, true);

        LocalDate routingDate = LocalDate.of(2021, 5, 19);

        Instant expectedLastRequestDateTime =
                LocalDateTime.of(routingDate, lastWaveTime).atZone(MOSCOW_ZONE).toInstant();


        //then
        //For Shift 2021.05.19 Request in 2021.05.19 01:00 -  is last wave
        assertTrue(routingRequestWaveService.isLastScheduledWave(SORTING_CENTER_ID, routingDate,
                expectedLastRequestDateTime));
    }

    @Test
    void shouldIsLastFalse_whenRequestAfterLast_SameDay() {
        //given
        //Schedule
        //21:00  sameDay = false
        //01:00  sameDay = true
        prepareScheduleRules(LocalTime.of(21,0), false);
        LocalTime lastWaveTime = LocalTime.of(1, 0);
        prepareScheduleRules(lastWaveTime, true);

        LocalDate routingDate = LocalDate.of(2021, 5, 19);

        Instant expectedLastRequestDateTime =
                LocalDateTime.of(routingDate, lastWaveTime.minusHours(1)).atZone(MOSCOW_ZONE).toInstant();


        //then
        //For Shift 2021.05.19 Request in 2021.05.19 00:00 -  is NOT last wave
        assertFalse(routingRequestWaveService.isLastScheduledWave(SORTING_CENTER_ID, routingDate,
                expectedLastRequestDateTime));
    }

    @Test
    void shouldIsLastFalse_whenRequestAfterLast() {
        //given
        prepareScheduleRules(LocalTime.of(21,0), false);
        LocalTime lastWaveTime = LocalTime.of(22, 0);
        prepareScheduleRules(lastWaveTime, false);

        LocalDate routingDate = LocalDate.of(2021, 5, 19);

        Instant expectedLastRequestDateTime =
                LocalDateTime.of(routingDate.minusDays(1), lastWaveTime.minusHours(1)).atZone(MOSCOW_ZONE).toInstant();


        //then
        assertFalse(routingRequestWaveService.isLastScheduledWave(SORTING_CENTER_ID, routingDate,
                expectedLastRequestDateTime));
    }

    @Test
    void getFirstWaveTime_whenSameDayFalse() {
        //given
        //Schedule
        //21:00  sameDay = false
        //22:00  sameDay = false
        LocalTime firstWaveTime = LocalTime.of(21, 0);
        prepareScheduleRules(firstWaveTime, false);
        prepareScheduleRules(firstWaveTime.plusHours(1L), false);

        LocalDate routeDate = LocalDate.of(2021,5,20);

        //when
        Optional<LocalDateTime> calculatedFirstWaveTime = routingRequestWaveService.getFirstRoutingWaveTime(FAKE_DS_ID,
                routeDate);


        //then
        assertTrue(calculatedFirstWaveTime.isPresent());
        assertEquals(LocalDateTime.of(routeDate.minusDays(1L), firstWaveTime), calculatedFirstWaveTime.get());
    }

    @Test
    void getFirstWaveTime_whenSameDayMixed() {
        //given
        //Schedule
        //21:00  sameDay = false
        //2:00  sameDay = true
        LocalTime firstWaveTime = LocalTime.of(21, 0);
        prepareScheduleRules(firstWaveTime, false);
        prepareScheduleRules(LocalTime.of(2,0), true);

        LocalDate routeDate = LocalDate.of(2021,5,20);

        //when
        Optional<LocalDateTime> calculatedFirstWaveTime = routingRequestWaveService.getFirstRoutingWaveTime(FAKE_DS_ID,
                routeDate);


        //then
        assertTrue(calculatedFirstWaveTime.isPresent());
        assertEquals(LocalDateTime.of(routeDate.minusDays(1L), firstWaveTime), calculatedFirstWaveTime.get());
    }

    @Test
    void getFirstWaveTime_whenSameDayTrue() {
        //given
        //Schedule
        //1:00  sameDay = true
        //2:00  sameDay = true
        LocalTime firstWaveTime = LocalTime.of(1, 0);
        prepareScheduleRules(firstWaveTime, true);
        prepareScheduleRules(LocalTime.of(2,0), true);

        LocalDate routeDate = LocalDate.of(2021,5,20);

        //when
        Optional<LocalDateTime> calculatedFirstWaveTime = routingRequestWaveService.getFirstRoutingWaveTime(FAKE_DS_ID,
                routeDate);


        //then
        assertTrue(calculatedFirstWaveTime.isPresent());
        assertEquals(LocalDateTime.of(routeDate, firstWaveTime), calculatedFirstWaveTime.get());
    }

    private void prepareScheduleRules(LocalTime lastWaveTime, boolean sameDay) {
        RoutingScheduleRule routingScheduleRule = RoutingScheduleRuleUtil.routingScheduleRule(SORTING_CENTER_ID, sameDay,
                null, lastWaveTime);
        routingScheduleRuleRepository.save(routingScheduleRule);
    }
}
