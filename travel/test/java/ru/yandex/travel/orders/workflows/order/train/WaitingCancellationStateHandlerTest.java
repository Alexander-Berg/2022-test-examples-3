package ru.yandex.travel.orders.workflows.order.train;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentRefund;
import ru.yandex.travel.orders.workflow.order.proto.TMoneyAcquired;
import ru.yandex.travel.orders.workflow.order.proto.TServiceCancelled;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState;
import ru.yandex.travel.orders.workflows.order.train.handlers.WaitingCancellationStateHandler;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class WaitingCancellationStateHandlerTest {

    private WaitingCancellationStateHandler handler;

    @Before
    public void setUp() {
        handler = new WaitingCancellationStateHandler();
    }

    @Test
    public void testServiceCancelledWithHoldedInvoice() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_CANCELLATION);
        ((TrainOrderItem) trainOrder.getOrderItems().get(0)).setState(EOrderItemState.IS_CANCELLED);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_HOLD);

        var ctx = testMessagingContext(trainOrder);

        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);
        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_WAITING_CANCELLATION);

        handler.handleEvent(TServiceCancelled.newBuilder().build(), ctx);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPaymentRefund.class);
        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
    }

    @Test
    public void testServiceCancelledWithWaitingPaymentInvoice() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_CANCELLATION);
        ((TrainOrderItem) trainOrder.getOrderItems().get(0)).setState(EOrderItemState.IS_CANCELLED);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_WAIT_FOR_PAYMENT);

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TServiceCancelled.newBuilder().build(), ctx);

        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
    }

    @Test
    public void testServiceCancelledWithInactiveInvoice() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_CANCELLATION);
        ((TrainOrderItem) trainOrder.getOrderItems().get(0)).setState(EOrderItemState.IS_CANCELLED);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_PAYMENT_NOT_AUTHORIZED);

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TServiceCancelled.newBuilder().build(), ctx);

        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_CANCELLED);
    }

    private TrainOrder createTrainOrder() {
        var factory = new TrainOrderItemFactory();
        var trainOrderItem = factory.createTrainOrderItem();

        var invoice = new TrustInvoice();
        invoice.setId(UUID.randomUUID());
        Workflow.createWorkflowForEntity(invoice);

        var trainOrder = new TrainOrder();
        trainOrder.addOrderItem(trainOrderItem);
        trainOrder.addInvoice(invoice);

        return trainOrder;
    }
}
