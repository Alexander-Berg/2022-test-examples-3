package ru.yandex.market.wms.autostart.autostartlogic;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dao.entity.SkuId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ResilienceLogicTest {
    @Test
    public void test() throws InterruptedException {
        ResilienceLogic resilienceLogic = new ResilienceLogic(1);

        SkuId skuId1 = SkuId.of("storer1", "sku1");
        SkuId skuId2 = SkuId.of("storer2", "sku2");
        assertEquals(0, resilienceLogic.cacheSize());
        assertFalse(resilienceLogic.getIgnoredSkuIds().contains(skuId1));
        assertFalse(resilienceLogic.getIgnoredSkuIds().contains(skuId2));
        resilienceLogic.addIgnoredSkuId(skuId1);
        Thread.sleep(500);
        assertEquals(1, resilienceLogic.cacheSize());
        resilienceLogic.addIgnoredSkuId(skuId2);
        resilienceLogic.addIgnoredSkuId(skuId2);
        assertEquals(2, resilienceLogic.cacheSize());
        assertTrue(resilienceLogic.getIgnoredSkuIds().contains(skuId1));
        assertTrue(resilienceLogic.getIgnoredSkuIds().contains(skuId2));
        Thread.sleep(500);
        assertEquals(2, resilienceLogic.cacheSize());
        Thread.sleep(500);
        assertFalse(resilienceLogic.getIgnoredSkuIds().contains(skuId1));
        assertFalse(resilienceLogic.getIgnoredSkuIds().contains(skuId2));
    }
}
