package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGetL4STest;

@DisplayName("Получение данных отгрузки магазина через клиент, через L4S")
class InternalPartnerShipmentGetL4STest extends AbstractPartnerShipmentGetL4STest {
    @Nonnull
    @Override
    protected String url(long shipmentId) {
        return "/internal/partner/shipments/" + shipmentId;
    }
}
