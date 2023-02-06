package ru.yandex.chemodan.app.psbilling.core.mocks;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import lombok.Getter;
import org.joda.time.Instant;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingTrustConfiguration;
import ru.yandex.chemodan.app.psbilling.core.entities.InappStore;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.trust.client.TrustClient;
import ru.yandex.chemodan.trust.client.responses.AppleReceiptResponse;
import ru.yandex.chemodan.trust.client.responses.InappSubscription;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionResponse;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.chemodan.trust.client.responses.PaymentResponse;
import ru.yandex.chemodan.trust.client.responses.PaymentStatus;
import ru.yandex.chemodan.trust.client.responses.ProcessInappReceiptResponse;
import ru.yandex.chemodan.trust.client.responses.ReceiptSubscriptionItem;
import ru.yandex.chemodan.trust.client.responses.RefundResponse;
import ru.yandex.chemodan.trust.client.responses.SubscriptionResponse;
import ru.yandex.chemodan.trust.client.responses.TrustRefundStatus;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;
import ru.yandex.inside.passport.PassportUid;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.chemodan.trust.client.responses.ReceiptSubscriptionItem.SYNC_STATUS_SUCCESS;

@Configuration
@Import(PsBillingTrustConfiguration.class)
public class TrustClientMockConfiguration {
    @Autowired
    private PsBillingTrustConfiguration psBillingTrustConfiguration;

    @Getter
    private TrustClient mock;

    @Bean
    @Primary
    public TrustClient trustClient(@Value("${trust.url}") String trustUrl,
                                   @PsBillingTrustConfiguration.Trust HttpClientConfigurator trustHttpClientConfigurator, List<PsBillingTrustConfiguration.TrustServiceDefinition> services) {
        TrustClient trustClient = psBillingTrustConfiguration.trustClient(
                trustUrl, trustHttpClientConfigurator, services);
        this.mock = Mockito.mock(TrustClient.class,
                Mockito.withSettings().spiedInstance(trustClient));
        return this.mock;
    }

    public ProcessInappReceiptResponse mockProcessInappReceipt(
            PassportUid uid, InappStore store, String productId,
            String trustOrderId,
            Function<InappSubscription.InappSubscriptionBuilder, InappSubscription.InappSubscriptionBuilder> customizer) {
        ReceiptSubscriptionItem subscriptionItem = new ReceiptSubscriptionItem();
        subscriptionItem.setSyncStatus(SYNC_STATUS_SUCCESS);
        subscriptionItem.setSubscription(createInappSubscription(uid, store, productId, trustOrderId, customizer));

        ProcessInappReceiptResponse response = new ProcessInappReceiptResponse();
        response.setItems(Cf.list(subscriptionItem));
        doAnswer(x -> response).when(mock).processInappReceipt(Mockito.any());
        return response;
    }

    public void mockCheckAppstoreReceipt(AppleReceiptResponse response) {
        doAnswer(x -> response).when(mock).checkAppstoreReceipt(Mockito.any(), Mockito.any());
    }

    public InappSubscriptionResponse mockGetInappSubscription(PassportUid uid, InappStore store, String productId,
                                                              String trustOrderId) {
        InappSubscriptionResponse response = new InappSubscriptionResponse(
                createInappSubscription(uid, store, productId, trustOrderId, x -> x));
        doAnswer(x -> response).when(mock).getInappSubscription(Mockito.any());
        return response;
    }

    public InappSubscriptionResponse mockGetInappSubscription(Function<InappSubscription.InappSubscriptionBuilder,
            InappSubscription.InappSubscriptionBuilder> customizer) {
        InappSubscriptionResponse response = new InappSubscriptionResponse(createInappSubscription(customizer));
        doAnswer(x -> response).when(mock).getInappSubscription(Mockito.any());
        return response;
    }

    public SubscriptionResponse mockGetSubscriptionPaid() {
        String lastPaymentPurchaseToken = UUID.randomUUID().toString();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .subscriptionUntil(DateUtils.futureDate())
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(1)
                .subscriptionState(3)
                .currentAmount(new String[][]{{"10", "RUB"}})
                .build();

        Mockito.when(mock.getSubscription(any()))
                .thenAnswer(invocation -> response);
        return response;
    }

    public SubscriptionResponse mockGetSubscriptionTrial() {
        String lastPaymentPurchaseToken = UUID.randomUUID().toString();
        SubscriptionResponse response = SubscriptionResponse.builder()
                .subscriptionUntil(DateUtils.futureDate())
                .paymentIds(Cf.list(lastPaymentPurchaseToken))
                .subscriptionPeriodCount(1)
                .subscriptionState(0)
                .currentAmount(new String[][]{{"0", "RUB"}})
                .build();

        Mockito.when(mock.getSubscription(any()))
                .thenAnswer(invocation -> response);
        return response;
    }

    public void mockPaymentOk(String purchaseToken) {
        Mockito.when(mock.getPayment(any()))
                .thenAnswer(invocation ->
                        PaymentResponse.builder()
                                .purchaseToken(purchaseToken)
                                .paymentStatus(PaymentStatus.cleared)
                                .build());

    }

    public void mockRefund(String refundId, TrustRefundStatus status) {
        RefundResponse refundResponse = new RefundResponse(refundId);
        refundResponse.setStatus(status.toString());
        Mockito.when(mock.getRefund(any())).thenReturn(refundResponse);
    }

    public void mockResyncError(Exception exception){
        Mockito.doThrow(exception).when(mock).resyncInappSubscription(any());
    }

    public void reset() {
        Mockito.reset(mock);
    }

    private InappSubscription createInappSubscription(
            PassportUid uid, InappStore store, String productId,
            String trustOrderId,
            Function<InappSubscription.InappSubscriptionBuilder, InappSubscription.InappSubscriptionBuilder> customizer) {
        return customizer.apply(InappSubscription.builder()
                .uid(uid.toString())
                .storeSubscriptionId("storeSubscriptionId")
                .productId(productId)
                .state(InappSubscriptionState.ACTIVE)
                .storeType(store.toTrustType())
                .storeExpirationTime(DateUtils.futureDate())
                .subscriptionId(trustOrderId)
                .subscriptionUntil(DateUtils.futureDate())
                .syncTime(Instant.now()))
                .build();
    }

    private InappSubscription createInappSubscription(Function<InappSubscription.InappSubscriptionBuilder,
            InappSubscription.InappSubscriptionBuilder> customizer) {
        return customizer.apply(
                InappSubscription.builder()
                        .storeSubscriptionId("storeSubscriptionId")
                        .state(InappSubscriptionState.ACTIVE)
                        .storeExpirationTime(DateUtils.futureDate())
                        .subscriptionUntil(DateUtils.futureDate())
                        .syncTime(Instant.now()))
                .build();
    }
}
