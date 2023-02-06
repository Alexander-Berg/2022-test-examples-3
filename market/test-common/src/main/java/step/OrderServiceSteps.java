package step;

import java.util.List;

import javax.annotation.Nonnull;

import client.OrderServiceClient;
import factory.orderservice.DeliveryOptions;
import factory.orderservice.OrderCreationRequest;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.order_service.client.model.ChangeOrderStatusResponse;
import ru.yandex.market.order_service.client.model.CreateExternalOrderResponse;
import ru.yandex.market.order_service.client.model.CreateExternalOrderResponseResult;
import ru.yandex.market.order_service.client.model.DeliveryOptionDto;
import ru.yandex.market.order_service.client.model.DeliveryOptionsDto;
import ru.yandex.market.order_service.client.model.GetDeliveryOptionsResponse;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsDto;
import ru.yandex.market.order_service.client.model.OrderStatus;
import ru.yandex.market.order_service.client.model.OrderSubStatus;
import ru.yandex.market.order_service.client.model.OrderSubStatus2;

@Slf4j
public class OrderServiceSteps {

    private static final OrderServiceClient ORDER_SERVICE_CLIENT = new OrderServiceClient();

    @Nonnull
    @Step("Выбираем валидную опцию доставки")
    public DeliveryOptionsDto getDeliveryOptions(long shopId) {
        return Retrier.clientRetry(() -> {
            GetDeliveryOptionsResponse response = ORDER_SERVICE_CLIENT.getDeliveryOptions(
                shopId,
                DeliveryOptions.DEFAULT.getRequest()
            );
            Assertions.assertNotNull(response.getResult(), "Нет доступных опций доставки");
            Assertions.assertNotNull(response.getResult().getOptions(), "Нет доступных опций доставки");
            Assertions.assertNotEquals(0, response.getResult().getOptions().size(), "Нет доступных опций доставки");
            return response.getResult();
        });
    }

    @Nonnull
    @Step("Создание заказа")
    public CreateExternalOrderResponseResult createOrder(long shopId, DeliveryOptionDto deliveryOptionDto) {
        return Retrier.clientRetry(() -> {
            CreateExternalOrderResponse response = ORDER_SERVICE_CLIENT.createOrder(
                shopId,
                OrderCreationRequest.DEFAULT.getRequest(deliveryOptionDto)
            );
            Assertions.assertNotNull(response.getResult(), "Ошибка при создании заказа");
            return response.getResult();
        });
    }

    @Step("Получение данных о заказе")
    public GetOrderLogisticsDto getOrder(
        long shopId,
        long orderId
    ) {
        return Retrier.retry(() -> {
            GetOrderLogisticsDto order = ORDER_SERVICE_CLIENT.getOrder(shopId, orderId).getOrder();
            Assertions.assertNotNull(order, "Заказ не найден");
            return order;
        });
    }

    @Step("Валидация статуса заказа")
    public void assertOrderStatus(
        long shopId,
        long orderId,
        OrderStatus expectedStatus,
        OrderSubStatus expectedSubStatus
    ) {
        Retrier.retry(() -> {
            GetOrderLogisticsDto order = ORDER_SERVICE_CLIENT.getOrder(shopId, orderId).getOrder();
            Assertions.assertEquals(expectedStatus, order.getStatus(), "Статус заказа не соответствует ожидаемому");
            Assertions.assertEquals(
                expectedSubStatus,
                order.getSubstatus(),
                "Подстатус заказа не соответствует ожидаемому"
            );
        });
    }

    @Step("Отмена заказа")
    public ChangeOrderStatusResponse cancelOrder(long shopId, long orderId, OrderSubStatus2 subStatus) {
        return Retrier.clientRetry(() -> {
            ChangeOrderStatusResponse changeOrderStatusResponse = ORDER_SERVICE_CLIENT.cancelOrder(
                shopId,
                orderId,
                subStatus
            );
            Assertions.assertNotNull(changeOrderStatusResponse.getResult(), "Ошибка при отмене заказа");
            Assertions.assertTrue(
                BooleanUtils.isTrue(changeOrderStatusResponse.getResult().getCancellationRequestCreated()),
                "Не создалась заявка на отмену заказа"
            );
            return changeOrderStatusResponse;
        });
    }

    @Step("Валидация КИЗов заказа")
    public void assertCis(long shopId, long orderId, String cis) {
        Retrier.retry(() -> {
            GetOrderLogisticsDto order = ORDER_SERVICE_CLIENT.getOrder(shopId, orderId).getOrder();
            Assertions.assertIterableEquals(order.getItems().get(0).getCis(), List.of(cis));
        });
    }

    @Step("Валидация КИЗов заказа")
    public void assertItemCount(long shopId, long orderId, int count) {
        Retrier.retry(() -> {
            GetOrderLogisticsDto order = ORDER_SERVICE_CLIENT.getOrder(shopId, orderId).getOrder();
            Assertions.assertEquals(order.getItems().get(0).getCount(), count);
        });
    }
}
