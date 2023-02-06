package ru.yandex.market.checkout.checkouter.order.status;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ConfirmationReason;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.PRESCRIPTION_MISMATCH;

public class PrescriptionMismatchCancellationRuleTest extends AbstractPrescriptionDeliveryTestBase {

    private static final String NOTES = "";

    @Test
    @DisplayName("Корректная отмена магазином ордера с рецептуркой")
    public void cancelOrderWithPrescription() {
        Order order = createOrderWithPrescription(
                BlueParametersProvider.defaultBlueOrderParameters(), true, true); //createOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(PRESCRIPTION_MISMATCH, NOTES);
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), ClientRole.SHOP, order.getShopId(),
                Collections.singletonList(BLUE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(SHOP, (long) SHOP_ID_WITH_PMS),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        Assertions.assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        ChangeRequest changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        Assertions.assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе APPLIED");
        Assertions.assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе REJECTED");

        //Пробуем перевести ChangeRequest в статус APPLIED
        Long changeRequestId = changeRequest.getId();
        client.updateChangeRequestStatus(order.getId(),
                changeRequestId,
                SHOP,
                (long) SHOP_ID_WITH_PMS,
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.APPLIED,
                        null,
                        null));
        //Заказ должен быть отменен и ChangeRequest должен перейти в статус APPLIED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                        ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        Assertions.assertEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        Assertions.assertEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(),
                "ChangeRequest должен быть в статусе APPLIED");
    }

    @Test
    @DisplayName("Попытка отмены ордера без рецептурки")
    public void tryCancelOrderWithoutPrescription() {
        Order order = createOrderWithPrescription(
                BlueParametersProvider.defaultBlueOrderParameters(), true, false); //createOrder();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        //Создаем запрос на отмену от имени пользователя
        var orderEditRequest = new OrderEditRequest();
        var cancellationRequest = new CancellationRequest(PRESCRIPTION_MISMATCH, NOTES);
        orderEditRequest.setCancellationRequest(cancellationRequest);

        client.editOrder(order.getId(), ClientRole.SHOP, order.getShopId(),
                Collections.singletonList(BLUE), orderEditRequest);

        var orderAfter = client.getOrder(
                new RequestClientInfo(SHOP, (long) SHOP_ID_WITH_PMS),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());

        //Заказ не должен быть отменен, но ChangeRequest должен быть создан
        Assertions.assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        ChangeRequest changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен быть создан");
        Assertions.assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе APPLIED");
        Assertions.assertNotEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе REJECTED");

        //Пробуем перевести ChangeRequest в статус APPLIED
        Long changeRequestId = changeRequest.getId();
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () ->
                client.updateChangeRequestStatus(
                        order.getId(),
                        changeRequestId,
                        SHOP,
                        (long) SHOP_ID_WITH_PMS,
                        new ChangeRequestPatchRequest(ChangeRequestStatus.APPLIED, null, null)
                )
        );
        //Заказ не должен быть отменен и ChangeRequest не должен переведен в статус APPLIED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(),
                        ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        Assertions.assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        Assertions.assertNotEquals(ChangeRequestStatus.APPLIED, changeRequest.getStatus(),
                "ChangeRequest не должен быть в статусе APPLIED");

        //Пробуем перевести ChangeRequest в статус REJECTED
        client.updateChangeRequestStatus(order.getId(),
                changeRequest.getId(),
                SHOP,
                (long) SHOP_ID_WITH_PMS,
                new ChangeRequestPatchRequest(
                        ChangeRequestStatus.REJECTED,
                        null,
                        new CancellationRequestPayload(
                                PRESCRIPTION_MISMATCH,
                                null,
                                null,
                                ConfirmationReason.DELIVERY)));
        //Заказ не должен быть отменен и ChangeRequest должен переведен в статус REJECTED
        orderAfter = client.getOrder(
                new RequestClientInfo(ClientInfo.SYSTEM.getRole(), ClientInfo.SYSTEM.getId()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.CHANGE_REQUEST))
                        .build());
        Assertions.assertNotEquals(OrderStatus.CANCELLED, orderAfter.getStatus(), "Заказ не должен быть отменен");
        Assertions.assertNotNull(orderAfter.getChangeRequests());
        changeRequest = orderAfter.getChangeRequests().stream()
                .filter(ch -> ch.getType() == ChangeRequestType.CANCELLATION)
                .findAny().orElse(null);
        Assertions.assertNotNull(changeRequest, "ChangeRequest с типом CANCELLATION должен существовать");
        Assertions.assertEquals(ChangeRequestStatus.REJECTED, changeRequest.getStatus(),
                "ChangeRequest должен быть в статусе REJECTED");
    }
}
