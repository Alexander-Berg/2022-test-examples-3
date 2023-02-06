package ru.yandex.market.wms.common.provider;

import java.util.List;

import ru.yandex.market.wms.common.spring.dao.entity.OrderDetail;

import static java.util.Arrays.asList;

public class OrderDetailProvider {

    private OrderDetailProvider() {
    }

    public static OrderDetail getOrderDetail(String orderKey, String orderLineNumber) {
        return OrderDetail.builder()
                .orderKey(orderKey)
                .orderLineNumber(orderLineNumber)
                .build();
    }

    public static List<OrderDetail> getOrderDetailsForOrdinalWarehouse() {
        return asList(
                getOrderDetail("0000039466", "00001"),
                getOrderDetail("0000039466", "00002"),
                getOrderDetail("0000039466", "00003")
        );
    }

    public static List<OrderDetail> getOrderDetailsForSuperWarehouse() {
        return asList(
                getOrderDetail("A000039455", "00001"),
                getOrderDetail("A000039455", "00002"),
                getOrderDetail("B000039455", "00003")
        );
    }
}
