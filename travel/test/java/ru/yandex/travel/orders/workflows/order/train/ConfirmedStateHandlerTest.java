package ru.yandex.travel.orders.workflows.order.train;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.MoneyRefund;
import ru.yandex.travel.orders.entities.MoneyRefundState;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderRefund;
import ru.yandex.travel.orders.entities.TrainOrder;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrainOrderUserRefund;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.proto.EOrderRefundState;
import ru.yandex.travel.orders.repository.MoneyRefundRepository;
import ru.yandex.travel.orders.repository.OrderRefundRepository;
import ru.yandex.travel.orders.repository.TrainTicketRefundRepository;
import ru.yandex.travel.orders.services.NotificationHelper;
import ru.yandex.travel.orders.services.finances.FinancialEventService;
import ru.yandex.travel.orders.services.orders.CheckMoneyRefundsService;
import ru.yandex.travel.orders.services.train.TrainRefundLogService;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentRefund;
import ru.yandex.travel.orders.workflow.order.proto.EInvoiceRefundType;
import ru.yandex.travel.orders.workflow.order.proto.TInvoiceRefunded;
import ru.yandex.travel.orders.workflow.order.proto.TServiceRefunded;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState;
import ru.yandex.travel.orders.workflows.WorkflowTestUtils;
import ru.yandex.travel.orders.workflows.order.train.handlers.ConfirmedStateHandler;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.workflow.BasicStateMessagingContext;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.entities.MoneyRefundState.IN_PROGRESS;
import static ru.yandex.travel.orders.entities.MoneyRefundState.PENDING;
import static ru.yandex.travel.orders.entities.MoneyRefundState.REFUNDED;
import static ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState.OS_CANCELLED;
import static ru.yandex.travel.orders.workflow.train.proto.ETrainOrderState.OS_CONFIRMED;

@SuppressWarnings("FieldCanBeLocal")
public class ConfirmedStateHandlerTest {
    private ConfirmedStateHandler handler;
    private NotificationHelper notificationHelper;
    private MoneyRefundRepository moneyRefundRepository;
    private OrderRefundRepository orderRefundRepository;
    private CheckMoneyRefundsService checkMoneyRefundsService;
    private TrainTicketRefundRepository trainTicketRefundRepository;
    private TrainRefundLogService trainRefundLogService;
    private FinancialEventService financialEventService;

    @Before
    public void init() {
        notificationHelper = mock(NotificationHelper.class);
        moneyRefundRepository = mock(MoneyRefundRepository.class);
        orderRefundRepository = mock(OrderRefundRepository.class);
        checkMoneyRefundsService = mock(CheckMoneyRefundsService.class);
        trainTicketRefundRepository = mock(TrainTicketRefundRepository.class);
        trainRefundLogService = mock(TrainRefundLogService.class);
        financialEventService = mock(FinancialEventService.class);

        handler = new ConfirmedStateHandler(notificationHelper, moneyRefundRepository, orderRefundRepository,
                checkMoneyRefundsService, trainTicketRefundRepository, trainRefundLogService, financialEventService);
    }

    @Test
    public void testSequentialMoneyRefunds() {
        TrainOrder order = testOrder();
        TrainOrderItem orderItem = (TrainOrderItem) order.getOrderItems().get(0);
        OrderRefund refund = TrainOrderUserRefund.createForOrder(order);
        when(orderRefundRepository.findById(any())).thenReturn(Optional.of(refund));

        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m1ctx = testContext(order);
        handler.handle(serviceRefunded(refund.getId()), m1ctx);
        assertThat(refunds(order)).isEqualTo(List.of(IN_PROGRESS));
        assertThat(order.getMoneyRefunds().get(0).getTargetFiscalItems())
                .isEqualTo(Map.of(1L, Money.of(0, "RUB")));
        assertThat(m1ctx.getScheduledEvents()).hasSize(1).first().satisfies(mp -> {
            assertThat(mp.getRecipientWorkflowId()).isEqualTo(order.getInvoices().get(0).getWorkflow().getId());
            assertThat(mp.getMessage()).isExactlyInstanceOf(TPaymentRefund.class);
        });
        assertThat(refund.getState()).isEqualTo(EOrderRefundState.RS_WAITING_INVOICE_REFUND);

        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m2ctx = testContext(order);
        handler.handle(serviceRefunded(refund.getId()), m2ctx);
        assertThat(refunds(order)).isEqualTo(List.of(IN_PROGRESS, PENDING));
        assertThat(m2ctx.getScheduledEvents()).hasSize(0);

        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m3ctx = testContext(order);
        handler.handle(serviceRefunded(refund.getId()), m3ctx);
        assertThat(refunds(order)).isEqualTo(List.of(IN_PROGRESS, PENDING, PENDING));
        assertThat(m3ctx.getScheduledEvents()).hasSize(0);

        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m4ctx = testContext(order);
        handler.handle(invoiceRefunded(refund.getId(), order.getInvoices().get(0).getId()), m4ctx);
        assertThat(refunds(order)).isEqualTo(List.of(REFUNDED, IN_PROGRESS, PENDING));
        assertThat(m4ctx.getScheduledEvents()).hasSize(3).last().satisfies(mp -> {
            assertThat(mp.getRecipientWorkflowId()).isEqualTo(order.getInvoices().get(0).getWorkflow().getId());
            assertThat(mp.getMessage()).isExactlyInstanceOf(TPaymentRefund.class);
        });
        assertThat(refund.getState()).isEqualTo(EOrderRefundState.RS_REFUNDED);

        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m5ctx = testContext(order);
        handler.handle(invoiceRefunded(refund.getId(), order.getInvoices().get(0).getId()), m5ctx);
        assertThat(refunds(order)).isEqualTo(List.of(REFUNDED, REFUNDED, IN_PROGRESS));
        assertThat(m5ctx.getScheduledEvents()).hasSize(3);
        assertThat(orderItem.getItemState()).isEqualTo(EOrderItemState.IS_CONFIRMED);
        assertThat(order.getState()).isEqualTo(OS_CONFIRMED);

        orderItem.setState(EOrderItemState.IS_CANCELLED);
        BasicStateMessagingContext<ETrainOrderState, TrainOrder> m6ctx = testContext(order);
        handler.handle(invoiceRefunded(refund.getId(), order.getInvoices().get(0).getId()), m6ctx);
        assertThat(refunds(order)).isEqualTo(List.of(REFUNDED, REFUNDED, REFUNDED));
        assertThat(m6ctx.getScheduledEvents()).hasSize(2);
        assertThat(order.getState()).isEqualTo(OS_CANCELLED);
    }

    private TrainOrder testOrder() {
        TrainOrder order = new TrainOrder();
        order.setState(OS_CONFIRMED);
        TrainOrderItem orderItem = new TrainOrderItem();
        orderItem.setReservation(new TrainReservation());
        orderItem.setState(EOrderItemState.IS_CONFIRMED);
        order.addOrderItem(orderItem);

        TrustInvoice invoice = new TrustInvoice();
        invoice.setId(UUID.randomUUID());
        invoice.addInvoiceItem(InvoiceItem.builder()
                .fiscalItemId(1L)
                .build());
        Workflow.createWorkflowForEntity(invoice);

        order.addInvoice(invoice);
        return order;
    }

    private BasicStateMessagingContext<ETrainOrderState, TrainOrder> testContext(TrainOrder order) {
        return WorkflowTestUtils.testMessagingContext(order);
    }

    private TServiceRefunded serviceRefunded(UUID refundId) {
        return TServiceRefunded.newBuilder()
                .putTargetFiscalItems(1, ProtoUtils.toTPrice(Money.of(0, "RUB")))
                .setOrderRefundId(refundId.toString())
                .build();
    }

    private TInvoiceRefunded invoiceRefunded(UUID refundId, UUID invoiceId) {
        return TInvoiceRefunded.newBuilder().setOrderRefundId(refundId.toString())
                .setInvoiceId(invoiceId.toString())
                .setRefundType(EInvoiceRefundType.EIR_REFUND)
                .build();
    }

    private List<MoneyRefundState> refunds(Order order) {
        return order.getMoneyRefunds().stream().map(MoneyRefund::getState).collect(Collectors.toList());
    }
}
