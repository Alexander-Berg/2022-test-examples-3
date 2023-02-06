package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentInboundGenerateActTest;

@DisplayName("Генерация фактического АПП отгрузки c фронта")
public class PartnerShipmentInboundGenerateActTest extends AbstractPartnerShipmentInboundGenerateActTest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/back-office/partner/shipments/" + shipmentId + "/inbound/act";
    }
}
