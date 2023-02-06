package ru.yandex.market.deliverycalculator.workflow.mardobasemodel.tariff.fast;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractPickupFastMardoMetaTariffTest {
    @Test
    void freezeAndCompressIndex() {
        // given
        Set<Integer> possiblyMemoryHeavySet = Sets.newHashSet(1, 2, 3);
        var index = Map.of("a", Map.of("b", new TreeMap<>(Map.of("c", possiblyMemoryHeavySet))));

        // when
        AbstractPickupFastMardoMetaTariff.freezeAndCompressIndex(index);
        var compressedSet = index.get("a").get("b").get("c");

        // then
        assertThat(compressedSet)
                .as("HashSet is too memory heavy")
                .isNotInstanceOf(HashSet.class)
                .isNotSameAs(possiblyMemoryHeavySet)
                .containsOnlyElementsOf(possiblyMemoryHeavySet);
    }
}
