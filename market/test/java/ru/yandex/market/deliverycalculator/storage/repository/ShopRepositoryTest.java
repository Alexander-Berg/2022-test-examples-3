package ru.yandex.market.deliverycalculator.storage.repository;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link ShopRepository}.
 */
public class ShopRepositoryTest extends FunctionalTest {

    @Autowired
    private ShopRepository tested;

    /**
     * Тест на {@link ShopRepository#findBySenderId(long)}.
     * Случай: искомый магазин найден.
     */
    @DbUnitDataSet(before = "database/searchShops.before.csv")
    @Test
    public void testFindBySenderId_found() {
        Optional<DeliveryShop> shop = tested.findBySenderId(3L);

        assertTrue(shop.isPresent());
        assertEquals(3L, shop.get().getId());
    }

    /**
     * Тест на {@link ShopRepository#findBySenderId(long)}.
     * Случай: искомый магазин найден.
     */
    @DbUnitDataSet(before = "database/searchShops.before.csv")
    @Test
    public void testFindBySenderId_notFound() {
        Optional<DeliveryShop> shop = tested.findBySenderId(5L);

        assertFalse(shop.isPresent());
    }
}
