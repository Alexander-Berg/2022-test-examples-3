package ru.yandex.market.logistic.gateway.service.converter.fulfillment;

import java.util.Collections;
import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.BaseTest;

public class OrderStatusHistoryConverterTest extends BaseTest {

    @Test
    public void convertOrderStatusHistoryFromApi() {
        OrderStatusHistory orderStatusHistoryIn = getOrderStatusHistoryApi();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory expectedOrderStatusHistory =
            getOrderStatusHistoryNotApi();

        Optional<ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory> actualOrderStatusHistory
            = OrderStatusHistoryConverter.convertOrderStatusHistoryFromApi(orderStatusHistoryIn);

        assertions.assertThat(actualOrderStatusHistory)
            .as("Asserting that actual order status history optional is present")
            .isPresent();
        assertions.assertThat(actualOrderStatusHistory.get())
            .as("Asserting that order status history wrapper is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(expectedOrderStatusHistory);
    }

    @Test
    public void convertOrderStatusFromApi() {
        OrderStatus orderStatusIn = getOrderStatusApi();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus expectedOrderStatus =
            getOrderStatusNotApi();

        Optional<ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus> actualOrderStatus =
            OrderStatusHistoryConverter.convertOrderStatusFromApi(orderStatusIn);

        assertions.assertThat(actualOrderStatus)
            .as("Asserting that actual order status optional is present")
            .isPresent();
        assertions.assertThat(actualOrderStatus.get())
            .as("Asserting that order status wrapper is converted correctly")
            .isEqualToComparingFieldByFieldRecursively(expectedOrderStatus);
    }

    private OrderStatusHistory getOrderStatusHistoryApi() {
        return new OrderStatusHistory(
            Collections.singletonList(getOrderStatusApi()),
            new ResourceId("12345", "ABC12345"));
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory
    getOrderStatusHistoryNotApi() {
        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusHistory(
            Collections.singletonList(getOrderStatusNotApi()),
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId.ResourceIdBuilder()
                .setYandexId("12345")
                .setPartnerId("ABC12345")
                .build()
        );
    }

    private OrderStatus getOrderStatusApi() {
        return new OrderStatus(
            OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED_FF,
            new DateTime("2017-09-09T10:16:00+03:00"),
            "MyMessage"
        );
    }

    private ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus getOrderStatusNotApi() {
        return new ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatus(
            ru.yandex.market.logistic.gateway.common.model.fulfillment.OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED,
            new ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime("2017-09-09T10:16:00+03:00"),
            "MyMessage"
        );
    }
}
