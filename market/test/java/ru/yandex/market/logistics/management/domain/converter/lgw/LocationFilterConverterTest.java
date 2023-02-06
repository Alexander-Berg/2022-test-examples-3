package ru.yandex.market.logistics.management.domain.converter.lgw;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.gateway.common.model.common.LocationFilter;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationFilterConverterTest extends AbstractTest {

    private static final Integer COUNTRY_ID = 110011;
    private static final String COUNTRY_NAME = "Германия";

    private final LocationFilterConverter locationFilterConverter = new LocationFilterConverter();

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testConvertFromCountry(RegionEntity country, LocationFilter locationFilter) {
        assertThat(locationFilterConverter.fromCountry(country)).isEqualTo(locationFilter);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testConvertFromCountryIdIsNull(String message, RegionEntity country) {
        assertThatThrownBy(() -> locationFilterConverter.fromCountry(country))
            .isInstanceOf(NullPointerException.class)
            .hasMessage(message);
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> testConvertFromCountry() {
        return Stream.of(
            Arguments.of(
                new RegionEntity()
                    .setId(COUNTRY_ID)
                    .setName(COUNTRY_NAME),
                new LocationFilter.LocationFilterBuilder()
                    .setLocationId(COUNTRY_ID.longValue())
                    .setCountry(COUNTRY_NAME)
                    .build()
            ),
            Arguments.of(null, null)
        );
    }

    @Nonnull
    @SuppressWarnings("unused")
    private static Stream<Arguments> testConvertFromCountryIdIsNull() {
        return Stream.of(
            Arguments.of(
                "country id can't be null",
                new RegionEntity()
                    .setId(null)
                    .setName(COUNTRY_NAME)
            ),
            Arguments.of(
                "country name can't be null",
                new RegionEntity()
                    .setId(COUNTRY_ID)
                    .setName(null)
            )
        );
    }

}
