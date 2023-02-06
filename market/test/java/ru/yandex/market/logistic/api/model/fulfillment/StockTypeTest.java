package ru.yandex.market.logistic.api.model.fulfillment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit тесты для {@link StockType}.
 */
class StockTypeTest {

    /**
     * Проверка валидного создания энума по коду.
     */
    @Test
    void createTest() {
        for (StockType stockType : StockType.values()) {
            assertEquals(stockType, StockType.create(stockType.getCode()));
        }
    }
}
