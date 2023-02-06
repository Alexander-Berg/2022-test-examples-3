package ru.yandex.market.tpl.core.domain.order.warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Builder;

@Builder
public class OrderWarehouseTestBuilder {

    @Builder.Default
    private String yandexId = "yaId";
    @Builder.Default
    private OrderWarehouseAddress address = OrderWarehouseAddressTestBuilder.builder()
            .longitude(BigDecimal.ZERO)
            .longitude(BigDecimal.ZERO)
            .build().get();

    public OrderWarehouse get() {
        return new OrderWarehouse(
                yandexId,
                "incorporation",
                address,
                new HashMap<>(),
                new ArrayList<>(),
                "descr",
                "cont"
        );
    }
}
