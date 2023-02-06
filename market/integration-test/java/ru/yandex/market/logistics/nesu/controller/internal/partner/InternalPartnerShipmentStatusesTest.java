package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentStatusesTest;

@DisplayName("Получение списка возможных статусов отгрузок через клиент")
class InternalPartnerShipmentStatusesTest extends AbstractPartnerShipmentStatusesTest {
    @Nonnull
    @Override
    protected String url() {
        return "/internal/partner/shipments/statuses";
    }
}
