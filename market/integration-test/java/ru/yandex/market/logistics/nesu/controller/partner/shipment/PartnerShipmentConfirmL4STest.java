package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentConfirmL4STest;

@DisplayName("Подтверждение отгрузки магазина с фронта через L4S")
class PartnerShipmentConfirmL4STest extends AbstractPartnerShipmentConfirmL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/back-office/partner/shipments/" + shipmentId + "/confirm";
    }
}
