package ru.yandex.market.jmf.timings.test;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.timings.impl.TimingScriptServiceApi;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class TimingScriptServiceApiTest {
    //-8GMT in January-February
    private static final long LA_REGION_ID = 200;
    private static final String LA_ZONE_ID = "America/Los_Angeles";
    //+3GMT
    private static final long MOSCOW_REGION_ID = 213;
    private static final String MOSCOW_ZONE_ID = "Europe/Moscow";

    /**
     * Тесты гоняются в этом времени. Если вдруг найдется проблема в какое-то определенное время, нужно будет
     * написать и на него тест
     */
    private static final Instant MOMENT = Dates.parseDateTime("13.06.2022 13:24:57").toInstant();

    @Inject
    TimingScriptServiceApi timingApi;
    @Inject
    ServiceTimeTestUtils utils;
    @Inject
    Geobase geobaseService;


    @BeforeEach
    public void setUp() {
        Mockito.when(geobaseService.getTimeZoneByRegionId(Mockito.eq(MOSCOW_REGION_ID))).thenReturn(MOSCOW_ZONE_ID);
        Mockito.when(geobaseService.getTimeZoneByRegionId(Mockito.eq(LA_REGION_ID))).thenReturn(LA_ZONE_ID);
    }

    @ParameterizedTest(name = "{index} => zone={1}; period=({2}, {3}); expectedDuration={4}")
    @CsvSource({
            LA_REGION_ID + ", " + LA_ZONE_ID + ", 1, 2, 1",
            MOSCOW_REGION_ID + ", " + MOSCOW_ZONE_ID + ", 25, 26, 25",
            MOSCOW_REGION_ID + ", " + MOSCOW_ZONE_ID + ", -2, 2, 0",
    })
    void durationToStartTime(long regionId, String zoneId, int periodStartAfterHours, int periodEndAfterHours,
                             int expectedDuration) {
        Now.withMoment(MOMENT, ZoneId.of(zoneId), () -> {
            ServiceTime serviceTime = utils.createServiceTime();
            var clientNow = Now.offsetDateTime();

            utils.createSingleDayPeriod(serviceTime,
                    clientNow.toLocalDateTime().plusHours(periodStartAfterHours),
                    clientNow.toLocalDateTime().plusHours(periodEndAfterHours)
            );
            Duration duration = timingApi.durationToStartTime(serviceTime, regionId);

            assertEquals(Duration.ofHours(expectedDuration), duration);
        });
    }


    @Test
    public void durationToStartTimeFromDateTimeMinDateBeforeServiceTime() {
        Now.withMoment(MOMENT, ZoneId.of(MOSCOW_ZONE_ID), () -> {
            ServiceTime serviceTime = utils.createServiceTime();
            var clientNow = Now.offsetDateTime();

            utils.createSingleDayPeriod(serviceTime,
                    clientNow.toLocalDateTime().plusHours(3),
                    clientNow.toLocalDateTime().plusHours(10));
            Duration duration = timingApi.durationToStartTime(serviceTime, MOSCOW_ZONE_ID,
                    clientNow.plusHours(1));

            assertEquals(Duration.ofHours(3), duration);
        });
    }

    @Test
    public void durationToStartTimeFromDateTimeWithMinDateInServiceTime() {
        Now.withMoment(MOMENT, ZoneId.of(LA_ZONE_ID), () -> {
            ServiceTime serviceTime = utils.createServiceTime();
            var clientNow = Now.offsetDateTime();

            var clientNextDay = clientNow.withHour(0).plusDays(1);

            OffsetDateTime startTimeFrom = clientNextDay.plusHours(1);
            utils.createSingleDayPeriod(serviceTime,
                    clientNextDay.toLocalDateTime().plusHours(0),
                    clientNextDay.toLocalDateTime().plusHours(5));

            Duration duration = timingApi.durationToStartTime(serviceTime, LA_ZONE_ID, startTimeFrom);

            assertEquals(Duration.between(clientNow, startTimeFrom), duration);
        });
    }


    @ParameterizedTest(name = "{index} => zone={0}; period=({1}, {2}); expected={3}")
    @CsvSource({
            LA_ZONE_ID + ", -1, 3, true",
            MOSCOW_ZONE_ID + ", 1, 3, false",
    })
    public void isNowServiceTime(String zoneId, int periodStartAfterHours, int periodEndAfterHours, boolean expected) {
        Now.withMoment(MOMENT, ZoneId.of(zoneId), () -> {
            ServiceTime serviceTime = utils.createServiceTime();
            var clientNow = Now.offsetDateTime();

            utils.createSingleDayPeriod(serviceTime,
                    clientNow.toLocalDateTime().plusMinutes(periodStartAfterHours),
                    clientNow.toLocalDateTime().plusMinutes(periodEndAfterHours));

            assertEquals(expected, timingApi.isNowServiceTime(serviceTime, zoneId));
        });
    }

}
