package ru.yandex.travel.orders.workflows.invoice.trust;

import java.time.Duration;
import java.time.Instant;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.orders.TestOrderItem;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.services.AccountService;
import ru.yandex.travel.orders.services.payments.InvoicePaymentFlags;
import ru.yandex.travel.orders.services.payments.TrustClient;
import ru.yandex.travel.orders.services.payments.TrustClientProvider;
import ru.yandex.travel.orders.services.payments.model.PaymentStatusEnum;
import ru.yandex.travel.orders.services.payments.model.TrustBasketStatusResponse;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentCreated;
import ru.yandex.travel.orders.workflow.invoice.proto.TTrustInvoiceCallbackReceived;
import ru.yandex.travel.orders.workflow.invoice.proto.TTrustPaymentAuthorized;
import ru.yandex.travel.orders.workflow.invoice.proto.TTrustPaymentNotAuthorized;
import ru.yandex.travel.orders.workflow.invoice.proto.TTrustPaymentStatusChanged;
import ru.yandex.travel.orders.workflows.invoice.trust.handlers.WaitForPaymentStateHandler;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;
import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class WaitForPaymentStateHandlerTest {
    private StateContext<ETrustInvoiceState, TrustInvoice> ctx;
    private AccountService accountService;
    private TrustClientProvider trustClientProvider;
    private TrustClient trustClient;

    private WaitForPaymentStateHandler subject;

    @Before
    public void setUp() {
        HotelOrder order = OrderCreateHelper.createTestHotelOrder();
        Workflow.createWorkflowForEntity(order);
        order.setAccount(Account.createAccount(ProtoCurrencyUnit.RUB));
        OrderItem orderItem = new TestOrderItem();
        FiscalItem fiscalItem = new FiscalItem();
        fiscalItem.setMoneyAmount(Money.of(1000L, ProtoCurrencyUnit.RUB));
        orderItem.addFiscalItem(fiscalItem);
        order.addOrderItem(orderItem);
        var owner = AuthorizedUser.createGuest(order.getId(), "test-key", "test-uid",
                AuthorizedUser.OrderUserRole.OWNER);
        TrustInvoice invoice = TrustInvoice.createInvoice(order, owner, InvoicePaymentFlags.builder()
                .force3ds(true)
                .useMirPromo(false)
                .processThroughYt(false)
                .build());
        invoice.setNextCheckStatusAt(Instant.now().minus(Duration.ofMinutes(1)));
        invoice.setState(ETrustInvoiceState.IS_WAIT_FOR_PAYMENT);
        invoice.setPurchaseToken("some_purchase_token");

        ctx = testMessagingContext(invoice);
        accountService = mock(AccountService.class);
        trustClientProvider = mock(TrustClientProvider.class);
        trustClient = mock(TrustClient.class);
        when(trustClientProvider.getTrustClientForPaymentProfile(any())).thenReturn(trustClient);
        subject = new WaitForPaymentStateHandler(accountService, trustClientProvider);
    }

    @Test
    public void testUnknownMessageLeadsToUnhandledError() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> subject.handleEvent(TPaymentCreated.newBuilder().build(), ctx)
        );
    }

    @Test
    public void testNotAuthorizedLeadsToPaymentNotAuthorized() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.NOT_AUTHORIZED);
        subject.handleEvent(TTrustPaymentNotAuthorized.newBuilder()
                .setBasketStatus(ProtoUtils.toTJson(basketStatus))
                .build(), ctx);
        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_PAYMENT_NOT_AUTHORIZED);
    }

    @Test
    public void testAuthorizedLeadsToPaymentHold() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.AUTHORIZED);
        subject.handleEvent(TTrustPaymentAuthorized.newBuilder()
                .setBasketStatus(ProtoUtils.toTJson(basketStatus))
                .build(), ctx);
        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_HOLD);
    }

    @Test
    public void testNotAuthorizedStateLeadsToPaymentNotAuthorized() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.NOT_AUTHORIZED);
        subject.handleEvent(TTrustPaymentStatusChanged.newBuilder()
                .setBasketStatus(ProtoUtils.toTJson(basketStatus))
                .build(), ctx);
        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_PAYMENT_NOT_AUTHORIZED);
    }

    @Test
    public void testAuthorizedStateLeadsToPaymentHold() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.AUTHORIZED);
        subject.handleEvent(TTrustPaymentStatusChanged.newBuilder()
                .setBasketStatus(ProtoUtils.toTJson(basketStatus))
                .build(), ctx);
        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_HOLD);
    }

    @Test
    public void testSuccessCallbackLeadsToPaymentHold() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.AUTHORIZED);

        when(trustClient.getBasketStatus(any(), any())).thenReturn(basketStatus);
        subject.handleEvent(TTrustInvoiceCallbackReceived.newBuilder()
                .setStatus("success")
                .build(), ctx);

        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_HOLD);
    }

    @Test
    public void testCancelledCallbackLeadsToPaymentNotAuthorized() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.NOT_AUTHORIZED);

        when(trustClient.getBasketStatus(any(), any())).thenReturn(basketStatus);
        subject.handleEvent(TTrustInvoiceCallbackReceived.newBuilder()
                .setStatus("cancelled")
                .build(), ctx);

        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_PAYMENT_NOT_AUTHORIZED);
    }

    @Test
    public void testWrongStatusCallbackLeadsToNoAction() {
        TrustBasketStatusResponse basketStatus = new TrustBasketStatusResponse();
        basketStatus.setPaymentStatus(PaymentStatusEnum.NOT_AUTHORIZED);

        when(trustClient.getBasketStatus(any(), any())).thenReturn(basketStatus);
        subject.handleEvent(TTrustInvoiceCallbackReceived.newBuilder()
                .setStatus("success")
                .build(), ctx);

        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_WAIT_FOR_PAYMENT);
    }
}
