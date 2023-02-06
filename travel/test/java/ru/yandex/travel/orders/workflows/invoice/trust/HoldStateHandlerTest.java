package ru.yandex.travel.orders.workflows.invoice.trust;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.orders.TestOrderItem;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.AuthorizedUser;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.ResizeTrustRefund;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.repository.ResizeTrustRefundRepository;
import ru.yandex.travel.orders.services.AccountService;
import ru.yandex.travel.orders.services.payments.InvoicePaymentFlags;
import ru.yandex.travel.orders.services.payments.TrustClient;
import ru.yandex.travel.orders.services.payments.TrustClientProvider;
import ru.yandex.travel.orders.services.payments.TrustHotelsProperties;
import ru.yandex.travel.orders.services.payments.model.TrustBasketStatusResponse;
import ru.yandex.travel.orders.services.payments.model.TrustBasketStatusResponseOrder;
import ru.yandex.travel.orders.services.payments.model.TrustClearResponse;
import ru.yandex.travel.orders.services.payments.model.TrustResponseStatus;
import ru.yandex.travel.orders.services.payments.model.TrustUnholdResponse;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentClear;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentCreated;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentRefund;
import ru.yandex.travel.orders.workflows.invoice.trust.handlers.HoldStateHandler;
import ru.yandex.travel.orders.workflows.order.OrderCreateHelper;
import ru.yandex.travel.workflow.StateContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class HoldStateHandlerTest {
    private StateContext<ETrustInvoiceState, TrustInvoice> ctx;
    private TrustClientProvider trustClientProvider;
    private AccountService accountService;
    private TrustHotelsProperties trustConfig;
    private TrustClient trustClient;
    private ResizeTrustRefundRepository resizeTrustRefundRepository;

    private HoldStateHandler subject;

    @Before
    public void setUp() {
        HotelOrder order = OrderCreateHelper.createTestHotelOrder();
        order.setAccount(Account.createAccount(ProtoCurrencyUnit.RUB));
        OrderItem orderItem = new TestOrderItem();
        order.addOrderItem(orderItem);

        TrustInvoice invoice = TrustInvoice.createInvoice(order,
                AuthorizedUser.create(order.getId(), new UserCredentials(
                                "session_key", "yandex_uid", null, "test@yandex-team.ru", null, "127.0.0.1",
                                false, false),
                        AuthorizedUser.OrderUserRole.OWNER
                ), InvoicePaymentFlags.builder()
                        .force3ds(true)
                        .useMirPromo(false)
                        .processThroughYt(false)
                        .build()
        );

        ctx = testMessagingContext(invoice);
        trustClientProvider = mock(TrustClientProvider.class);
        trustClient = mock(TrustClient.class);
        when(trustClientProvider.getTrustClientForPaymentProfile(any())).thenReturn(trustClient);
        accountService = mock(AccountService.class);
        trustConfig = mock(TrustHotelsProperties.class);
        when(trustConfig.getPaymentRefreshTimeout()).thenReturn(Duration.ofSeconds(1));
        resizeTrustRefundRepository = mock(ResizeTrustRefundRepository.class);
        when(resizeTrustRefundRepository.save(any())).thenReturn(new ResizeTrustRefund());
        subject = new HoldStateHandler(trustClientProvider, accountService, trustConfig, resizeTrustRefundRepository);
    }

    @Test
    public void testUnknownMessageLeadsToUnhandledError() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> subject.handleEvent(TPaymentCreated.newBuilder().build(), ctx)
        );
    }

    @Test
    public void testFullRefundLeadsToCancelled() {
        TrustInvoice invoice = ctx.getWorkflowEntity();
        TPaymentRefund.Builder paymentRefundBuilder = TPaymentRefund.newBuilder();
        TrustBasketStatusResponse basketStatusResponse = new TrustBasketStatusResponse();
        basketStatusResponse.setOrders(new ArrayList<>());
        for (InvoiceItem invoiceItem : invoice.getInvoiceItems()) {
            invoiceItem.setTrustOrderId(String.valueOf(ThreadLocalRandom.current().nextLong()));
            paymentRefundBuilder.putTargetFiscalItems(
                    invoiceItem.getId(), ProtoUtils.toTPrice(Money.of(0, ProtoCurrencyUnit.RUB)));
            TrustBasketStatusResponseOrder orderResponse = new TrustBasketStatusResponseOrder();
            orderResponse.setOrderId(invoiceItem.getTrustOrderId());
            orderResponse.setPaidAmount(BigDecimal.ZERO);
            basketStatusResponse.getOrders().add(orderResponse);
        }
        when(trustClient.unhold(anyString(), any())).thenReturn(new TrustUnholdResponse());
        when(trustClient.getBasketStatus(any(), any())).thenReturn(basketStatusResponse);
        subject.handlePaymentRefund(paymentRefundBuilder.build(), ctx);
        assertThat(ctx.getState()).isEqualTo(ETrustInvoiceState.IS_CANCELLED);
    }

    @Test
    public void testSuccessfulClearLeadsToClearing() {
        TrustClearResponse successResponse = new TrustClearResponse();
        successResponse.setStatus(TrustResponseStatus.SUCCESS);

        when(trustClient.clear(eq(ctx.getWorkflowEntity().getPurchaseToken()), any()))
                .thenReturn(successResponse);

        subject.handleEvent(TPaymentClear.newBuilder().build(), ctx);
        assertThat(ctx.getState()).isEqualTo(ETrustInvoiceState.IS_CLEARING);
    }
}
