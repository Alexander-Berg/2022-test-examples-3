package ru.yandex.market.logistics.logistics4go.utils;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.logistics4go.AbstractTest;

class DeliveryIntervalUtilsTest extends AbstractTest {
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void isFirstIntervalIncludesSecond(
        String caseName,
        Pair<Integer, Integer> first,
        Pair<Integer, Integer> second,
        boolean resultCheck
    ) {
        softly.assertThat(DeliveryIntervalUtils.isFirstIntervalIncludesSecond(first, second)).isEqualTo(resultCheck);
    }

    @Nonnull
    private static Stream<Arguments> isFirstIntervalIncludesSecond() {
        return Stream.of(
                new Object[]{0, 5, 2, 4, true},
                new Object[]{0, 4, 2, 4, true},
                new Object[]{2, 5, 2, 4, true},
                new Object[]{2, 4, 2, 4, true},
                new Object[]{2, 3, 2, 4, false},
                new Object[]{3, 4, 2, 4, false},
                new Object[]{3, 3, 2, 4, false},
                new Object[]{null, 5, 2, 4, false},
                new Object[]{0, null, 2, 4, false},
                new Object[]{0, 5, null, 4, false},
                new Object[]{0, 5, 2, null, false}
            )
            .map(a -> Arguments.of(
                "(%s <= %s) && (%s <= %s) == %s".formatted(a[0], a[2], a[3], a[1], a[4]),
                Pair.of(a[0], a[1]),
                Pair.of(a[2], a[3]),
                a[4]
            ));
    }
}
