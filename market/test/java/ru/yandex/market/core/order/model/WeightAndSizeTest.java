package ru.yandex.market.core.order.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тесты для {@link WeightAndSize}.
 *
 * @author ivmelnik
 * @since 23.08.18
 */
class WeightAndSizeTest {

    private static final Long WEIGHT = 1000L;
    private static final Long HEIGHT = 10L;
    private static final Long WIDTH = 20L;
    private static final Long DEPTH = 30L;

    private static final Long DEFAULT = 40L;

    @Test
    void checkEmpty() {
        WeightAndSize weightAndSize = WeightAndSize.EMPTY;
        assertNull(weightAndSize.getWeight());
        assertNull(weightAndSize.getHeight());
        assertNull(weightAndSize.getWidth());
        assertNull(weightAndSize.getDepth());
    }

    @Test
    void buildNull() {
        WeightAndSize weightAndSize = WeightAndSize.builder()
                .build();
        assertNull(weightAndSize.getWeight());
        assertNull(weightAndSize.getHeight());
        assertNull(weightAndSize.getWidth());
        assertNull(weightAndSize.getDepth());
    }

    @Test
    void build() {
        WeightAndSize weightAndSize = WeightAndSize.builder()
                .withWeight(WEIGHT)
                .withHeight(HEIGHT)
                .withWidth(WIDTH)
                .withDepth(DEPTH)
                .build();
        assertEquals(WEIGHT, weightAndSize.getWeight());
        assertEquals(HEIGHT, weightAndSize.getHeight());
        assertEquals(WIDTH, weightAndSize.getWidth());
        assertEquals(DEPTH, weightAndSize.getDepth());
    }

    @Test
    void buildWithDefaultIfNull() {
        WeightAndSize weightAndSize = WeightAndSize.builder()
                .withWeight(WEIGHT)
                .withHeight(HEIGHT)
                .build();
        WeightAndSize withDefaultIfNull = weightAndSize.withDefaultIfNull(DEFAULT);
        assertEquals(WEIGHT, withDefaultIfNull.getWeight());
        assertEquals(HEIGHT, withDefaultIfNull.getHeight());
        assertEquals(DEFAULT, withDefaultIfNull.getWidth());
        assertEquals(DEFAULT, withDefaultIfNull.getDepth());
    }
}
