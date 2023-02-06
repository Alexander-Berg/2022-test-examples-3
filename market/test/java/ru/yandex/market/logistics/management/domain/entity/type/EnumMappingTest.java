package ru.yandex.market.logistics.management.domain.entity.type;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.dto.front.logistic.segment.LogisticSegmentFrontType;
import ru.yandex.market.logistics.management.domain.dto.front.partner.PartnerTypeFrontEnum;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EnumMappingTest {

    @ParameterizedTest
    @MethodSource("getMappedEnums")
    void testEnumsMapping(Class<? extends Enum> enum1, Class<? extends Enum> enum2) {
        assertEquals(
            toStringSet(enum1),
            toStringSet(enum2)
        );
    }

    private static Set<String> toStringSet(Class<? extends Enum> enum1) {
        return Stream.of(enum1.getEnumConstants())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

    private static Stream<Arguments> getMappedEnums() {
        return Stream.of(
            Arguments.of(
                PossibleOrderChangeType.class,
                ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType.class
            ),
            Arguments.of(
                PossibleOrderChangeMethod.class,
                ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod.class
            ),
            Arguments.of(
                TrackCodeSource.class,
                ru.yandex.market.logistics.management.entity.type.TrackCodeSource.class
            ),
            Arguments.of(
                LogisticSegmentType.class,
                LogisticSegmentFrontType.class
            ),
            Arguments.of(
                PartnerType.class,
                PartnerTypeFrontEnum.class
            )
        );
    }
}
