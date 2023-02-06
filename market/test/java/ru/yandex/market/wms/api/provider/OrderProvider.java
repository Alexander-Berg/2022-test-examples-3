package ru.yandex.market.wms.api.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.spring.dao.entity.Order;

public class OrderProvider {

    private OrderProvider() {
    }

    public static Order getOrder(String originOrderKey,
                                 String orderKey,
                                 OffsetDateTime scheduledShipDate,
                                 String status) {
        Order order = Order.builder()
                .status(status)
                .originOrderKey(originOrderKey)
                .orderKey(orderKey)
                .externalOrderKey(originOrderKey + "EXT")
                .door("door")
                .carrierCode("code")
                .carrierName("name")
                .scheduledShipDate(scheduledShipDate)
                .totalqty(BigDecimal.valueOf(1))
                .susr2("1")
                .type(OrderType.OUTBOUND_AUTO.getCode())
                .maxAbsentItemsPricePercent(BigDecimal.valueOf(99.0))
                .build();

        return order;
    }
}
