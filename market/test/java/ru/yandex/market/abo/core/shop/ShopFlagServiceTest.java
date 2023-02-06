package ru.yandex.market.abo.core.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.message.Flag;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 18/09/2020.
 */
class ShopFlagServiceTest extends EmptyTest {
    private static final long SHOP_ID = -12232;

    @Autowired
    private ShopFlagService shopFlagService;

    @Test
    void testAddShopFlag() {
        shopFlagService.addShopFlag(Flag.THRESHOLD_CPC_START_TIME, SHOP_ID);
        flushAndClear();

        assertTrue(shopFlagService.shopFlagExists(Flag.THRESHOLD_CPC_START_TIME, SHOP_ID));
    }
}