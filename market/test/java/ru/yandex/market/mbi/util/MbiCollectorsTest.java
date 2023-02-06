package ru.yandex.market.mbi.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class MbiCollectorsTest {
    @Test
    void testFiltering() {
        List<Integer> result =
                IntStream.range(0, 100).boxed().collect(MbiCollectors.filtering(i -> i % 11 == 0, Collectors.toList()));
        assertThat(result).containsExactly(0, 11, 22, 33, 44, 55, 66, 77, 88, 99);
    }

    @Test
    void testGroupByWithNullKeys() {
        Map<String, List<TestClass>> result = Stream.of(
                new TestClass(null, 1),
                new TestClass("abc", 2),
                new TestClass(null, 3)
        ).collect(MbiCollectors.groupingByWithNullKeys(TestClass::getId));
        assertThat(result).hasSize(2);
        assertThat(result.get("abc")).hasSize(1);
        assertThat(result.get(null)).hasSize(2);
    }

    private static class TestClass {
        private final String id;
        private final int value;

        TestClass(@Nullable String id, int value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public int getValue() {
            return value;
        }
    }
}
