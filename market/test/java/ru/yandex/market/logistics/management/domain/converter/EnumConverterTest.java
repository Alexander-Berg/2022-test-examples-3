package ru.yandex.market.logistics.management.domain.converter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Sets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.logistic.service.ServiceCodeNameFrontEnum;

class EnumConverterTest extends AbstractTest {
    @ParameterizedTest
    @MethodSource
    void allEnumConstantsCovered(Class<? extends Enum<?>> enumClass1, Class<? extends Enum<?>> enumClass2) {
        Sets.SetView<String> symmetricDifference = Sets.symmetricDifference(
            names(enumClass1),
            names(enumClass2)
        );

        softly.assertThat(symmetricDifference).isEmpty();
    }

    @Nonnull
    private static Stream<Arguments> allEnumConstantsCovered() {
        return Stream.of(
            Arguments.of(
                ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName.class,
                ru.yandex.market.logistics.management.entity.type.ServiceCodeName.class
            ),
            Arguments.of(
                ru.yandex.market.logistics.management.entity.type.ServiceCodeName.class,
                ServiceCodeNameFrontEnum.class
            ),
            Arguments.of(
                ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName.class,
                ServiceCodeNameFrontEnum.class
            )
        );
    }

    @Nonnull
    private Set<String> names(Class<? extends Enum<?>> enumClass) {
        if (!enumClass.isEnum()) {
            return Set.of();
        }
        return Arrays.stream(enumClass.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }
}
