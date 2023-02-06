package ru.yandex.market.logistics.lom.converter.lgw;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistics.lom.converter.AddressNormalizer;
import ru.yandex.market.logistics.lom.entity.embedded.Address;

import static ru.yandex.market.logistics.lom.LmsModelFactory.createLmsAddress;

class LocationLgwConverterTest {

    private static final long NON_CDEK_PARTNER_ID = 1;

    private static final long CDEK_PARTNER_ID = 51;

    private LocationLgwConverter converter = new LocationLgwConverter(new AddressNormalizer());

    @DisplayName("Проверка корректной конвертации для адреса под заказ")
     // Address.toString contains variable identity hash code, leading to variable test name and flaky test
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("getOrderData")
    void convertForOrder(Address from, long partnerId, Location expected) {
        Assertions.assertEquals(expected, converter.toExternalLocation(from, partnerId));
    }

    @DisplayName("Проверка корректной конвертации для адреса под отгрузку")
    @ParameterizedTest
    @MethodSource("getShipmentData")
    void convertForShipment(
        ru.yandex.market.logistics.management.entity.response.core.Address from,
        long partnerId,
        Location expected
    ) {
        Assertions.assertEquals(expected, converter.toExternalLocation(from, partnerId));
    }

    @Nonnull
    private static Stream<Arguments> getOrderData() {
        return Stream.of(
            Arguments.of(createAddress(100500), NON_CDEK_PARTNER_ID, createLocation(100500, "region")),
            Arguments.of(createAddress(100500), CDEK_PARTNER_ID, createLocation(100500, "region")),
            Arguments.of(createAddress(213), NON_CDEK_PARTNER_ID, createLocation(213, "Москва и Московская область")),
            Arguments.of(createAddress(213), CDEK_PARTNER_ID, createLocation(213, "Москва и Московская область")),
            Arguments.of(
                createAddress(10747),
                NON_CDEK_PARTNER_ID,
                createLocation(10747, "Москва и Московская область")
            ),
            Arguments.of(createAddress(10747), CDEK_PARTNER_ID, createLocation(10747, "Москва и Московская область")),
            Arguments.of(createAddress(120013), NON_CDEK_PARTNER_ID, createLocation(120013, "region")),
            Arguments.of(createAddress(120013), CDEK_PARTNER_ID, createLocation(120013, "Москва и Московская область")),
            Arguments.of(createAddress(101060), NON_CDEK_PARTNER_ID, createLocation(101060, "region")),
            Arguments.of(createAddress(101060), CDEK_PARTNER_ID, createLocation(101060, "Москва и Московская область")),
            Arguments.of(createAddress(21651), NON_CDEK_PARTNER_ID, createLocation(21651, "region")),
            Arguments.of(createAddress(21651), CDEK_PARTNER_ID, createLocation(21651, "Москва и Московская область")),
            Arguments.of(
                createAddress(21651, "Kazakhstan"),
                CDEK_PARTNER_ID,
                createLocation(21651, "Москва и Московская область", "Kazakhstan")
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getShipmentData() {
        return Stream.of(
            Arguments.of(createLmsAddress(100500), NON_CDEK_PARTNER_ID, createShipmentLocation(100500, "region")),
            Arguments.of(createLmsAddress(100500), CDEK_PARTNER_ID, createShipmentLocation(100500, "region")),
            Arguments.of(
                createLmsAddress(213),
                NON_CDEK_PARTNER_ID,
                createShipmentLocation(213, "Москва и Московская область")
            ),
            Arguments.of(
                createLmsAddress(213),
                CDEK_PARTNER_ID,
                createShipmentLocation(213, "Москва и Московская область")
            ),
            Arguments.of(
                createLmsAddress(10747),
                NON_CDEK_PARTNER_ID,
                createShipmentLocation(10747, "Москва и Московская область")
            ),
            Arguments.of(
                createLmsAddress(10747),
                CDEK_PARTNER_ID,
                createShipmentLocation(10747, "Москва и Московская область")
            ),
            Arguments.of(createLmsAddress(120013), NON_CDEK_PARTNER_ID, createShipmentLocation(120013, "region")),
            Arguments.of(
                createLmsAddress(120013),
                CDEK_PARTNER_ID,
                createShipmentLocation(120013, "Москва и Московская область")
            ),
            Arguments.of(createLmsAddress(101060), NON_CDEK_PARTNER_ID, createShipmentLocation(101060, "region")),
            Arguments.of(
                createLmsAddress(101060),
                CDEK_PARTNER_ID,
                createShipmentLocation(101060, "Москва и Московская область")
            ),
            Arguments.of(createLmsAddress(21651), NON_CDEK_PARTNER_ID, createShipmentLocation(21651, "region")),
            Arguments.of(
                createLmsAddress(21651),
                CDEK_PARTNER_ID,
                createShipmentLocation(21651, "Москва и Московская область")
            )
        );
    }

    @Nonnull
    private static Location createLocation(int locationId, String region) {
        return createLocation(locationId, region, "country");
    }

    @Nonnull
    private static Location createLocation(int locationId, String region, String country) {
        return new Location.LocationBuilder(country, "locality", region)
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
            .setMetro("metro")
            .setLat(new BigDecimal("10.1"))
            .setLng(new BigDecimal("100.2"))
            .setLocationId(locationId)
            .setIntercom("intercom")
            .build();
    }

    @Nonnull
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

    @Nonnull
    private static Address createAddress(int locationId) {
        return createAddress(locationId, "country");
    }

    @Nonnull
    private static Address createAddress(int locationId, String country) {
        Address address = new Address();
        address.setLocality("locality");
        address.setRegion("region");
        address.setFederalDistrict("federalDistrict");
        address.setCountry(country);
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
