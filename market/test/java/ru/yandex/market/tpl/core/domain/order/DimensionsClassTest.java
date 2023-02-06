package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.BULKY_CARGO;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.BULKY_CARGO_VOLUME_IN_CUBIC_METERS_BOUND;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.BULKY_CARGO_WEIGHT_BOUND;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.REGULAR_CARGO;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.values;

class DimensionsClassTest {

    @DisplayName("(КГТ) Проверяем, что габариты м^2 подходят под определяение КГТ заказа")
    @Test
    void fromDimensionsIsBulkyCargoTest() {
        Dimensions dimensions = new Dimensions();
        dimensions.setHeight(100);
        dimensions.setLength(300);
        dimensions.setWidth(1000);
        int compareCubicMeters = dimensions.calculateVolumeInCubicMeters()
                .compareTo(BULKY_CARGO_VOLUME_IN_CUBIC_METERS_BOUND);

        DimensionsClass dimensionsClass = DimensionsClass.fromDimensions(dimensions);

        Assertions.assertTrue(compareCubicMeters > 0);
        Assertions.assertTrue(dimensions.isBulkyCargo());
        Assertions.assertEquals(dimensionsClass, BULKY_CARGO);
    }

    @DisplayName("(не КГТ) Проверяем, что габариты веса не подходят под определяение КГТ заказа")
    @Test
    void fromDimensionsIsNotBulkyCargoTest() {
        Dimensions dimensions = new Dimensions();
        dimensions.setHeight(0);
        dimensions.setLength(0);
        dimensions.setWidth(0);
        BigDecimal weight = BULKY_CARGO_WEIGHT_BOUND.subtract(BigDecimal.ONE);
        dimensions.setWeight(weight);
        int compareWeightBound = dimensions.getWeight()
                .compareTo(BULKY_CARGO_WEIGHT_BOUND);

        DimensionsClass dimensionsClass = DimensionsClass.fromDimensions(dimensions);

        Assertions.assertFalse(compareWeightBound > 0);
        Assertions.assertFalse(dimensions.isBulkyCargo());
        Assertions.assertEquals(dimensionsClass, REGULAR_CARGO);
    }


    @DisplayName("Проверяем реальное количество типов габаритов груза")
    @Test
    void checkValuesTest() {
        DimensionsClass[] values = values();

        Assertions.assertEquals(values.length, 3);
    }
}
