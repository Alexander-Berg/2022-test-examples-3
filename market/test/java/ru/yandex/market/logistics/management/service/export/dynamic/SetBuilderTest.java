package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;

class SetBuilderTest extends AbstractTest {

    @Test
    public void testNull() {
        SetBuilder<Integer> setBuilder = new SetBuilder<>();

        softly.assertThat(setBuilder.buildSetKey(null)).isEqualTo(0);
        softly.assertThat(setBuilder.keys().collect(Collectors.toList())).isEqualTo(Collections.singletonList(null));
        softly
            .assertThat(setBuilder.entries().collect(Collectors.toList()))
            .isEqualTo(Collections.singletonList(new AbstractMap.SimpleEntry<>(null, 0)));
    }

    @Test
    public void testCommon() {
        SetBuilder<Integer> setBuilder = new SetBuilder<>();

        softly.assertThat(setBuilder.buildSetKey(1)).isEqualTo(0);
        softly.assertThat(setBuilder.buildSetKey(1)).isEqualTo(0);
        softly.assertThat(setBuilder.buildSetKey(2)).isEqualTo(1);
        softly.assertThat(setBuilder.buildSetKey(1)).isEqualTo(0);
        softly.assertThat(setBuilder.buildSetKey(2)).isEqualTo(1);

        softly
            .assertThat(setBuilder.keys().sorted(Integer::compareTo).collect(Collectors.toList()))
            .isEqualTo(List.of(1, 2));

        softly
            .assertThat(setBuilder.entries().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList()))
            .isEqualTo(List.of(
                new AbstractMap.SimpleEntry<>(1, 0),
                new AbstractMap.SimpleEntry<>(2, 1)
            ));
    }

    @Test
    public void testInitialId() {
        SetBuilder<Integer> setBuilder = new SetBuilder<>(10);

        softly.assertThat(setBuilder.buildSetKey(1)).isEqualTo(10);
        softly.assertThat(setBuilder.buildSetKey(2)).isEqualTo(11);
    }

    @Test
    public void testCollision() {
        SetBuilder<CollisionObject> sb = new SetBuilder<>();

        softly.assertThat(sb.buildSetKey(new CollisionObject())).isEqualTo(0);
        softly.assertThat(sb.buildSetKey(new CollisionObject())).isEqualTo(1);
        softly.assertThat(sb.buildSetKey(new CollisionObject())).isEqualTo(2);
    }

    @Test
    public void testSet() {
        SetBuilder<Set<Integer>> sb = new SetBuilder<>();

        softly.assertThat(sb.buildSetKey(Set.of(1))).isEqualTo(0);
        softly.assertThat(sb.buildSetKey(Set.of(1))).isEqualTo(0);
        softly.assertThat(sb.buildSetKey(Set.of(1, 2))).isEqualTo(1);
    }

    static class CollisionObject {
        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }
    }
}
