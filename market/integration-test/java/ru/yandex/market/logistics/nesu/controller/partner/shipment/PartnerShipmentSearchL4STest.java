package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentSearchL4STest;

@DisplayName("Поиск отгрузок магазина с фронта, через L4S")
class PartnerShipmentSearchL4STest extends AbstractPartnerShipmentSearchL4STest {
    @Override
    @Nonnull
    protected String url() {
        return "/back-office/partner/shipments/search";
    }
}
