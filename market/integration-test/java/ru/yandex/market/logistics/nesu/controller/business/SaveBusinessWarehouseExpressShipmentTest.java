package ru.yandex.market.logistics.nesu.controller.business;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.controller.partner.AbstractNewExpressRelationTest;

@DisplayName("Сохранение связки бизнес-склада с экспрессом")
class SaveBusinessWarehouseExpressShipmentTest extends AbstractNewExpressRelationTest {
    @Nonnull
    @Override
    protected String getUrl(long partnerId) {
        return "/back-office/business/warehouses/" + partnerId + "/shipment-settings";
    }
}
