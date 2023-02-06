package ru.yandex.market.logistics.nesu.controller.partner.shipment;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGetL4STest;

@DisplayName("Получение данных отгрузки магазина с фронта, через L4S")
class PartnerShipmentGetL4STest extends AbstractPartnerShipmentGetL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/back-office/partner/shipments/" + shipmentId;
    }
}
