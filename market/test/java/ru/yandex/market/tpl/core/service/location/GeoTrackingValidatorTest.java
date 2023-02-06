package ru.yandex.market.tpl.core.service.location;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.location.tracking.GeoTrackingDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GeoTrackingValidatorTest {

    @InjectMocks
    private GeoTrackingValidator geoTrackingValidator;

    @DisplayName("Валидация заполненных гео-точек")
    @Test
    public void validateAndGetValidTrackingDtoValidListTest() {
        GeoTrackingDto trackingDto = new GeoTrackingDto();
        trackingDto.setLatitude(BigDecimal.ONE);
        trackingDto.setLongitude(BigDecimal.ONE);
        trackingDto.setUserTime(Instant.now());
        List<GeoTrackingDto> geoTrackingList = List.of(trackingDto);

        List<GeoTrackingDto> validGeoTrackingList =
                geoTrackingValidator.validateAndGetValidTrackingDto(geoTrackingList);

        assertEquals(geoTrackingList, validGeoTrackingList);
    }

    @DisplayName("Валидация не полностью заполненных гео-точек")
    @Test
    public void validateAndGetValidTrackingDtoNotValidListTest() {
        GeoTrackingDto trackingDto = new GeoTrackingDto();
        trackingDto.setLatitude(BigDecimal.ONE);
        trackingDto.setLongitude(null);
        trackingDto.setUserTime(Instant.now());
        List<GeoTrackingDto> geoTrackingList = List.of(trackingDto);

        List<GeoTrackingDto> validGeoTrackingList =
                geoTrackingValidator.validateAndGetValidTrackingDto(geoTrackingList);

        assertTrue(validGeoTrackingList.isEmpty());
        assertNotEquals(geoTrackingList, validGeoTrackingList);
    }

    @DisplayName("Валидация гео-точек c незаполненным клиентским временем")
    @Test
    public void validateAndGetValidTrackingDtoNotValidUserTimeTest() {
        GeoTrackingDto trackingDto = new GeoTrackingDto();
        trackingDto.setLatitude(BigDecimal.ONE);
        trackingDto.setLongitude(BigDecimal.ONE);
        trackingDto.setUserTime(null);
        List<GeoTrackingDto> geoTrackingList = List.of(trackingDto);

        List<GeoTrackingDto> validGeoTrackingList =
                geoTrackingValidator.validateAndGetValidTrackingDto(geoTrackingList);

        assertTrue(validGeoTrackingList.isEmpty());
        assertNotEquals(geoTrackingList, validGeoTrackingList);
    }

    @DisplayName("Валидация пустого листа гео-точек")
    @Test
    public void validateAndGetValidTrackingDtoEmptyListTest() {
        List<GeoTrackingDto> geoTrackingList =
                geoTrackingValidator.validateAndGetValidTrackingDto(Collections.emptyList());

        assertTrue(geoTrackingList.isEmpty());
    }

}
