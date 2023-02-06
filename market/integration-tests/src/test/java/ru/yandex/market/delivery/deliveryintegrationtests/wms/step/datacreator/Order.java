package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator;

import java.util.List;

import io.qameta.allure.Step;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.DatacreatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;

public class Order {
    private static final DatacreatorClient DATACREATOR_CLIENT = new DatacreatorClient();

    @Step("Получаем историю статусов по заказу {orderId}")
        public List<String> getOrderStatusHistory(String orderId) {
            return DATACREATOR_CLIENT.getOrderStatusHistory(orderId);
    }
}
