package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.tasks.v2.factory.ProcessReturnPaymentsPartitionTaskV2Factory;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.ClientHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_RETURN_STATUS_UPDATED;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.SBER_ID;

class ProcessReturnPaymentsTaskV2Test extends AbstractReturnTestBase {
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ProcessReturnPaymentsPartitionTaskV2Factory processReturnPaymentsPartitionTaskV2Factory;

    private Order order;

    @Test
    public void failsReturnIfRefundedItemsNotRefundable() {
        Parameters params = defaultBlueOrderParameters();
        params.getOrder().getItems().forEach(item -> {
            item.setCount(10);
            item.setQuantity(BigDecimal.TEN);
        });
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return request1 = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp1 = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, 3331L, request1);
        request1 = ReturnHelper.copy(returnResp1);
        client.returns().resumeReturn(order.getId(),
                returnResp1.getId(),
                ClientRole.REFEREE,
                ClientHelper.REFEREE_UID,
                request1);

        assertThrows(ErrorCodeException.class, () -> {
            Return request2 = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
            Return returnResp2 = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, Long.MAX_VALUE,
                    request2);
            request2 = ReturnHelper.copy(returnResp2);
            client.returns().resumeReturn(order.getId(),
                    returnResp2.getId(),
                    ClientRole.REFEREE,
                    ClientHelper.REFEREE_UID,
                    request2);
        });

        processReturnPaymentsPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        Collection<Return> returns = getReturnsByOrderId(order.getId());
        assertThat(returns, hasSize(1));

        Return normalReturn = getReturnByStatus(returns, ReturnStatus.REFUND_IN_PROGRESS);

        // Order event generated
        Collection<OrderHistoryEvent> events = getOrderEvents(order.getId());
        assertThat(events.size(), greaterThan(0));
        assertThat(events, hasItem(hasProperty("returnId", equalTo(normalReturn.getId()))));
    }

    @Test
    public void compensationCreationWithSberIdShouldWork() {
        Parameters params = defaultBlueOrderParameters(BuyerProvider.getSberIdBuyer());
        params.getOrder().getItems().forEach(item -> {
            item.setCount(10);
            item.setQuantity(BigDecimal.TEN);
        });
        order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        Return request = prepareDefaultReturnRequest(order, DeliveryType.PICKUP);
        Return returnResp1 = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, SBER_ID, request);
        request = ReturnHelper.copy(returnResp1);
        client.returns().resumeReturn(order.getId(),
                returnResp1.getId(),
                ClientRole.REFEREE,
                ClientHelper.REFEREE_UID,
                request);

        processReturnPaymentsPartitionTaskV2Factory.getTasks().forEach((key, value) -> {
            var anotherResult = value.run(TaskRunType.ONCE);
            Assertions.assertEquals(TaskStageType.SUCCESS, anotherResult.getStage(), anotherResult.toString());
        });

        Collection<Return> returns = getReturnsByOrderId(order.getId());
        assertThat(returns, hasSize(1));

        List<Payment> compensationPayments = paymentService.getPayments(order.getId(),
                ClientInfo.SYSTEM,
                PaymentGoal.MARKET_COMPENSATION,
                PaymentGoal.USER_COMPENSATION);
        assertEquals(2, compensationPayments.size());
        compensationPayments.forEach(p -> assertNull(p.getUid()));
    }

    private Collection<OrderHistoryEvent> getOrderEvents(long orderId) {
        return eventService.getPagedOrderHistoryEvents(
                orderId,
                Pager.atPage(1, 100),
                null, null,
                Collections.singleton(ORDER_RETURN_STATUS_UPDATED),
                false,
                ClientInfo.SYSTEM,
                null).getItems();
    }

    @Nonnull
    private Return getReturnByStatus(Collection<Return> returns, ReturnStatus refundInProgress) {
        return returns.stream().filter(r -> r.getStatus() == refundInProgress).findAny().orElseThrow(() -> new
                RuntimeException("Return not found!"));
    }

    private Collection<Return> getReturnsByOrderId(long orderId) {
        return returnService.findReturns(
                orderId,
                null,
                ClientInfo.SYSTEM,
                Pager.atPage(1, 100)
        ).getItems();
    }

}
