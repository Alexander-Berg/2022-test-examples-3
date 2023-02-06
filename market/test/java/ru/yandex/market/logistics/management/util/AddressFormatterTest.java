package ru.yandex.market.logistics.management.util;

import java.math.BigDecimal;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Address;
import ru.yandex.market.logistics.management.model.enums.AddressPrecision;

@DisplayName("Тесто форматтера адресов AddressFormatterTest")
class AddressFormatterTest extends AbstractTest {

    private static final Address ADDRESS = new Address()
        .setAddressString("село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2")
        .setShortAddressString("село Зудово, Солнечная улица, 9A, 2")
        .setLocationId(133543)
        .setLatitude(BigDecimal.valueOf(55.822463D))
        .setLongitude(BigDecimal.valueOf(84.258002D))
        .setPostCode("633372")
        .setRegion("Новосибирская область")
        .setSubRegion("Болотнинский район")
        .setSettlement("Зудово")
        .setStreet("Солнечная")
        .setHouse("6")
        .setHousing("2")
        .setBuilding("А")
        .setApartment("318");

    public static final Location LOCATION = new Location(
        "Россия",
        "Зудово",
        "Новосибирская область",
        "Сибирский федеральный округ",
        "Болотнинский район",
        "Зудовский сельсовет",
        "Солнечная",
        "6",
        "А",
        "2",
        "318",
        "633372",
        null,
        3,
        null,
        BigDecimal.valueOf(55.822463D),
        BigDecimal.valueOf(84.258002D),
        133543,
        null
    );

    @Nonnull
    private static Stream<Arguments> addresses() {
        return Stream.of(
            Arguments.of(
                AddressPrecision.EXACT,
                ADDRESS,
                "Новосибирская область, Болотнинский район, Зудово, Солнечная, дом 6, строение А, корпус 2"
            ),
            Arguments.of(
                AddressPrecision.LOCALITY,
                ADDRESS,
                "Новосибирская область, Болотнинский район, Зудово"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("addresses")
    @DisplayName("Формируем строковый запрос для геопоиска по объекту Address")
    void toGeosearchQueryByAddress(AddressPrecision precision, Address address, String expectedQuery) {
        softly.assertThat(AddressFormatter.toGeosearchQuery(precision, address)).isEqualTo(expectedQuery);
    }

    @Nonnull
    private static Stream<Arguments> locations() {
        return Stream.of(
            Arguments.of(
                AddressPrecision.EXACT,
                LOCATION,
                "Россия, Сибирский федеральный округ, Новосибирская область, Болотнинский район, Зудовский сельсовет," +
                    " Зудово, Солнечная, дом 6, строение А, корпус 2"
            ),
            Arguments.of(
                AddressPrecision.LOCALITY,
                LOCATION,
                "Россия, Сибирский федеральный округ, Новосибирская область, Болотнинский район, Зудовский сельсовет," +
                    " Зудово"
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("locations")
    @DisplayName("Формируем строковый запрос для геопоиска по объекту Location")
    void toGeosearchQueryByLocation(AddressPrecision precision, Location location, String expectedQuery) {
        softly.assertThat(AddressFormatter.toGeosearchQuery(precision, location))
            .isEqualTo(expectedQuery);
    }

    @Test
    @DisplayName("Переводим адрес в подробное строковое представление")
    void toAddressString() {
        softly.assertThat(AddressFormatter.toAddressString(ADDRESS))
            .isEqualTo(
                "633372, Новосибирская область, Болотнинский район, Зудово, Солнечная, д. 6, стр. А, корп. 2, кв. 318"
            );
    }

    @Test
    @DisplayName("Переводим адрес в краткое строковое представление")
    void toShortAddressString() {
        softly.assertThat(AddressFormatter.toShortAddressString(ADDRESS))
            .isEqualTo("Солнечная, д. 6, стр. А, корп. 2, кв. 318");
    }

    @Test
    @DisplayName("Переводим адрес в подробное строковое представление с уникальными частями")
    void toAddressStringWithUniqueChunks() {
        Address duplicatingAddress = ADDRESS.copy();
        duplicatingAddress.setSettlement("Новосибирская область");
        duplicatingAddress.setHousing("6");
        duplicatingAddress.setApartment("");
        duplicatingAddress.setBuilding(null);
        softly.assertThat(AddressFormatter.toAddressString(duplicatingAddress))
            .isEqualTo(
                "633372, Новосибирская область, Болотнинский район, Солнечная, д. 6, корп. 6"
            );
    }
}
