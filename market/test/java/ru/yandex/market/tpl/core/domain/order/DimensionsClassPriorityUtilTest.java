package ru.yandex.market.tpl.core.domain.order;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.BULKY_CARGO;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.MEDIUM_SIZE_CARGO;
import static ru.yandex.market.tpl.core.domain.order.DimensionsClass.REGULAR_CARGO;

class DimensionsClassPriorityUtilTest {

    @DisplayName("(КГТ) Если в списке габаритов есть крупногабаритный груз, " +
            "то мы должны выделить этот признак как более важный")
    @Test
    void getPriorityCargoDimensionsBulkyCargoTest() {
        List<DimensionsClass> dimensions = List.of(
                REGULAR_CARGO,
                BULKY_CARGO,
                MEDIUM_SIZE_CARGO
        );

        DimensionsClass dimensionsClass = DimensionsClassPriorityUtil.getPriorityCargoDimensions(dimensions);

        Assertions.assertTrue(dimensions.contains(BULKY_CARGO));
        Assertions.assertEquals(dimensionsClass, BULKY_CARGO);
    }

    @DisplayName("(не КГТ) Если в списке габаритов есть нет крупногабаритного груза, " +
            "то мы должны выделить, что общеважный признак также не КГТ")
    @Test
    void getPriorityCargoDimensionsRegularCargoTest() {
        List<DimensionsClass> dimensions = List.of(REGULAR_CARGO);

        DimensionsClass dimensionsClass = DimensionsClassPriorityUtil.getPriorityCargoDimensions(dimensions);

        Assertions.assertEquals(dimensionsClass, REGULAR_CARGO);
    }

    @DisplayName("Пустой тип габарита груза при отсутсвии списка габаритов")
    @Test
    void getPriorityCargoDimensionsNullTest() {
        List<DimensionsClass> dimensions = List.of();

        DimensionsClass dimensionsClass = DimensionsClassPriorityUtil.getPriorityCargoDimensions(dimensions);

        Assertions.assertNull(dimensionsClass);
    }

    @DisplayName("Все типы габаритов груза приоритезированы")
    @Test
    void checkAllDimensionCargoIsPrioritisationTest() {
        Set<DimensionsClass> dimensionsCargoByPriority = DimensionsClassPriorityUtil.cargoPriority.keySet();
        List<DimensionsClass> dimensionsCargoList = Arrays.asList(DimensionsClass.values());

        Assertions.assertTrue(
                dimensionsCargoList.containsAll(dimensionsCargoByPriority)
        );
    }
}
