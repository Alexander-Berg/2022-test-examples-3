package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateActL4STest;

@DisplayName("Генерация АПП отгрузки с фронта, через L4S")
public class ShipmentOutboundGenerateActL4STest extends AbstractPartnerShipmentGenerateActL4STest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/back-office/partner/shipments/" + shipmentId + "/act";
    }
}
