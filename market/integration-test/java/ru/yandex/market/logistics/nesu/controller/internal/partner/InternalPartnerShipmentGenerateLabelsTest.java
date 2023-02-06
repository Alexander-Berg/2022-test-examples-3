package ru.yandex.market.logistics.nesu.controller.internal.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.partner.AbstractPartnerShipmentGenerateLabelsTest;

@DisplayName("Генерация ярлыков заказов через клиент")
class InternalPartnerShipmentGenerateLabelsTest extends AbstractPartnerShipmentGenerateLabelsTest {
    @Nonnull
    @Override
    protected String url() {
        return "/internal/partner/shipments/labels";
    }
}
