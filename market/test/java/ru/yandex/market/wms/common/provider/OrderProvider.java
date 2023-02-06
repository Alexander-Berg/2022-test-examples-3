package ru.yandex.market.wms.common.provider;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import ru.yandex.market.wms.common.spring.dao.entity.Order;

public class OrderProvider {

    private OrderProvider() {
    }

    public static Order getOrder(String orderKey,
                                 String externOrderKey,
                                 String status,
                                 BigDecimal maxAbsentItemsPricePercent,
                                 int version) {
        Order order = Order.builder()
                .status(status)
                .door("S01")
                .carrierCode("100")
                .sumQtyallocated(new BigDecimal("1.00000"))
                .rowVersion(version)
                .sumQtyPicked(new BigDecimal("1.00000"))
                .sumQtyOpen(new BigDecimal("3.00000"))
                .totalqty(new BigDecimal("1.00000"))
                .susr2(null)
                .type("14")
                .orderKey(orderKey)
                .trailerKey("")
                .externalOrderKey(externOrderKey)
                .maxAbsentItemsPricePercent(maxAbsentItemsPricePercent)
                .scheduledShipDate(OffsetDateTime.of(2020, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(3)))
                .build();

        return order;
    }

    public static Order getOrder(String orderKey,
                                 String externOrderKey,
                                 String status,
                                 int version,
                                 BigDecimal sumQtyallocated,
                                 OffsetDateTime scheduledShipDate,
                                 OffsetDateTime scheduledShipDateInDB) {
        Order order = getOrder(orderKey, externOrderKey, status, new BigDecimal("99.00"), version);
        order.setSumQtyallocated(sumQtyallocated);
        order.setScheduledShipDate(scheduledShipDate);
        order.setScheduledShipDateInDB(scheduledShipDateInDB);

        return order;
    }

    @SuppressWarnings("ParameterNumber")
    public static Order getOrder(String orderKey,
                                 String externOrderKey,
                                 String status,
                                 int version,
                                 BigDecimal sumQtyallocated,
                                 OffsetDateTime scheduledShipDate,
                                 OffsetDateTime scheduledShipDateInDB,
                                 OffsetDateTime shipmentDateTime) {
        Order order = getOrder(orderKey, externOrderKey, status, version, sumQtyallocated, scheduledShipDate,
                scheduledShipDateInDB);
        order.setShipmentDateTime(shipmentDateTime);

        return order;
    }
}
