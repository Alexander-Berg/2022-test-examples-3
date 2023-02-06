package ru.yandex.market.logistic.gateway.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.LocationFilter;
import ru.yandex.market.logistic.gateway.BaseTest;

class LocationFilterConverterTest extends BaseTest {

    @Test
    void convertLocationFiltersToApi() {
        assertions.assertThat(LocationFilterConverter.convertLocationFiltersToApi(List.of(createLocationFilterLgw())))
            .as("Asserting that the converted list is valid")
            .isEqualTo(List.of(createLocationFilterApi()));
    }

    @Test
    void convertLocationFiltersToApiNull() {
        assertions.assertThat(LocationFilterConverter.convertLocationFiltersToApi(null))
            .as("Asserting that the null list is converted to the empty list")
            .isEmpty();
    }

    @Test
    void testConvertLocationFilterToApi() {
        assertions.assertThat(LocationFilterConverter.convertLocationFilterToApi(createLocationFilterLgw()))
            .as("Asserting that the converted entity is valid")
            .isEqualTo(createLocationFilterApi());
    }

    @Test
    void testConvertLocationFilterToApiNull() {
        assertions.assertThat(LocationFilterConverter.convertLocationFilterToApi(null))
            .as("Asserting that the null entity is converted to the empty optional")
            .isEmpty();
    }

    private ru.yandex.market.logistic.gateway.common.model.common.LocationFilter createLocationFilterLgw() {
        return ru.yandex.market.logistic.gateway.common.model.common.LocationFilter.builder()
            .setCountry("country")
            .setRegion("region")
            .setLocality("locality")
            .setFederalDistrict("federalDistrict")
            .setSubRegion("subRegion")
            .setSettlement("settlement")
            .setStreet("street")
            .setHouse("house")
            .setBuilding("building")
            .setHousing("housing")
            .setRoom("room")
            .setZipCode("zipCode")
            .setPorch("porch")
            .setFloor(7)
            .setMetro("metro")
            .setLat(BigDecimal.valueOf(55.66))
            .setLng(BigDecimal.valueOf(33.44))
            .setLocationId(225L)
            .setIntercom("intercom")
            .build();
    }

    private LocationFilter createLocationFilterApi() {
        return LocationFilter.builder()
            .setCountry("country")
            .setRegion("region")
            .setLocality("locality")
            .setFederalDistrict("federalDistrict")
            .setSubRegion("subRegion")
            .setSettlement("settlement")
            .setStreet("street")
            .setHouse("house")
            .setBuilding("building")
            .setHousing("housing")
            .setRoom("room")
            .setZipCode("zipCode")
            .setPorch("porch")
            .setFloor(7)
            .setMetro("metro")
            .setLat(BigDecimal.valueOf(55.66))
            .setLng(BigDecimal.valueOf(33.44))
            .setLocationId(225L)
            .setIntercom("intercom")
            .build();
    }
}
