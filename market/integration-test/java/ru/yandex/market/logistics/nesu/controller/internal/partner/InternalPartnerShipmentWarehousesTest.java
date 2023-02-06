package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentWarehousesTest;

@DisplayName("Получение складов отгрузок магазина через клиент")
public class InternalPartnerShipmentWarehousesTest extends AbstractPartnerShipmentWarehousesTest {

    @Nonnull
    @Override
    protected String url() {
        return "/internal/partner/shipments/warehouses";
    }
}
