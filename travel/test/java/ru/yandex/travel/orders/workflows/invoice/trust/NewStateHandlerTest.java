package ru.yandex.travel.orders.workflows.invoice.trust;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.commons.proto.EDisplayOrderType;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.VatType;
import ru.yandex.travel.orders.services.payments.TrustClient;
import ru.yandex.travel.orders.services.payments.TrustClientProvider;
import ru.yandex.travel.orders.services.payments.TrustHotelsProperties;
import ru.yandex.travel.orders.services.payments.TrustXpayProperties;
import ru.yandex.travel.orders.services.payments.model.TrustCreateBasketResponse;
import ru.yandex.travel.orders.services.payments.model.TrustCreateOrderResponse;
import ru.yandex.travel.orders.services.payments.model.TrustStartPaymentResponse;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentClear;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentCreated;
import ru.yandex.travel.orders.workflows.invoice.trust.handlers.NewStateHandler;
import ru.yandex.travel.workflow.StateContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

@SuppressWarnings("FieldCanBeLocal")
public class NewStateHandlerTest {

    private StateContext<ETrustInvoiceState, TrustInvoice> ctx;
    private TrustClient trustClient;
    private TrustClientProvider trustClientProvider;

    private TrustInvoice invoice;
    private InvoiceItem invoiceItem;

    private NewStateHandler subject;

    @Before
    public void setUp() {
        invoice = TrustInvoice.createEmptyInvoice();
        invoiceItem = new InvoiceItem();
        invoiceItem.setFiscalItemType(FiscalItemType.EXPEDIA_HOTEL);
        invoiceItem.setFiscalNds(VatType.VAT_NONE);
        invoiceItem.setPrice(BigDecimal.valueOf(1));
        invoice.addInvoiceItem(invoiceItem);
        invoice.setExpirationDate(Instant.now().plus(Duration.ofSeconds(10)));
        invoice.setClientPhone("+79111111111");
        Order order = new HotelOrder();
        order.setCurrency(ProtoCurrencyUnit.RUB);
        invoice.setSource("desktop");
        invoice.setOrder(order);

        ctx = testMessagingContext(invoice);
        trustClient = mock(TrustClient.class);
        trustClientProvider = mock(TrustClientProvider.class);
        when(trustClientProvider.getTrustClientForPaymentProfile(any())).thenReturn(trustClient);
        TrustHotelsProperties trustProperties = TrustHotelsProperties.builder()
                .defaultServiceSettings(EDisplayOrderType.DT_UNKNOWN)
                .serviceSettings(Map.of("ignored", TrustHotelsProperties.ServiceSettings.builder()
                        .orderType(EDisplayOrderType.DT_UNKNOWN)
                        .build()))
                .build();
        subject = new NewStateHandler(trustProperties, new TrustXpayProperties(),
                Clock.system(ZoneId.systemDefault()), trustClientProvider, null);
    }

    @Test
    public void testUnknownMessageLeadsToUnhandledError() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
                () -> subject.handleEvent(TPaymentClear.newBuilder().build(), ctx)
        );
    }

    @Test
    public void testCorrectPaymentResponseLeadsToPaymentWaiting() {
        String trustOrderId = "some_id";
        String purchaseToken = "purchase_token";
//        PaymentRegisterItemResponse registerItemResponse = PaymentRegisterItemResponse.builder()
//                .trustOrderId(trustOrderId)
//                .build();
        TrustCreateOrderResponse response = new TrustCreateOrderResponse();
        response.setOrderId(trustOrderId);

        when(trustClient.createOrder(any(), any())).thenReturn(response);

        TrustCreateBasketResponse trustCreateBasketResponse = new TrustCreateBasketResponse();
        trustCreateBasketResponse.setPurchaseToken(purchaseToken);
        when(trustClient.createBasket(any(), any(), any())).thenReturn(trustCreateBasketResponse);

        TrustStartPaymentResponse trustStartPaymentResponse = new TrustStartPaymentResponse();
        when(trustClient.startPayment(any(), any())).thenReturn(trustStartPaymentResponse);

        subject.handleEvent(TPaymentCreated.newBuilder().build(), ctx);
        assertThat(ctx.getWorkflowEntity().getInvoiceState()).isEqualTo(ETrustInvoiceState.IS_WAIT_FOR_PAYMENT);
        assertThat(invoiceItem.getTrustOrderId()).isEqualTo(trustOrderId);
    }

    @Test
    public void testIncorrectPaymentResponseLeadsToUnhandledError() {
        CompletableFuture<TrustCreateOrderResponse> responseFuture = new CompletableFuture<>();
        responseFuture.completeExceptionally(new RuntimeException("Test exception"));
        when(trustClient.createOrder(any(), any())).thenThrow(new RuntimeException("Test exception"));

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                () -> subject.handleEvent(TPaymentCreated.newBuilder().build(), ctx)
        );
    }
}
