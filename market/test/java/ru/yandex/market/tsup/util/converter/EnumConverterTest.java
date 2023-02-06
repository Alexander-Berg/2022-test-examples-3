package ru.yandex.market.tsup.util.converter;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleSubtypeDto;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleType;

class EnumConverterTest {

    @ParameterizedTest
    @MethodSource("getTestCases")
    <F extends Enum<F>, T extends Enum<T>> void convertEnum(Class<F> from, Class<T> to, Set<F> excluded) {
        Stream.of(from.getEnumConstants())
            .filter(value -> !excluded.contains(value))
            .forEach(value -> EnumConverter.toEnum(value, to));
    }

    private static Stream<Arguments> getTestCases() {
        return Stream.of(
            Arguments.of(
                RouteScheduleType.class,
                ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleTypeDto.class,
                Collections.emptySet()
            ),
            Arguments.of(
                RouteScheduleType.class,
                ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto.class,
                Collections.emptySet()
            ),
            Arguments.of(
                RouteScheduleType.class,
                ru.yandex.market.tsup.service.data_provider.entity.route.RouteScheduleType.class,
                Collections.emptySet()
            ),
            Arguments.of(
                RouteScheduleSubtype.class,
                RouteScheduleSubtypeDto.class,
                Collections.emptySet()
            )
        );
    }

}
