package ru.yandex.market.jmf.timings.test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.geo.Geobase;
import ru.yandex.market.jmf.timings.impl.TimingScriptServiceApi;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class TimingScriptServiceApiTest {
    //-8GMT in January-February
    private static final long LA_REGION_ID = 200;
    private static final String LA_ZONE_ID = "America/Los_Angeles";
    //+3GMT
    private static final long MOSCOW_REGION_ID = 213;
    private static final String MOSCOW_ZONE_ID = "Europe/Moscow";

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

    @AfterEach
    public void tearDown() {
        Mockito.reset(geobaseService);
    }

    @ParameterizedTest(name = "{index} => zone={1}; period=({2}, {3}); expectedDuration={4}")
    @CsvSource({
            LA_REGION_ID + ", " + LA_ZONE_ID + ", 1, 2, 1",
            MOSCOW_REGION_ID + ", " + MOSCOW_ZONE_ID + ", 25, 26, 25",
            MOSCOW_REGION_ID + ", " + MOSCOW_ZONE_ID + ", -2, 2, 0",
    })
    @Transactional
    void durationToStartTime(long regionId, String zoneId, int periodStartAfterHours, int periodEndAfterHours,
                             int expectedDuration) {
        ServiceTime serviceTime = utils.createServiceTime();
        ZonedDateTime clientNow = ZonedDateTime.now(ZoneId.of(zoneId));

        utils.createSingleDayPeriod(serviceTime,
                clientNow.toLocalDateTime().plusHours(periodStartAfterHours),
                clientNow.toLocalDateTime().plusHours(periodEndAfterHours)
        );
        Duration duration = timingApi.durationToStartTime(serviceTime, regionId);

        assertDurationEqualsWithTolerance(Duration.ofHours(expectedDuration), duration, Duration.ofMinutes(10));
    }


    @Test
    @Transactional
    public void durationToStartTimeFromDateTimeMinDateBeforeServiceTime() {
        ServiceTime serviceTime = utils.createServiceTime();
        ZonedDateTime clientNow = ZonedDateTime.now(ZoneId.of(MOSCOW_ZONE_ID));

        utils.createSingleDayPeriod(serviceTime,
                clientNow.toLocalDateTime().plusHours(3),
                clientNow.toLocalDateTime().plusHours(10));
        Duration duration = timingApi.durationToStartTime(serviceTime, MOSCOW_ZONE_ID,
                clientNow.toOffsetDateTime().plusHours(1));

        assertDurationEqualsWithTolerance(Duration.ofHours(3), duration, Duration.ofMinutes(10));
    }

    @Test
    @Transactional
    public void durationToStartTimeFromDateTimeWithMinDateInServiceTime() {
        ServiceTime serviceTime = utils.createServiceTime();
        ZonedDateTime clientNow = ZonedDateTime.now(ZoneId.of(LA_ZONE_ID));

        ZonedDateTime clientNextDay = clientNow.withHour(0).plusDays(1);

        OffsetDateTime startTimeFrom = clientNextDay.toOffsetDateTime().plusHours(1);
        utils.createSingleDayPeriod(serviceTime,
                clientNextDay.toLocalDateTime().plusHours(0),
                clientNextDay.toLocalDateTime().plusHours(5));

        Duration duration = timingApi.durationToStartTime(serviceTime, LA_ZONE_ID, startTimeFrom);

        assertDurationEqualsWithTolerance(Duration.between(clientNow, startTimeFrom), duration, Duration.ofMinutes(10));
    }


    @ParameterizedTest(name = "{index} => zone={0}; period=({1}, {2}); expected={3}")
    @CsvSource({
            LA_ZONE_ID + ", -1, 3, true",
            MOSCOW_ZONE_ID + ", 1, 3, false",
    })
    @Transactional
    public void isNowServiceTime(String zoneId, int periodStartAfterHours, int periodEndAfterHours, boolean expected) {
        ServiceTime serviceTime = utils.createServiceTime();
        ZonedDateTime clientNow = ZonedDateTime.now(ZoneId.of(zoneId));

        // FIXME Тест может быть не стабилен в последнюю минуту часа (с 23.59 до 00:00 только с лету не соображу по какому часовому поясу)
        utils.createSingleDayPeriod(serviceTime,
                clientNow.toLocalDateTime().plusMinutes(periodStartAfterHours),
                clientNow.toLocalDateTime().plusMinutes(periodEndAfterHours));

        assertEquals(expected, timingApi.isNowServiceTime(serviceTime, zoneId));
    }

    private void assertDurationEqualsWithTolerance(Duration expected, Duration actual, Duration epsilon) {
        var difference = actual.minus(expected).abs();
        assertTrue(difference.toMillis() < epsilon.abs().toMillis(),
                "Difference " + difference + "bigger than epsilon" + epsilon);
    }

}
