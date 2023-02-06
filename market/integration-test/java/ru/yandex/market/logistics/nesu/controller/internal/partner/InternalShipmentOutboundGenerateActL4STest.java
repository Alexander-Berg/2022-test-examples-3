package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateActL4STest;

@DisplayName("Генерация АПП отгрузки через клиент, через L4S")
public class InternalShipmentOutboundGenerateActL4STest extends AbstractPartnerShipmentGenerateActL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/internal/partner/shipments/" + shipmentId + "/act";
    }
}
