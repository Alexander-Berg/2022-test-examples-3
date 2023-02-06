package ru.yandex.market.logistics.nesu.controller;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.AbstractTvmAuthDeliveryOptionsSearchTest;

@DisplayName("Поиск вариантов доставки")
class BackOfficeDeliveryOptionsSearchTest extends AbstractTvmAuthDeliveryOptionsSearchTest {
    @Nonnull
    protected String uri() {
        return "/back-office/delivery-options";
    }
}
