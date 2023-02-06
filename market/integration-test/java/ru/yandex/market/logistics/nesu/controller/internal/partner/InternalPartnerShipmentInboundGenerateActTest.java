package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentInboundGenerateActTest;

@DisplayName("Генерация фактического АПП отгрузки через клиент")
public class InternalPartnerShipmentInboundGenerateActTest extends AbstractPartnerShipmentInboundGenerateActTest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/internal/partner/shipments/" + shipmentId + "/inbound/act";
    }
}
