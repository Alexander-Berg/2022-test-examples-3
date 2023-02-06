package ru.yandex.travel.orders.workflows.order.train;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentRefund;
import ru.yandex.travel.orders.workflow.order.proto.TInvoiceRefunded;
import ru.yandex.travel.orders.workflow.order.proto.TMoneyAcquireErrorOccurred;
import ru.yandex.travel.orders.workflow.order.proto.TMoneyAcquired;
import ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState;
import ru.yandex.travel.orders.workflows.order.train.handlers.WaitingRefundAfterCancellationStateHandler;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class WaitingRefundAfterCancellationStateHandlerTest {

    private WaitingRefundAfterCancellationStateHandler handler;

    @Before
    public void setUp() {
        handler = new WaitingRefundAfterCancellationStateHandler();
    }

    @Test
    public void testMoneyAcquired() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_HOLD);
        trainOrder.getInvoices().get(0).getInvoiceItems().get(0).setPrice(BigDecimal.valueOf(10));

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TMoneyAcquired.newBuilder().build(), ctx);

        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPaymentRefund.class);
        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
    }

    @Test
    public void testMoneyAcquireErrorOccurred() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_PAYMENT_NOT_AUTHORIZED);

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TMoneyAcquireErrorOccurred.newBuilder().build(), ctx);

        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_CANCELLED);
    }

    @Test
    public void testInvoiceRefunded() {
        TrainOrder trainOrder = createTrainOrder();
        trainOrder.setState(ETrainOrderState.OS_WAITING_REFUND_AFTER_CANCELLATION);
        ((TrustInvoice) trainOrder.getInvoices().get(0)).setState(ETrustInvoiceState.IS_CANCELLED);
        trainOrder.getInvoices().get(0).getInvoiceItems().get(0).setPrice(BigDecimal.ZERO);

        var ctx = testMessagingContext(trainOrder);
        handler.handleEvent(TInvoiceRefunded.newBuilder().build(), ctx);

        assertThat(trainOrder.getState()).isEqualByComparingTo(ETrainOrderState.OS_CANCELLED);
    }

    private TrainOrder createTrainOrder() {
        var factory = new TrainOrderItemFactory();
        var trainOrderItem = factory.createTrainOrderItem();

        var invoice = new TrustInvoice();
        var invoiceItem = new InvoiceItem();
        var account = new Account();
        invoice.addInvoiceItem(invoiceItem);
        invoice.setId(UUID.randomUUID());
        invoice.setAccount(account);
        invoiceItem.setFiscalItemId(1L);
        account.setCurrency(ProtoCurrencyUnit.EUR);
        Workflow.createWorkflowForEntity(invoice);

        var trainOrder = new TrainOrder();
        trainOrder.addOrderItem(trainOrderItem);
        trainOrder.addInvoice(invoice);

        return trainOrder;
    }
}
