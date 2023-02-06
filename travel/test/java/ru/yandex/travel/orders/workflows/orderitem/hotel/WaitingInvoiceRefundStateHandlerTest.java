package ru.yandex.travel.orders.workflows.orderitem.hotel;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.TPrice;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.MoneyRefund;
import ru.yandex.travel.orders.entities.MoneyRefundState;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.services.NotificationHelper;
import ru.yandex.travel.orders.services.payments.InvoicePaymentFlags;
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TScheduleClearing;
import ru.yandex.travel.orders.workflow.order.proto.EInvoiceRefundType;
import ru.yandex.travel.orders.workflow.order.proto.TInvoiceNotRefunded;
import ru.yandex.travel.orders.workflow.order.proto.TInvoiceRefunded;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;
import ru.yandex.travel.orders.workflows.order.hotel.handlers.WaitingInvoiceRefundStateHandler;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class WaitingInvoiceRefundStateHandlerTest {

    private StateContext<EHotelOrderState, HotelOrder> ctx;

    private WaitingInvoiceRefundStateHandler subject;

    private TrustInvoice trustInvoice;

    private NotificationHelper notificationHelper;

    @Before
    public void setUp() {
        HotelOrder order = OrderCreateHelper.createTestHotelOrder();
        ExpediaOrderItem item = new ExpediaOrderItem();
        item.setId(UUID.randomUUID());
        item.setItinerary(new ExpediaHotelItinerary());
        item.getItinerary().setOrderDetails(OrderDetails.builder()
                .checkinDate(LocalDate.now())
                .checkoutDate(LocalDate.now().plusDays(1))
                .build());
        item.addFiscalItem(FiscalItem.builder()
                .id(0L)
                .type(FiscalItemType.EXPEDIA_HOTEL)
                .moneyAmount(Money.of(1000, ProtoCurrencyUnit.RUB))
                .build());
        order.addOrderItem(item);
        Workflow.createWorkflowForEntity(order);

        var owner = AuthorizedUser.createGuest(order.getId(), "test-key", "test-yuid",
                AuthorizedUser.OrderUserRole.OWNER);

        trustInvoice = TrustInvoice.createInvoice(order, owner, InvoicePaymentFlags.builder()
                .force3ds(true)
                .useMirPromo(false)
                .processThroughYt(false)
                .build());
        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setPrice(BigDecimal.valueOf(1000));
        invoiceItem.setFiscalItemId(0L);
        trustInvoice.addInvoiceItem(invoiceItem);
        trustInvoice.setNextCheckStatusAt(Instant.now().plus(Duration.ofMinutes(1)));
        trustInvoice.setState(ETrustInvoiceState.IS_HOLD);
        trustInvoice.setPurchaseToken("some_purchase_token");
        order.setInvoices(List.of(trustInvoice));
        MoneyRefund refund = MoneyRefund.createPendingRefundFromProto(order,
                Map.of(0L,
                        TPrice.newBuilder()
                                .setAmount(10000)
                                .setPrecision(2)
                                .setCurrency(ECurrency.C_RUB)
                                .build()
                ), null, null, "test");
        refund.setState(MoneyRefundState.IN_PROGRESS);

        Workflow.createWorkflowForEntity(trustInvoice);

        ctx = testMessagingContext(order);
        notificationHelper = mock(NotificationHelper.class);
        when(notificationHelper.createWorkflowForRefundHotelNotification(any())).thenReturn(UUID.randomUUID());
        subject = new WaitingInvoiceRefundStateHandler(notificationHelper);
    }

    @Test
    public void testInvoiceNotRefundedLeadsToRuntimeException() {
        Assertions.assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                () -> subject.handleEvent(TInvoiceNotRefunded.newBuilder().build(), ctx)
        );
    }

    @Test
    public void testInvoiceClearEventSentAfterInvoiceRefunded() {
        subject.handleEvent(TInvoiceRefunded.newBuilder().setInvoiceId(trustInvoice.getId().toString()).setRefundType(EInvoiceRefundType.EIR_RESIZE).build(), ctx);
        Assertions.assertThat(ctx.getScheduledEvents()).filteredOn(wp -> wp.getRecipientWorkflowId().equals(trustInvoice.getWorkflow().getId()))
                .filteredOn(wp -> wp.getMessage() instanceof TScheduleClearing).hasSize(1);
    }
}
