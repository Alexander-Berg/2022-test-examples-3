package ru.yandex.market.tpl.integration.tests.facade;

import java.util.Optional;

import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.api.model.delivery.response.UpdateOrderResponse;
import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.integration.tests.client.DeliveryApiClient;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.DeliveryTestConfiguration;
import ru.yandex.market.tpl.integration.tests.utils.TplTestUtils;

@Component
@RequiredArgsConstructor
public class DeliveryApiFacade extends BaseFacade {
    @Value("${delivery.yandex.id}")
    private Long yandexId;
    @Value("${delivery.tkn}")
    private String apiToken;
    private final DeliveryApiClient deliveryApiClient;
    private final ManualApiClient manualApiClient;

    public CreateOrderResponse createOrder() {
        return createOrder(yandexId + "", getDeliveryConfig());
    }

    @Step("Создание заказа")
    public CreateOrderResponse createOrder(String externalOrderId, DeliveryTestConfiguration configuration) {
        String request = TplTestUtils.readRequestAsString(configuration.getCreateOrderRequestPath(), apiToken,
                externalOrderId, configuration.getSenderId(), configuration.getWarehouseId());
        CreateOrderResponse response = deliveryApiClient.createOrder(request);
        String orderId = Optional.ofNullable(response.getOrderId()).map(ResourceId::getPartnerId)
                .orElseThrow(() -> new RuntimeException("Bad response"));
        getContext().setOrderId(orderId);
        return response;
    }

    public UpdateOrderResponse updateOrder() {
        return updateOrder(yandexId + "", getDeliveryConfig());
    }

    @Step("Обновление заказа")
    public UpdateOrderResponse updateOrder(String externalOrderId, DeliveryTestConfiguration configuration) {
        String request = TplTestUtils.readRequestAsString(
            configuration.getUpdateOrderRequestPath(),
            apiToken,
            externalOrderId,
            configuration.getSenderId(),
            configuration.getWarehouseId(),
            configuration.getDeliveryDate()
        );
        return deliveryApiClient.updateOrder(request);
    }


    public GetOrdersStatusResponse getOrderStatus() {
        return getOrderStatus(yandexId + "", getDeliveryConfig());
    }

    @Step("Получение статуса заказа")
    public GetOrdersStatusResponse getOrderStatus(String externalOrderId, DeliveryTestConfiguration configuration) {
        String request = TplTestUtils.readRequestAsString(configuration.getGetOrdersStatusRequestPath(), apiToken,
                externalOrderId);
        return deliveryApiClient.getOrdersStatus(request);
    }

    public GetOrderHistoryResponse getOrderStatusHistory() {
        return getOrderStatusHistory(yandexId + "", getDeliveryConfig());
    }

    @Step("Получение истории статусов заказа")
    public GetOrderHistoryResponse getOrderStatusHistory(String externalOrderId,
                                                         DeliveryTestConfiguration configuration) {
        String request = TplTestUtils.readRequestAsString(configuration.getGetOrderHistoryRequestPath(), apiToken,
                externalOrderId);
        return deliveryApiClient.getOrderHistory(request);
    }

    public DetailedOrderDto getDetailedOrderInfo() {
        return manualApiClient.getDetailedOrderInfo(getContext().getOrderId());
    }


}
