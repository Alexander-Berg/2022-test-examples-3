package ru.yandex.market.logistics.nesu.controller.internal;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;

import ru.yandex.market.logistics.nesu.base.AbstractTvmAuthDeliveryOptionsSearchTest;

@DisplayName("Поиск вариантов доставки")
class InternalDeliveryOptionsSearchTest extends AbstractTvmAuthDeliveryOptionsSearchTest {
    @Nonnull
    protected String uri() {
        return "/internal/delivery-options";
    }
}
