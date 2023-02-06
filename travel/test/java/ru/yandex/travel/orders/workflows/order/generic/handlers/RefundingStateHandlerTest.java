package ru.yandex.travel.orders.workflows.order.generic.handlers;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.GenericOrderUserRefund;
import ru.yandex.travel.orders.entities.Invoice;
import ru.yandex.travel.orders.entities.MoneyRefund;
import ru.yandex.travel.orders.entities.MoneyRefundState;
import ru.yandex.travel.orders.entities.OrderRefundPayload;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.context.OrderStateContext;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.proto.EOrderRefundState;
import ru.yandex.travel.orders.repository.MoneyRefundRepository;
import ru.yandex.travel.orders.services.NotificationHelper;
import ru.yandex.travel.orders.services.buses.BusNotificationHelper;
import ru.yandex.travel.orders.services.orders.GenericOrderMoneyRefundService;
import ru.yandex.travel.orders.services.promo.PromoCodeApplicationService;
import ru.yandex.travel.orders.services.promo.UserOrderCounterService;
import ru.yandex.travel.orders.services.train.RebookingService;
import ru.yandex.travel.orders.services.train.TrainRefundLogService;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentRefund;
import ru.yandex.travel.orders.workflow.order.generic.proto.EOrderState;
import ru.yandex.travel.orders.workflow.order.proto.TClearingInProcess;
import ru.yandex.travel.orders.workflow.order.proto.TInvoiceCleared;
import ru.yandex.travel.orders.workflows.order.generic.GenericWorkflowService;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class RefundingStateHandlerTest {
    private final MoneyRefundRepository moneyRefundRepository = mock(MoneyRefundRepository.class);
    private final GenericOrderMoneyRefundService moneyRefundService = new GenericOrderMoneyRefundService();
    private final NotificationHelper notificationHelper = mock(NotificationHelper.class);
    private final BusNotificationHelper busNotificationHelper = mock(BusNotificationHelper.class);
    private final TrainRefundLogService trainRefundLogService = mock(TrainRefundLogService.class);
    private final StarTrekService starTrekService = mock(StarTrekService.class);

    private final RefundingStateHandler handler = new RefundingStateHandler(
            moneyRefundRepository,
            moneyRefundService,
            notificationHelper,
            busNotificationHelper,
            trainRefundLogService,
            starTrekService,
            new GenericWorkflowService(Mockito.mock(PromoCodeApplicationService.class),
                    Mockito.mock(RebookingService.class),
                    notificationHelper,
                    busNotificationHelper,
                    Clock.systemDefaultZone()),
            mock(UserOrderCounterService.class)
    );

    @Test
    public void clearingInProcessTest() {
        GenericOrder order = createTrainOrder();
        var moneyRefund = new MoneyRefund();
        moneyRefund.setState(MoneyRefundState.IN_PROGRESS);
        moneyRefund.setOrderRefundId(order.getOrderRefunds().get(0).getId());
        moneyRefund.setTargetFiscalItems(Map.of(123L, Money.of(100, ProtoCurrencyUnit.RUB)));
        order.addMoneyRefund(moneyRefund);

        StateContext<EOrderState, GenericOrder> ctx = testMessagingContext(order);
        handler.handle(TClearingInProcess.getDefaultInstance(), ctx);
        assertThat(moneyRefund.getState()).isEqualTo(MoneyRefundState.WAITING_CLEARING);

        ctx = testMessagingContext(order);
        handler.handle(TInvoiceCleared.getDefaultInstance(), ctx);
        assertThat(moneyRefund.getState()).isEqualTo(MoneyRefundState.IN_PROGRESS);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(1);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TPaymentRefund.class);
    }

    private GenericOrder createTrainOrder() {
        GenericOrder order = new GenericOrder();
        order.setWorkflow(createWorkflow());
        order.setId(UUID.randomUUID());
        order.setState(EOrderState.OS_REFUNDING);
        var factory = new TrainOrderItemFactory();
        order.addOrderItem(factory.createTrainOrderItem());
        OrderStateContext stateContext = new OrderStateContext();
        stateContext.init(EDisplayOrderType.DT_TRAIN);
        order.setStateContext(stateContext);
        var refund = new GenericOrderUserRefund();
        refund.setId(UUID.randomUUID());
        refund.setState(EOrderRefundState.RS_WAITING_INVOICE_REFUND);
        refund.setPayload(new OrderRefundPayload());
        order.addOrderRefund(refund);
        Invoice invoice = new TrustInvoice();
        order.setCurrentInvoice(invoice);
        invoice.setId(UUID.randomUUID());
        invoice.setOrder(order);
        invoice.setWorkflow(createWorkflow());
        return order;
    }

    private Workflow createWorkflow() {
        var workflow = new Workflow();
        workflow.setId(UUID.randomUUID());
        workflow.setState(EWorkflowState.WS_RUNNING);
        return workflow;
    }
}
