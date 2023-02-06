package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderProvider;

public class GetOrderChangeRequestsByEvenTest extends AbstractWebTestBase {

    private final ClientInfo shop = new ClientInfo(ClientRole.SHOP, OrderProvider.SHOP_ID);

    @Autowired
    private OrderHistoryEventsTestHelper orderHistoryEventsTestHelper;

    @Test
    @DisplayName("Ищем ChangeRequest с типом DELIVERY_DATES по ID события c типом ORDER_CHANGE_REQUEST_CREATED")
    public void findOrderChangeRequestsByDeliveryDatesChangedEventId() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        var order = orderCreateHelper.createOrder(parameters);

        // Изменяем дату доставки заказа
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setDeliveryEditRequest(DeliveryEditRequest.newDeliveryEditRequest()
                .deliveryServiceId(order.getDelivery().getDeliveryServiceId())
                .fromDate(LocalDate.now(getClock()).plusDays(3))
                .toDate(LocalDate.now(getClock()).plusDays(4))
                .timeInterval(new TimeInterval(LocalTime.of(10, 0), LocalTime.of(18, 0)))
                .reason(HistoryEventReason.USER_MOVED_DELIVERY_DATES_BY_DS)
                .build());

        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.SHOP,
                shop.getShopId(),
                Collections.singletonList(Color.WHITE),
                orderEditRequest);

        //Получаем все OrderHistoryEvent'ы по заказу с типом ORDER_CHANGE_REQUEST_CREATED
        var orderHistoryEvent = orderHistoryEventsTestHelper.getEventsOfType(order.getId(),
                HistoryEventType.ORDER_CHANGE_REQUEST_CREATED)
                .stream()
                .findAny().orElse(null);
        Assertions.assertNotNull(orderHistoryEvent);

        //Получаем ChangeRequest'ы по заказу с указанным eventId
        var changeRequestFromClient = client.getChangeRequestsByEventId(
                order.getId(),
                orderHistoryEvent.getId(),
                new RequestClientInfo(shop.getRole(), shop.getShopId()))
                .stream().filter(cr -> cr.getType() == ChangeRequestType.DELIVERY_DATES)
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(changeRequestFromClient);

        //Проверяем что получили ожидаемый ChangeRequest
        Assertions.assertTrue(changeRequests
                .stream()
                .map(ChangeRequest::getId)
                .collect(Collectors.toList())
                .contains(changeRequestFromClient.getId()));
    }

    @Test
    @DisplayName("Ищем ChangeRequest с типом CANCELLATION по ID события c типом ORDER_CHANGE_REQUEST_CREATED")
    public void findOrderChangeRequestByCancellationEventId() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        var order = orderCreateHelper.createOrder(parameters);

        // Отменяем заказ
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        var orderCancellationRequest = new OrderEditRequest();
        orderCancellationRequest.setCancellationRequest(CancellationRequest.builder()
                .substatus(OrderSubstatus.SHOP_FAILED)
                .build());

        var changeRequests = client.editOrder(order.getId(),
                ClientRole.SHOP,
                OrderProvider.SHOP_ID,
                Collections.singletonList(Color.WHITE),
                orderCancellationRequest);

        //Получаем все OrderHistoryEvent'ы по заказу с типом ORDER_CHANGE_REQUEST_CREATED
        var orderHistoryEvent = orderHistoryEventsTestHelper
                .getEventsOfType(order.getId(),
                        HistoryEventType.ORDER_CHANGE_REQUEST_CREATED)
                .stream().findAny().orElse(null);

        Assertions.assertNotNull(orderHistoryEvent);

        //Получаем ChangeRequest'ы по заказу с указанным eventId
        var changeRequestFromClient = client.getChangeRequestsByEventId(
                order.getId(),
                orderHistoryEvent.getId(),
                new RequestClientInfo(shop.getRole(), shop.getShopId()))
                .stream().filter(cr -> cr.getType() == ChangeRequestType.CANCELLATION)
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(changeRequestFromClient);

        //Проверяем что получили ожидаемый ChangeRequest
        Assertions.assertTrue(changeRequests
                .stream()
                .map(ChangeRequest::getId)
                .collect(Collectors.toList())
                .contains(changeRequestFromClient.getId()));
    }

    @Test
    @DisplayName("Ищем ChangeRequest'ы по несуществующему заказу")
    public void findChangeRequestByNonExistedOrder() {
        Assertions.assertThrows(OrderNotFoundException.class, () -> client.getChangeRequestsByEventId(
                123L,
                123L,
                new RequestClientInfo(shop.getRole(), shop.getShopId())));
    }

    @Test
    @DisplayName("Ищем ChangeRequest'ы по несуществующему eventId")
    public void findChangeRequestByNonExistedEventId() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(Color.WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        var order = orderCreateHelper.createOrder(parameters);

        var changeRequestFromClient = client.getChangeRequestsByEventId(
                order.getId(),
                123L,
                new RequestClientInfo(shop.getRole(), shop.getShopId()));
        Assertions.assertEquals(0, changeRequestFromClient.size());
    }
}
