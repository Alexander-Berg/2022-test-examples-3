package ru.yandex.market.logistic.gateway.common;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.api.model.fulfillment.UnitCargoType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitType;
import ru.yandex.market.logistic.api.model.fulfillment.WarehouseIdType;
import ru.yandex.market.logistic.gateway.common.model.common.EnumConverter;

class EnumConverterTest {

    @ParameterizedTest
    @MethodSource("enums")
    <F extends Enum<F>, T extends Enum<T>> void toEnum(Class<F> from, Class<T> to) {
        Stream.of(from.getEnumConstants()).forEach(value -> EnumConverter.toEnum(value, to));
    }

    static Stream<Arguments> enums() {
        return Stream.of(
            Arguments.of(
                UnitCargoType.class,
                ru.yandex.market.delivery.gruzin.model.UnitCargoType.class
            ),
            Arguments.of(
                UnitType.class,
                ru.yandex.market.delivery.gruzin.model.UnitType.class
            ),
            Arguments.of(
                WarehouseIdType.class,
                ru.yandex.market.delivery.gruzin.model.WarehouseIdType.class
            )
        );
    }
}
