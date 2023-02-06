package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentWarehousesTest;

@DisplayName("Получение складов отгрузок магазина с фронта")
class PartnerShipmentWarehousesTest extends AbstractPartnerShipmentWarehousesTest {
    @Nonnull
    @Override
    protected String url() {
        return "/back-office/partner/shipments/warehouses";
    }
}
