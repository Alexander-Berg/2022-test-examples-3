package ru.yandex.market.tpl.core.domain.order.warehouse;

import java.math.BigDecimal;

import lombok.Builder;

@Builder
public class OrderWarehouseAddressTestBuilder {

    @Builder.Default
    private BigDecimal longitude = BigDecimal.ONE;
    @Builder.Default
    private BigDecimal latitude = BigDecimal.TEN;

    public OrderWarehouseAddress get() {
        return new OrderWarehouseAddress(
                "address",
                "country",
                "locality",
                "region",
                "city",
                "st",
                "house",
                "apart",
                1,
                longitude,
                latitude
        );
    }
}
