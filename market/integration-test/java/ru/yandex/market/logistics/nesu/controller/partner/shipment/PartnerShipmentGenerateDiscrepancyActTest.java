package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateDiscrepancyActTest;

@DisplayName("Генерация акта расхождений с фронта")
class PartnerShipmentGenerateDiscrepancyActTest extends AbstractPartnerShipmentGenerateDiscrepancyActTest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return String.format("/back-office/partner/shipments/%d/discrepancy-act", shipmentId);
    }
}
