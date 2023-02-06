package ru.yandex.direct.jobs.placements;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.regions.GeoTreeFactory;

import static java.time.LocalDateTime.now;
import static java.util.Collections.singleton;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;

class GeoIdDetectorCoordinatesFormatTest {

    private GeoIdDetector geoIdDetector;

    static List<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"12.345,67.890"},
                new Object[]{"12,67"},
                new Object[]{"12.345 , 67.890"},
                new Object[]{".345,.890"}
        );
    }

    @BeforeEach
    void before() {
        GeosearchClient geosearchClient = mock(GeosearchClient.class);
        GeoTreeFactory geoTreeFactory = mock(GeoTreeFactory.class);
        geoIdDetector = new GeoIdDetector(geosearchClient, geoTreeFactory);
    }

    @ParameterizedTest(name = "{index}. координаты: {0}")
    @MethodSource("parameters")
    void noExceptionsThrown(String coordinates) {
        geoIdDetector.detectGeoIds(
                singleton(outdoorBlockWithOneSize(1L, 2L, now(), coordinates)));
    }
}
