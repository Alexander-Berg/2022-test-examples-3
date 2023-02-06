package ru.yandex.direct.utils.collectors;

import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public final class FlipMapCollectorTest {
    @Test
    public void flipMap() {
        final Map<String, Set<Integer>> map = Map.of("A", Set.of(1, 2, 3), "B", Set.of(1, 2), "C", Set.of(3), "D", Set.of());
        final var result = map.entrySet().stream().collect(new FlipMapCollector<>());

        Assert.assertEquals(result, Map.of(1, Set.of("A", "B"), 2, Set.of("A", "B"), 3, Set.of("A", "C")));
    }
}
