package ru.yandex.direct.utils.collectors;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public final class MergeMapCollectorTest {
    @Test
    public void mergeMaps() {
        final var maps = List.of(
                Map.of(1, Set.of(1)),
                Map.of(2, Set.of(1)),
                Map.of(1, Set.of(2), 2, Set.of(2), 3, Set.of(1, 2))
        );

        final var result = maps.stream().collect(new MergeMapCollector<>());

        Assert.assertEquals(result, Map.of(1, Set.of(1, 2), 2, Set.of(1, 2), 3, Set.of(1, 2)));
    }
}
