package ru.yandex.market.deliverycalculator.storage.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.model.DeliveryShop;

class DeliveryCalculatorStorageServiceTest extends FunctionalTest {
    @Autowired
    private DeliveryCalculatorStorageService storageService;

    @Test
    void testInsertShop() {
        long shopId = 100;
        DeliveryShop shop = StorageTestUtils.getShop(shopId);
        storageService.deleteShop(shopId);
        storageService.insertShop(shop);
        storageService.deleteShop(shopId);
    }

    @Test
    void testGetOldestNotExportedShopsEmpty() {
        storageService.getShopIdsWithNotExportedCourierTariff();
    }

    @Test
    void testGetOldestNotExportedShops() {
        long shopId = 200;
        DeliveryShop shop = StorageTestUtils.getShop(shopId);
        storageService.deleteShop(shopId);
        storageService.insertShop(shop);
        storageService.getShopIdsWithNotExportedCourierTariff();
    }

    @Test
    void testMarkShopExportedIfNotChanged() {
        long shopId = 300;
        DeliveryShop shop = StorageTestUtils.getShop(shopId);
        storageService.deleteShop(shopId);
        storageService.insertShop(shop);
        storageService.markShopCourierTariffExportedIfNotChanged(shopId, shop.getCourierUpdateTime(), 3);
    }

}
