package ru.yandex.market.core.fulfillment.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.test.utils.UniquenessTestUtils;

/**
 * Тесты для модели {@link Warehouse}.
 */
class WarehouseTest {

    @Test
    void testUniqueness() {
        UniquenessTestUtils.checkUniqueness(Warehouse.class);
    }

}
