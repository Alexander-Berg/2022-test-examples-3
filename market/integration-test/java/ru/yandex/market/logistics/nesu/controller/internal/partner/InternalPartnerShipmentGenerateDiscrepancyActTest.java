package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateDiscrepancyActTest;

@DisplayName("Генерация акта расхождений через клиент")
public class InternalPartnerShipmentGenerateDiscrepancyActTest
    extends AbstractPartnerShipmentGenerateDiscrepancyActTest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return String.format("/internal/partner/shipments/%d/discrepancy-act", shipmentId);
    }
}
