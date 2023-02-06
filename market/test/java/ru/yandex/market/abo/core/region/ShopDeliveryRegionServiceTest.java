package ru.yandex.market.abo.core.region;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 12/11/18.
 */
class ShopDeliveryRegionServiceTest extends EmptyTest {
    private static final long TEST_SHOP = 774L;

    @Autowired
    private ShopDeliveryRegionService shopDeliveryRegionService;

    @Test
    void testGetRegion() {
        assertTrue(0 < shopDeliveryRegionService.getRegionForShop(TEST_SHOP));
    }

}