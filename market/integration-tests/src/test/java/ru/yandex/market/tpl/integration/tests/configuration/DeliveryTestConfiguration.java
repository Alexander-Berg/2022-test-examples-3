package ru.yandex.market.tpl.integration.tests.configuration;

import lombok.Data;

@Data
public class DeliveryTestConfiguration {
    private String createOrderRequestPath;
    private String updateOrderRequestPath;
    private String getOrdersStatusRequestPath;
    private String getOrderHistoryRequestPath;
    private String senderId = "431782";
    private String warehouseId = "10000010736";
    private String deliveryDate;
}
