package ru.yandex.market.tpl.core.service.location.calculator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;
import ru.yandex.market.tpl.core.service.location.calculator.util.CoordinateDtoBuilderTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class UserLocationSpoofingServiceTest {

    public static final long SECONDS_STEP = 2 * 60;

    @InjectMocks
    private UserLocationSpoofingService spoofingService;

    @DisplayName("Проверка на спуфинг пустого трека точек")
    @Test
    public void removeSpoofingCoordinateFromEmptyList() {
        List<CoordinateDto> coordinateDtos = spoofingService.removeSpoofingCoordinate(Collections.emptyList());

        assertEquals(coordinateDtos.size(), 0);
    }

    @DisplayName("Проверка на спуфинг валидного трека")
    @Test
    public void removeSpoofingCoordinateFromValidListTest() {
        List<CoordinateDto> coordinateDtos = buildValidMoscowTrack();

        List<CoordinateDto> result = spoofingService.removeSpoofingCoordinate(coordinateDtos);

        assertNotNull(result);
        assertEquals(coordinateDtos.size(), result.size());
    }

    @DisplayName("Проверка на спуфинг невалидного трека")
    @Test
    public void removeSpoofingCoordinateFromInvalidListTest() {
        List<CoordinateDto> coordinateDtos = buildInvalidMoscowTrack();

        List<CoordinateDto> result = spoofingService.removeSpoofingCoordinate(coordinateDtos);

        assertNotNull(result);
        assertNotEquals(coordinateDtos.size(), result.size());
    }

    @DisplayName("Проверка на спуфинг невалидных точек")
    @Test
    public void removeSpoofingCoordinateTest() {
        int zeroCoordinate = 0;
        List<CoordinateDto> coordinateDtos = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(0, 0), Instant.now()
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(45, 45), Instant.now()
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(45, 45), Instant.now()
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(5, 5), Instant.now().plusSeconds(2 * 60)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(0.001, 0.001), Instant.now().plusSeconds(2 * 60)
                )
        );

        List<CoordinateDto> result = spoofingService.removeSpoofingCoordinate(coordinateDtos);

        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(result.iterator().next().getGeoPoint().getLongitude().intValue(), zeroCoordinate);
    }

    private List<CoordinateDto> buildValidMoscowTrack() {
        Instant start = Instant.now();
        int i = 0;
        List<CoordinateDto> coordinateDtos = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.75239157553176, 37.58628539404965),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.75236091968523, 37.60022009344747),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.75590360167856, 37.600092937530945),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.75863461339128, 37.59988106528994),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.76455638500314, 37.60580691616344),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.76797948076131, 37.61372612007092),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.77294294607459, 37.60894520036595),
                        start.plusSeconds(SECONDS_STEP * i++)
                )
        );
        return coordinateDtos;
    }

    private List<CoordinateDto> buildInvalidMoscowTrack() {
        Instant start = Instant.now();
        int i = 0;
        List<CoordinateDto> coordinateDtos = List.of(
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.7869890973227, 37.59363750663719),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.79219174778563, 37.595388638372384),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        //Координата добавляющая шум в трек
                        GeoPoint.ofLatLon(55.83, 37.64),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.792420145496834, 37.60780217079436),
                        start.plusSeconds(SECONDS_STEP * i++)
                ),
                CoordinateDtoBuilderTestUtil.buildCoordinateDto(
                        GeoPoint.ofLatLon(55.78008144750799, 37.63498151867729),
                        start.plusSeconds(SECONDS_STEP * i++)
                )
        );
        return coordinateDtos;
    }
}
