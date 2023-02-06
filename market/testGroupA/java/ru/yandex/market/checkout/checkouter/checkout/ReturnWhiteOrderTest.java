package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.pay.ReturnableItemsService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.IllegalReturnStatusException;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnableItemsResponse;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReturnWhiteOrderTest extends AbstractReturnTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReturnableItemsService returnableItemsService;

    @Test
    void shouldCreateReturn() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, order.getPaymentMethod());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());

        trustMockConfigurer.mockWholeTrust();
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return ret = returnHelper.initReturn(order.getId(), request);

        ret = returnHelper.resumeReturn(order.getId(), ret.getId(), ret);

        returnHelper.processReturnPayments(order, ret);

        List<Payment> payments = paymentService.getReturnPayments(ret);

        BigDecimal totalAmount = order.getItemsTotal().add(request.getUserCompensationSum());
        for (Payment payment : payments) {
            assertEquals(totalAmount, payment.getTotalAmount());
        }
        assertThat(payments.stream().map(Payment::getType).collect(Collectors.toList()),
                containsInAnyOrder(PaymentGoal.USER_COMPENSATION, PaymentGoal.MARKET_COMPENSATION));
    }

    @Test
    void shouldThrowErrorOnSecondResumeReturn() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, order.getPaymentMethod());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());

        trustMockConfigurer.mockWholeTrust();
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return ret = returnHelper.initReturn(order.getId(), request);

        ret = returnHelper.resumeReturn(order.getId(), ret.getId(), ret);
        Order finalOrder = order;
        Return finalRet = ret;
        assertThrows(
                IllegalReturnStatusException.class,
                () -> returnHelper.resumeReturn(finalOrder.getId(), finalRet.getId(), finalRet)
        );
    }


    @Test
    void shouldReturnAllItemsForReferee() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, order.getPaymentMethod());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        ClientInfo clientInfo = new ClientInfo(ClientRole.REFEREE, 1L);
        setFixedTime(Instant.now().plus(21, ChronoUnit.DAYS));

        ReturnableItemsResponse returnableItems = returnableItemsService.getReturnableItems(order.getId(), clientInfo);

        assertTrue(returnableItems.getNonReturnableItems().isEmpty());

        trustMockConfigurer.mockWholeTrust();
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        request.setItems(returnableItems.getReturnableItems().stream().map(this::toReturnItem)
                .collect(Collectors.toList()));
        request.getItems().get(0).setSupplierCompensation(request.getUserCompensationSum());
        Return ret = returnHelper.initReturn(order.getId(), request);

        ret = returnHelper.resumeReturn(order.getId(), ret.getId(), ret);

        returnHelper.processReturnPayments(order, ret);

        List<Payment> payments = paymentService.getReturnPayments(ret);

        BigDecimal totalAmount = order.getItemsTotal().add(request.getUserCompensationSum());
        for (Payment payment : payments) {
            assertEquals(totalAmount, payment.getTotalAmount());
        }
        assertThat(payments.stream().map(Payment::getType).collect(Collectors.toList()),
                containsInAnyOrder(PaymentGoal.USER_COMPENSATION, PaymentGoal.MARKET_COMPENSATION));
    }

    @Test
    void shouldReturnAllItemsForSystem() {
        Parameters whiteParameters = WhiteParametersProvider.defaultWhiteParameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        assertEquals(PaymentMethod.CARD_ON_DELIVERY, order.getPaymentMethod());

        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        queuedCallService.executeQueuedCallBatch(CheckouterQCType.ORDER_CREATE_CASH_PAYMENT);

        order = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        ClientInfo clientInfo = new ClientInfo(ClientRole.SYSTEM, 1L);
        setFixedTime(Instant.now().plus(21, ChronoUnit.DAYS));

        ReturnableItemsResponse returnableItems = returnableItemsService.getReturnableItems(order.getId(), clientInfo);

        assertTrue(returnableItems.getNonReturnableItems().isEmpty());

        trustMockConfigurer.mockWholeTrust();
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        request.setItems(returnableItems.getReturnableItems().stream().map(this::toReturnItem)
                .collect(Collectors.toList()));
        request.getItems().get(0).setSupplierCompensation(request.getUserCompensationSum());
        Return ret = returnHelper.initReturn(order.getId(), request);

        ret = returnHelper.resumeReturn(order.getId(), ret.getId(), ret);

        returnHelper.processReturnPayments(order, ret);

        List<Payment> payments = paymentService.getReturnPayments(ret);

        BigDecimal totalAmount = order.getItemsTotal().add(request.getUserCompensationSum());
        for (Payment payment : payments) {
            assertEquals(totalAmount, payment.getTotalAmount());
        }
        assertThat(payments.stream().map(Payment::getType).collect(Collectors.toList()),
                containsInAnyOrder(PaymentGoal.USER_COMPENSATION, PaymentGoal.MARKET_COMPENSATION));
    }
}
