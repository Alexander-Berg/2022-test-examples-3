package ru.yandex.market.ir.autogeneration.common.util;

import org.assertj.core.util.Streams;
import org.junit.Test;
import ru.yandex.common.util.collections.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MapUtilsTest {
    @Test
    public void testToMapWithValueCombiner() {
        Function<Pair<Integer, String>, Integer> keyFunction = Pair::getFirst;
        BiFunction<Integer, List<? extends Pair<Integer, String>>, Pair<Integer, String>> combiner = (key, values) -> {
            assertThat(values.size()).isGreaterThan(1);
            return Pair.of(
                    key,
                    values.stream()
                            .map(Pair::getSecond)
                            .collect(Collectors.joining("-"))
            );
        };

        // Just run on several inputs and compare the result with what an unoptimized reference implementation returns.
        // Our T type is a Pair of Integer (the key) and String (the payload)
        Random random = new Random(0);
        for (int i = 0; i < 10; i++) {
            List<Pair<Integer, String>> input = new ArrayList<>();
            for (int j = 0; j < i; j++) {
                input.add(Pair.of(random.nextInt(3), "" + j));
                Collections.shuffle(input, random);
            }

            Map<Integer, Pair<Integer, String>> actualResult = MapUtils.toMapWithValueCombiner(input, keyFunction, combiner);
            Map<Integer, Pair<Integer, String>> referenceResult = toMapWithValueCombinerReference(input, keyFunction, combiner);

            assertThat(actualResult).isEqualTo(referenceResult);
        }
    }

    private static <T, K> Map<K, T> toMapWithValueCombinerReference(
            Iterable<T> input,
            Function<? super T, K> keyFunction,
            BiFunction<? super K, ? super List<? extends T>, T> combiner
    ) {
        return Streams.stream(input)
                .collect(Collectors.groupingBy(keyFunction, Collectors.toList()))
                .entrySet().stream()
                .map(en -> Pair.of(en.getKey(), en.getValue().size() == 1 ? en.getValue().get(0) : combiner.apply(en.getKey(), en.getValue())))
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }
}
