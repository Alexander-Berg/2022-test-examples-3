package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentConfirmL4STest;

@DisplayName("Подтверждение отгрузки магазина через клиент, через L4S")
class InternalPartnerShipmentConfirmL4STest extends AbstractPartnerShipmentConfirmL4STest {

    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/internal/partner/shipments/" + shipmentId + "/confirm";
    }
}
