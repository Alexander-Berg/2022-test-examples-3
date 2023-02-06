package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateLabelsTest;

@DisplayName("Генерация ярлыков заказов с фронта")
class PartnerShipmentGenerateLabelsTest extends AbstractPartnerShipmentGenerateLabelsTest {
    @Nonnull
    @Override
    protected String url() {
        return "/back-office/partner/shipments/labels";
    }
}
