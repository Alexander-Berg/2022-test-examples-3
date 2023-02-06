package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PICKUP;

public class ChangeRequestCancelDsbsTest extends AbstractWebTestBase {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
                Stream.of(
                        OrderSubstatus.USER_CHANGED_MIND,
                        OrderSubstatus.USER_REFUSED_DELIVERY,
                        OrderSubstatus.USER_BOUGHT_CHEAPER,
                        OrderSubstatus.CUSTOM,
                        OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
                        OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                        OrderSubstatus.REPLACING_ORDER)
                        .map(ss -> new Object[]{DELIVERY, DeliveryType.DELIVERY, ss}),
                Stream.of(
                        OrderSubstatus.USER_CHANGED_MIND,
                        OrderSubstatus.USER_REFUSED_DELIVERY,
                        OrderSubstatus.USER_BOUGHT_CHEAPER,
                        OrderSubstatus.CUSTOM,
                        OrderSubstatus.USER_WANTS_TO_CHANGE_ADDRESS,
                        OrderSubstatus.USER_WANTS_TO_CHANGE_DELIVERY_DATE,
                        OrderSubstatus.REPLACING_ORDER)
                        .map(ss -> new Object[]{PICKUP, DeliveryType.PICKUP, ss})
        ).flatMap(Function.identity()).map(Arguments::of);
    }


    @ParameterizedTest
    @MethodSource("testParameters")
    @DisplayName("Пользователь не может апплаить инициированные им отмены DSBS заказов в статусе " +
            "DELIVERY, PICKUP, но может реджектить")
    public void avoidApplyingDsbsOrderCancellationByUser(OrderStatus status, DeliveryType deliveryType,
                                                         OrderSubstatus substatus) {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(deliveryType);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, status);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(substatus, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(USER, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе APPLIED");
        assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе REJECTED");


        //Пробуем перевести ChangeRequest в статус APPLIED
        var changeRequestId = changeRequest.getId();
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            client.updateChangeRequestStatus(order.getId(),
                    changeRequestId,
                    USER,
                    order.getBuyer().getUid(),
                    new ChangeRequestPatchRequest(
                            ChangeRequestStatus.APPLIED,
                            null,
                            null));
        });
        //Заказ не должен быть отменен и ChangeRequest не должен переведен в статус APPLIED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                        ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе APPLIED");

        //Пробуем перевести ChangeRequest в статус REJECTED
        client.updateChangeRequestStatus(order.getId(),
                changeRequest.getId(),
                USER,
                order.getBuyer().getUid(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.REJECTED,
                        null,
                        new CancellationRequestPayload(
                                substatus,
                                null,
                                null,
                                ConfirmationReason.DELIVERY)));
        //Заказ не должен быть отменен и ChangeRequest должен переведен в статус REJECTED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        assertEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(), "ChangeRequest должен быть в" +
                " статусе REJECTED");
    }

    @Test
    @DisplayName("Пользователь может отменить DSBS заказ в статусе PROCESSING")
    public void applyingDsbsProcessingOrderCancellationByUser() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(USER, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ должен быть отменен и ChangeRequest должен быть в статусе APPLIED
        assertEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest должен " +
                "быть в статусе APPLIED");
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    @DisplayName("Магазин может апплаить отмены инициированные пользователем по DSBS заказам в статусе " +
            "DELIVERY, PICKUP")
    public void applyingDsbsOrderCancellationByShop(OrderStatus status, DeliveryType deliveryType,
                                                    OrderSubstatus substatus) {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(deliveryType);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, status);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(substatus, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(USER, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе APPLIED");
        assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе REJECTED");

        //Пробуем перевести ChangeRequest в статус APPLIED от имени магазина
        var changeRequestId = changeRequest.getId();
        client.updateChangeRequestStatus(order.getId(),
                changeRequestId,
                ClientRole.SHOP,
                order.getShopId(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.APPLIED,
                        null,
                        null));

        //Заказ должен быть отменен и ChangeRequest должен быть переведен в статус APPLIED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                        ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        assertEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest не должен быть" +
                " в статусе APPLIED");
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    @DisplayName("Магазин может реджектить отмены инициированные пользователем по DSBS заказам в статусе " +
            "DELIVERY, PICKUP")
    public void rejectingDsbsOrderCancellationByShop(OrderStatus status, DeliveryType deliveryType,
                                                     OrderSubstatus substatus) {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(deliveryType);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, status);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(substatus, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(USER, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе APPLIED");
        assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(), "ChangeRequest не должен " +
                "быть в статусе REJECTED");
        Assertions.assertNotNull(orderAfter.getCancellationRequest());
        orderAfter.getDelivery().getParcels().forEach(p -> Assertions.assertNotNull(p.getCancellationRequest()));

        //Пробуем перевести ChangeRequest в статус REJECTED от имени магазина
        client.updateChangeRequestStatus(order.getId(),
                changeRequest.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.REJECTED,
                        null,
                        new CancellationRequestPayload(
                                substatus,
                                null,
                                null,
                                ConfirmationReason.DELIVERY)));

        //Заказ не должен быть отменен и ChangeRequest должен переведен в статус REJECTED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest =
                orderAfter.getChangeRequests().stream().filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        assertEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(), "ChangeRequest должен быть в" +
                " статусе REJECTED");
        Assertions.assertNull(orderAfter.getCancellationRequest());
        order.getDelivery().getParcels().stream()
                .filter(p -> p.getCancellationRequest() != null)
                .forEach(p -> assertEquals(p.getCancellationRequest().getRequestStatus(),
                        CancellationRequestStatus.REJECTED));
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    @DisplayName("Магазин может реджектить отмены инициированные пользователем по DSBS заказам в статусе " +
            "DELIVERY, PICKUP. Если ConfirmationReason == DELIVERED заказ перводится в статус OrderStatus.DELIVERED")
    public void rejectingDsbsOrderCancellationByShopWithDeliveredReason(OrderStatus status, DeliveryType deliveryType,
                                                                        OrderSubstatus substatus) {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(deliveryType);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderToStatus(order, status);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(substatus, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(USER, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(),
                "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest = orderAfter.getChangeRequests()
                .stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny()
                .orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе APPLIED");
        assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе REJECTED");

        //Пробуем перевести ChangeRequest в статус REJECTED от имени магазина
        client.updateChangeRequestStatus(order.getId(),
                changeRequest.getId(),
                ClientRole.SHOP,
                order.getShopId(),
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.REJECTED,
                        null,
                        new CancellationRequestPayload(
                                substatus,
                                null,
                                null,
                                ConfirmationReason.DELIVERED)));

        //Заказ должен быть переведн в статус DELIVERED и ChangeRequest должен переведен в статус REJECTED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        assertEquals(OrderStatus.DELIVERED, orderAfter.getStatus(),
                "Заказ должен быть в статусе DELIVERED");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequestAfterReject =
                orderAfter.getChangeRequests()
                        .stream()
                        .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                        .findAny()
                        .orElse(null);
        Assertions.assertNotNull(changeRequestAfterReject,
                "ChangeRequest с типом CANCELLATION должен существовать");
        assertEquals(ChangeRequestStatus.REJECTED, changeRequestAfterReject.getStatus(),
                "ChangeRequest должен быть в статусе REJECTED");
    }

    @Test
    @DisplayName("Если во время создания changeRequest'a на отмену произошла ошибка, не должно создаться никаких" +
            "changeRequest'ов ни старых ни новых")
    public void exceptionOnCancellationChangeRequestCreation() {
        Order order = createDbsOrderWithStatusAndPaymentMethod(DELIVERY, PaymentMethod.CARD_ON_DELIVERY);
        OrderEditRequest orderEditRequest = initCancellationRequest();

        Assertions.assertThrows(RuntimeException.class, () ->
                client.editOrder(order.getId(), ClientRole.UNKNOWN, order.getBuyer().getUid(), List.of(WHITE),
                        orderEditRequest));
        Order orderAfter = getActualOrderWithChangeRequests(order);

        assertThat(orderAfter.getStatus()).isEqualTo(DELIVERY);
        assertThat(orderAfter.getChangeRequests()).isNull();
        assertThat(orderAfter.getCancellationRequest()).isNull();
    }


    @Test
    @DisplayName("Облегченная модель заказа для ПИ")
    public void lightweightModelForPI() {
        var parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        var order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), USER, order.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, 0L);

        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.partials = EnumSet.of(OptionalOrderPart.CHANGE_REQUEST);
        orderSearchRequest.orderIds = List.of(order.getId());
        orderSearchRequest.rgbs = Set.of(WHITE);
        orderSearchRequest.lightweightResultForPI = true;

        PagedOrders response = client.getOrders(
                requestClientInfo,
                orderSearchRequest);

        Assertions.assertNotNull(response.getItems());
        Order orderAfter = Iterables.getFirst(response.getItems(), null);
        Assertions.assertNotNull(orderAfter);

        Assertions.assertNotNull(orderAfter.getChangeRequests());
        var changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest);
        assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus());

        Assertions.assertNotNull(orderAfter.getCancellationRequest());
        assertEquals(OrderSubstatus.USER_CHANGED_MIND, orderAfter.getCancellationRequest().getSubstatus());
    }

    @Test
    @DisplayName("Свойство доставки estimated не меняется после отмены заказа")
    public void checkEstimatedDontChangeTest() {
        Parameters parameters = WhiteParametersProvider.applyTo(WhiteParametersProvider.defaultWhiteParameters());
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        var orderBefore = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(orderBefore, OrderStatus.PROCESSING);

        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, "");
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(orderBefore.getId(), USER, orderBefore.getBuyer().getUid(),
                Collections.singletonList(WHITE), orderEditRequest);

        var orderAfter = orderService.getOrder(orderBefore.getId(), ClientInfo.SYSTEM);

        assertNotEquals(OrderStatus.CANCELLED, orderBefore.getStatus());
        assertEquals(OrderStatus.CANCELLED, orderAfter.getStatus());

        assertTrue(orderBefore.getDelivery().getEstimated());
        assertTrue(orderAfter.getDelivery().getEstimated());
    }

    private Order getActualOrderWithChangeRequests(Order order) {
        return client.getOrder(
                new RequestClientInfo(SYSTEM, order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
    }

    private OrderEditRequest initCancellationRequest() {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, ""));
        return orderEditRequest;
    }

    private Order createDbsOrderWithStatusAndPaymentMethod(OrderStatus status, PaymentMethod paymentMethod) {
        Parameters parameters = new Parameters();
        parameters.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        parameters.setColor(WHITE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(paymentMethod);
        Order order = orderCreateHelper.createOrder(parameters);
        return orderStatusHelper.proceedOrderToStatus(order, status);
    }
}
