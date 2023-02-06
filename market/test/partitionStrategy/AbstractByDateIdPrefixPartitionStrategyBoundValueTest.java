package ru.yandex.market.jmf.db.api.test.partitionStrategy;


import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.jmf.db.api.AbstractByDateIdPrefixPartitionStrategy;
import ru.yandex.market.jmf.metadata.Fqn;

public class AbstractByDateIdPrefixPartitionStrategyBoundValueTest {
    public static List<Arguments> data() {
        return List.of(
                Arguments.of(LocalDate.of(2020, 12, 3), "2012T"),
                Arguments.of(LocalDate.of(2021, 1, 3), "2101T"),
                Arguments.of(LocalDate.of(2021, 5, 7), "2105T"),
                Arguments.of(LocalDate.of(2021, 12, 7), "2112T"),
                Arguments.of(LocalDate.of(2022, 1, 7), "2201T"),
                Arguments.of(LocalDate.of(2023, 12, 1), "2312T")
        );
    }

    @ParameterizedTest(name = "{index} {0} => {1}")
    @MethodSource(value = "data")
    public void checkBound_december21(LocalDate value, String expected) {
        AbstractByDateIdPrefixPartitionStrategy strategy = new AbstractByDateIdPrefixPartitionStrategy(Fqn.of("test"),
                true) {
        };

        String result = strategy.boundValue(value);
        Assertions.assertEquals(expected, result);
    }
}
