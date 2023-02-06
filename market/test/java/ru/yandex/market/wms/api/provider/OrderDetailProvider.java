package ru.yandex.market.wms.api.provider;

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

    public static List<OrderDetail> getOrderDetailsForSuperWarehouse() {
        String orderKey1 = "A000000654";
        String orderKey2 = "B000000654";
        String orderLineNumber1 = "00001";
        String orderLineNumber2 = "00003";
        return asList(
                getOrderDetail(orderKey1, orderLineNumber1),
                getOrderDetail(orderKey2, orderLineNumber2)
        );
    }

    public static List<OrderDetail> getOrderDetailsForOrdinaryWarehouse() {
        String orderKey1 = "0000000654";
        String orderLineNumber1 = "00001";
        String orderLineNumber2 = "00003";
        return asList(
                getOrderDetail(orderKey1, orderLineNumber1),
                getOrderDetail(orderKey1, orderLineNumber2)
        );
    }
}
