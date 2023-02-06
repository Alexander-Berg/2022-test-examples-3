package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentSearchL4STest;

@DisplayName("Поиск отгрузок магазина через клиент, через L4S")
class InternalPartnerShipmentSearchL4STest extends AbstractPartnerShipmentSearchL4STest {
    @Override
    @Nonnull
    protected String url() {
        return "/internal/partner/shipments/search";
    }
}
