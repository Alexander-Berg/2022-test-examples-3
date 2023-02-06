package ru.yandex.market.loyalty.core.monitor;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.core.config.Juggler;
import ru.yandex.market.loyalty.core.logbroker.LogBrokerEvent;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerClient;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.applicability.PromoApplicabilityPolicy;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.DiscountService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.core.utils.JugglerEventView;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.loyalty.core.utils.OrderRequestUtils;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.monitoring.beans.JugglerEventsPushExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static ru.yandex.market.loyalty.core.model.coin.CoreCoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.core.monitor.CoreMonitorType.PUSH_LOGBROKER_EVENTS;
import static ru.yandex.market.loyalty.core.utils.JugglerTestUtils.JUGGLER_CLIENT_REQUEST_TYPE;

public class JugglerEventsPushExecutorTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private CoinService coinService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private DiscountService discountService;
    @Autowired
    private TskvLogBrokerClient logBrokerClient;
    @Autowired
    private JugglerEventsPushExecutor jugglerEventsPushExecutor;
    @Autowired
    @Juggler
    private HttpClient jugglerHttpClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DiscountUtils discountUtils;


    @Ignore
    @Test
    public void shouldSendServiceEventsToJugglerPushServer() throws InterruptedException, IOException {
        willThrow(IllegalArgumentException.class).given(logBrokerClient).pushEvent(any(LogBrokerEvent.class));

        Promo promoFixed = promoManager.createSmartShoppingPromo(PromoUtils.SmartShopping.defaultFixed());

        CoinKey fixedCoinId = coinService.create.createCoin(promoFixed, CoinInsertRequest.authMarketBonus(0L)
                .setSourceKey("coinFixed")
                .setReason(CoreCoinCreationReason.OTHER)
                .setStatus(ACTIVE)
                .build());

        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder.builder(
                OrderRequestUtils.orderRequestWithBundlesBuilder()
                        .withOrderItem()
                        .build())
                .withCoins(fixedCoinId)
                .withOperationContext(OperationContextFactory.withUidBuilder(0L).buildOperationContext())
                .build();

        PromoApplicabilityPolicy applicabilityPolicy = configurationService.currentPromoApplicabilityPolicy();
        discountService.spendDiscount(request, applicabilityPolicy, null);


        Thread.sleep(1000);

        given(jugglerHttpClient.execute(any(HttpUriRequest.class))).willReturn(getOkResponse());

        jugglerEventsPushExecutor.jugglerPushEvents();

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        then(jugglerHttpClient).should(atLeast(1)).execute(captor.capture());

        final List<HttpUriRequest> allValues = captor.getAllValues();
        assertThat(allValues, hasSize(1));
        final HttpPost httpRequest = (HttpPost) allValues.get(0);
        final List<JugglerEventView> jugglerEvents = objectMapper.readValue(
                httpRequest.getEntity().getContent(), JUGGLER_CLIENT_REQUEST_TYPE
        );

        assertThat(jugglerEvents, hasItem(
                allOf(
                        hasProperty("service", equalTo(PUSH_LOGBROKER_EVENTS.getJugglerService())),
                        hasProperty("status", equalTo(JugglerEvent.Status.CRIT.name()))
                )
        ));
    }

    @NotNull
    private static BasicHttpResponse getOkResponse() {
        return new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
    }

}
