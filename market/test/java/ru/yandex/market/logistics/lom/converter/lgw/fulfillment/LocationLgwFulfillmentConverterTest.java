package ru.yandex.market.logistics.lom.converter.lgw.fulfillment;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.Location;
import ru.yandex.market.logistics.lom.entity.embedded.Address;

class LocationLgwFulfillmentConverterTest {
    private final LocationLgwFulfillmentConverter converter = new LocationLgwFulfillmentConverter();

    @DisplayName("Проверка корректной конвертации для адреса под заказ")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getOrderData")
    void convertForOrder(String caseName, Address from, Location expected) {
        Assertions.assertEquals(expected, converter.toExternalLocation(from));
    }

    private static Stream<Arguments> getOrderData() {
        return Stream.of(
            Arguments.of(
                "Регион не подменяется",
                createWaybillLocation(100500),
                createLocation(100500, "region")
            ),
            Arguments.of(
                "Москва",
                createWaybillLocation(213),
                createLocation(213, "region")
            ),
            Arguments.of(
                "Подольск",
                createWaybillLocation(10747),
                createLocation(10747, "region")
            ),
            Arguments.of(
                "Софьино",
                createWaybillLocation(120013),
                createLocation(120013, "region")
            ),
            Arguments.of(
                "Томилино",
                createWaybillLocation(101060),
                createLocation(101060, "region")
            ),
            Arguments.of(
                "Котельники",
                createWaybillLocation(21651),
                createLocation(21651, "region")
            )
        );
    }

    private static Location createLocation(int locationId, String region) {
        return new Location.LocationBuilder("country", "locality", region)
            .setFederalDistrict("federalDistrict")
            .setSubRegion("subRegion")
            .setSettlement("settlement")
            .setStreet("street")
            .setHouse("house")
            .setBuilding("building")
            .setHousing("housing")
            .setRoom("room")
            .setZipCode("zipCode")
            .setPorch("porch").setFloor(1)
            .setMetro("metro").setLat(new BigDecimal("10.1"))
            .setLng(new BigDecimal("100.2"))
            .setLocationId(locationId)
            .build();
    }

    private static Location createShipmentLocation(int locationId, String region) {
        return new Location.LocationBuilder("Россия", "locality", region)
            .setStreet("street")
            .setHouse("house")
            .setBuilding("building")
            .setHousing("housing")
            .setLat(new BigDecimal("10.1"))
            .setLng(new BigDecimal("100.2"))
            .setZipCode("zipCode")
            .setRoom("room")
            .setLocationId(locationId)
            .build();
    }

    private static Address createWaybillLocation(int locationId) {
        Address address = new Address();
        address.setLocality("locality");
        address.setRegion("region");
        address.setFederalDistrict("federalDistrict");
        address.setCountry("country");
        address.setBuilding("building");
        address.setFloor(1);
        address.setGeoId(locationId);
        address.setHouse("house");
        address.setHousing("housing");
        address.setIntercom("intercom");
        address.setLatitude(new BigDecimal("10.1"));
        address.setLongitude(new BigDecimal("100.2"));
        address.setMetro("metro");
        address.setPorch("porch");
        address.setRoom("room");
        address.setSubRegion("subRegion");
        address.setStreet("street");
        address.setSettlement("settlement");
        address.setZipCode("zipCode");
        return address;
    }

}
