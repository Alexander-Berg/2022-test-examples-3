package ru.yandex.market.logistics.cs.dbqueue.servicecounter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cs.AbstractTest;

class ItemCounterTest extends AbstractTest {

    ItemCounter itemCounter = new ItemCounter();

    @ParameterizedTest
    @MethodSource("testRealItemCountData")
    void testRealItemCount(int delta, int expected) {
        itemCounter.add(delta, 1.0);
        softly.assertThat(itemCounter.getRealItemCount()).isEqualTo(expected);
    }

    @Nonnull
    private static Stream<Arguments> testRealItemCountData() {
        return Stream.of(
            Arguments.of(0, 0),
            Arguments.of(1, 1),
            Arguments.of(2, 2),
            Arguments.of(3, 3),
            Arguments.of(5, 5),
            Arguments.of(8, 8),
            Arguments.of(13, 13),
            Arguments.of(999, 999),
            Arguments.of(100500, 100500)
        );
    }

    @ParameterizedTest
    @MethodSource("testItemCountWithFactorData")
    void testItemCountWithFactor(int delta, double cargoTypeFactor, int expected) {
        itemCounter.add(delta, cargoTypeFactor);
        softly.assertThat(itemCounter.getItemCountWithFactor()).isEqualTo(expected);
    }

    @Nonnull
    private static Stream<Arguments> testItemCountWithFactorData() {
        return Stream.of(
            Arguments.of(0, 0.0, 0),
            Arguments.of(0, Double.MIN_VALUE, 0),
            Arguments.of(0, 0.0001, 0),
            Arguments.of(1, 0.0, 0),
            Arguments.of(1, Double.MIN_VALUE, 1),
            Arguments.of(1, 0.0001, 1),
            Arguments.of(1, 1.0, 1),
            Arguments.of(2, 1.0, 2),
            Arguments.of(3, 1.0, 3),
            Arguments.of(5, 1.0, 5),
            Arguments.of(8, 1.0, 8),
            Arguments.of(13, 1.0, 13),
            Arguments.of(999, 1.0, 999),
            Arguments.of(100500, 1.0, 100500),
            Arguments.of(1, 1.0, 1),
            Arguments.of(1, 1.0000000000000001, 1),
            Arguments.of(1, 1.000000000000001, 2),
            Arguments.of(1, 1.5, 2),
            Arguments.of(1, 1.9999, 2),
            Arguments.of(9, 1.1, 10),
            Arguments.of(10, 1.1, 11),
            Arguments.of(11, 1.1, 13),
            Arguments.of(12, 2.6, 32)
        );
    }
}
