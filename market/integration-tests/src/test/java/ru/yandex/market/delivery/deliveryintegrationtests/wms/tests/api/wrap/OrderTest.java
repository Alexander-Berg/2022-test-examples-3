package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.wrap;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ApiClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Order;

@DisplayName("API: Order")
@Epic("API Tests")
@Slf4j
public class OrderTest {
    private final ApiClient apiClient = new ApiClient();

    private final Order SHIPPED_ORDER = new Order(1555688309453L, "0000000908");

    @Test
    @DisplayName("getOrderStatus")
    public void getOrderStatusTest() {
        log.info("Testing getOrderStatus");

        apiClient.getOrderStatus(SHIPPED_ORDER)
                .body("root.response.orderStatusHistories.orderStatusHistory.history.orderStatus.statusCode",
                        Matchers.is("130"));
    }

    @Test
    @DisplayName("getOrderHistory")
    public void getOrderHistoryTest() {
        log.info("Testing getOrderHistory");

        apiClient.getOrderHistory(SHIPPED_ORDER)
                .body("root.response.orderStatusHistory.history.orderStatus.find {it.statusCode == '130'}.statusCode",
                        Matchers.is("130"))
                .body("root.response.orderStatusHistory.history.orderStatus.find {it.statusCode == '120'}.statusCode",
                        Matchers.is("120"))
                .body("root.response.orderStatusHistory.history.orderStatus.find {it.statusCode == '110'}.statusCode",
                        Matchers.is("110"))
                .body("root.response.orderStatusHistory.history.orderStatus.find {it.statusCode == '101'}.statusCode",
                        Matchers.is("101"));
    }
}
