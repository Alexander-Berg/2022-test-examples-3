package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentStatusesTest;

@DisplayName("Получение списка возможных статусов отгрузок с фронта")
class PartnerShipmentStatusesTest extends AbstractPartnerShipmentStatusesTest {
    @Nonnull
    @Override
    protected String url() {
        return "/back-office/partner/shipments/statuses";
    }
}
