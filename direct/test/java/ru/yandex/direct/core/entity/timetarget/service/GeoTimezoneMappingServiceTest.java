package ru.yandex.direct.core.entity.timetarget.service;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Collection;
import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("ConstantConditions")  // Подсовываем null моку через argThat, оригинальная функция не ожидает null
public class GeoTimezoneMappingServiceTest {

    private static final long UNKNOWN_TIMEZONE_ID = -1L;
    private static final long BELARUS_TIMEZONE_ID = 149L;
    private static final long BELARUS_COUNTRY_ID = 149L;
    private static final long JAVA_UNKNOWN_TIMEZONE_ID = 776L;
    private static final ZoneId BELARUS_ZONE_ID = ZoneId.of("Europe/Minsk");
    private static final GeoTimezone BELARUS_GEO_TIMEZONE =
            new GeoTimezone()
                    .withTimezoneId(BELARUS_TIMEZONE_ID)
                    .withRegionId(BELARUS_COUNTRY_ID)
                    .withTimezone(BELARUS_ZONE_ID);

    private GeoTimezoneMappingService serviceUnderTest;
    private GeoTimezoneRepository geoTimezoneRepository;

    private static Collection<Long> contains(long id) {
        return argThat(ids -> ids != null  // mockito неявно подставляет сюда null
                && ids.contains(id));
    }

    @Before
    public void setUp() throws Exception {
        geoTimezoneRepository = mock(GeoTimezoneRepository.class);
        // на все неизвестные timezoneId репозиторий отвечает null'ом
        when(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(any()))
                .thenReturn(Collections.emptySet());
        when(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(contains(BELARUS_TIMEZONE_ID)))
                .thenReturn(Collections.singleton(BELARUS_GEO_TIMEZONE));

        serviceUnderTest = new GeoTimezoneMappingService(geoTimezoneRepository);
    }

    @Test
    public void getRegionIdByTimezoneId_returnRusRegion_whenTimezoneIdIsNull() {
        GeoTimezone actual = serviceUnderTest.getRegionIdByTimezoneId(null);

        GeoTimezone expected = GeoTimezoneMappingService.DEFAULT_GEO_TIMEZONE_RUS;
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getRegionIdByTimezoneId_returnRusRegion_whenTimezoneIdIsZero() {
        GeoTimezone actual = serviceUnderTest.getRegionIdByTimezoneId(0L);

        GeoTimezone expected = GeoTimezoneMappingService.DEFAULT_GEO_TIMEZONE_RUS;
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getRegionIdByTimezoneId_returnRusRegion_whenTimezoneIdIsUnknown() {
        GeoTimezone actual = serviceUnderTest.getRegionIdByTimezoneId(UNKNOWN_TIMEZONE_ID);

        GeoTimezone expected = GeoTimezoneMappingService.DEFAULT_GEO_TIMEZONE_RUS;
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getRegionIdByTimezoneId_returnRusRegion_whenTimezoneIdInJavaIsUnknown() {
        when(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(contains(JAVA_UNKNOWN_TIMEZONE_ID)))
                .thenThrow(new ZoneRulesException("Unknown time-zone ID"));

        GeoTimezone actual = serviceUnderTest.getRegionIdByTimezoneId(JAVA_UNKNOWN_TIMEZONE_ID);

        GeoTimezone expected = GeoTimezoneMappingService.DEFAULT_GEO_TIMEZONE_RUS;
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getRegionIdByTimezoneId_returnCorrectRegion_whenTimezoneIdIsSpecified() {
        GeoTimezone actual = serviceUnderTest.getRegionIdByTimezoneId(BELARUS_TIMEZONE_ID);

        Assertions.assertThat(actual).isEqualTo(BELARUS_GEO_TIMEZONE);
    }

    @Test
    public void getRegionIdByTimezoneId_cacheWorks_whenTimezoneIdIsKnown() {
        serviceUnderTest.getRegionIdByTimezoneId(BELARUS_TIMEZONE_ID);
        verify(geoTimezoneRepository).getGeoTimezonesByTimezoneIds(contains(BELARUS_TIMEZONE_ID));
        serviceUnderTest.getRegionIdByTimezoneId(BELARUS_TIMEZONE_ID);
        verifyNoMoreInteractions(geoTimezoneRepository);
    }

    @Test
    public void getRegionIdByTimezoneId_cacheWorks_whenTimezoneIdIsUnknown() {
        serviceUnderTest.getRegionIdByTimezoneId(UNKNOWN_TIMEZONE_ID);
        verify(geoTimezoneRepository).getGeoTimezonesByTimezoneIds(contains(UNKNOWN_TIMEZONE_ID));
        serviceUnderTest.getRegionIdByTimezoneId(UNKNOWN_TIMEZONE_ID);
        verifyNoMoreInteractions(geoTimezoneRepository);
    }

    @Test
    public void getRegionIdByTimezoneId_catchRuntimeException() {

        NullPointerException someUnpredictableException = new NullPointerException("Some unpredictable exception");
        when(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(contains(JAVA_UNKNOWN_TIMEZONE_ID)))
                .thenThrow(someUnpredictableException);

        Assertions.assertThatThrownBy(() -> serviceUnderTest.getRegionIdByTimezoneId(JAVA_UNKNOWN_TIMEZONE_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Error during getting regionId by timezoneId")
                .hasCause(someUnpredictableException);
    }
}

