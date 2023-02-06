package ru.yandex.market.tpl.core.domain.usershift.location.precise;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.BDDMockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.geocoder.GeoClient;
import ru.yandex.common.util.geocoder.GeoObject;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.LogisticRequestPointService;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
class PreciseGeoPointServiceTest {

    private static final AddressString DELIVERY_ADDRESS = AddressString.builder()
            .city("Город")
            .street("ул. Пушкина")
            .house("2")
            .build();
    private static final String STRING_ADDRESS = "г. Город, ул. Пушкина, 2";
    private static final String STRING_ADDRESS_WITHOUT_CITY_PREFIX = "Город, ул. Пушкина, 2";
    private static final String STRING_ADDRESS_WITH_ENTRANCE = STRING_ADDRESS + ", подъезд 1";
    @MockBean
    private GeoClient geoSearchApiClient;
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private GeoObject geoObject;
    @MockBean
    private LogisticRequestPointService logisticRequestPointService;

    @BeforeEach
    void setUp() {
        reset(geoSearchApiClient);
    }

    @Test
    void getByAddress() {
        //given
        PreciseGeoPointService preciseGeoPointService = getPreciseGeoPointService();
        given(geoSearchApiClient.find(eq(STRING_ADDRESS), any()))
                .willReturn(List.of(geoObject));
        given(geoObject.getPoint())
                .willReturn("37.590497 55.736306");

        //when
        Optional<GeoPoint> geoPoint = preciseGeoPointService.getByAddress(DELIVERY_ADDRESS, null);

        //then
        then(geoPoint).hasValue(GeoPoint.ofLatLon(
                new BigDecimal("55.736306"),
                new BigDecimal("37.590497")
        ));
    }

    @Test
    void getByAddress_withRegionIdentify() {
        //given
        PreciseGeoPointService preciseGeoPointService = getPreciseGeoPointService();
        given(geoSearchApiClient.find(eq(STRING_ADDRESS_WITHOUT_CITY_PREFIX), any()))
                .willReturn(List.of(geoObject));
        given(geoObject.getPoint())
                .willReturn("38.590497 75.736306");

        //when
        Optional<GeoPoint> geoPoint = preciseGeoPointService.getByAddress(DELIVERY_ADDRESS, 123L);

        //then
        then(geoPoint).hasValue(GeoPoint.ofLatLon(
                new BigDecimal("75.736306"),
                new BigDecimal("38.590497")
        ));
    }

    @Test
    void getByAddressWhenEntranceIsEmpty() {
        //given
        PreciseGeoPointService preciseGeoPointService = getPreciseGeoPointService();
        given(geoSearchApiClient.find(eq(STRING_ADDRESS), any()))
                .willReturn(List.of());
        given(geoSearchApiClient.find(eq(STRING_ADDRESS_WITH_ENTRANCE), any()))
                .willReturn(List.of(geoObject));
        String expectedLon = "37.590498";
        String expectedLat = "55.736307";
        given(geoObject.getPoint())
                .willReturn(expectedLon + " " + expectedLat);

        //when
        Optional<GeoPoint> geoPoint = preciseGeoPointService.getByAddress(DELIVERY_ADDRESS, null);

        //then
        then(geoPoint).hasValue(GeoPoint.ofLatLon(
                new BigDecimal(expectedLat),
                new BigDecimal(expectedLon)
        ));
        BDDMockito.then(geoSearchApiClient)
                .should(times(1))
                .find(eq(STRING_ADDRESS_WITH_ENTRANCE), any());
        BDDMockito.then(geoSearchApiClient)
                .shouldHaveNoMoreInteractions();
    }

    private PreciseGeoPointService getPreciseGeoPointService() {
        return new PreciseGeoPointService(geoSearchApiClient, logisticRequestPointService);
    }

}
