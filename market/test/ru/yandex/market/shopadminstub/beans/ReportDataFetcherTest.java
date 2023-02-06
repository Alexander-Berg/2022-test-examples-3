package ru.yandex.market.shopadminstub.beans;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartParameters;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;
import ru.yandex.market.shopadminstub.services.CartService;
import ru.yandex.market.shopadminstub.services.report.OfferInfoService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ReportDataFetcherTest extends AbstractTestBase {

    private static final OfferItemKey OFFER_KEY = OfferItemKey.of("3", 1069L, null);
    @Autowired
    private WireMockServer reportMock;

    @Autowired
    private OfferInfoService offerInfoService;

    @Autowired
    private CartService cartService;

    @Test
    public void diffInOfferInfo() throws Exception {
        final FeedOfferId feedOfferId = FeedOfferId.from(871361, "965217");
        final long shopId = 955340;

        var item = new Item();
        item.setFeedId(871361L);
        item.setOfferId("965217");
        item.setCount(1);
        item.setDeliveryPossible(false);
        item.setPickupPossible(false);
        item.setGlobal(false);
        item.setFulfilmentShopId(shopId);
        item.setSku("101153977926");
        item.setShopSku("965217");
        item.setWarehouseId(52306);

        var cartRequest = new CartRequest();
        cartRequest.setItems(List.of(item));
        cartRequest.setRegionId(10758);
        cartRequest.setDeliveryRegionId(10758);
        cartRequest.setCurrency("RUR");
        cartRequest.setFulfilment(false);
        cartRequest.setPreorder(false);
        cartRequest.setDeliveryCurrency(Currency.RUR);
        cartRequest.setRgb(Color.BLUE);
        cartRequest.setContext(Context.MARKET);
        cartRequest.setHasCertificate(false);

        var answerOfferInfoWithRids = new CartParameters();

        answerOfferInfoWithRids.setDeliveryRegionId(10758);
        answerOfferInfoWithRids.setCurrency("RUR");
        answerOfferInfoWithRids.setDeliveryCurrency(Currency.RUR);
        answerOfferInfoWithRids.setRgb(Color.BLUE);
        answerOfferInfoWithRids.setShopId(shopId);

        answerOfferInfoWithRids.setItems(new HashMap<>(
                ImmutableMap.of(
                        OfferItemKey.of(feedOfferId, null),
                        item)));

        WireMockHelper.mockReport(
                reportMock,
                feedOfferId,
                0L,
                ReportDataFetcherTest.class.getResource("/report/report_answer_rids_0.json")
        );

        WireMockHelper.mockReport(
                reportMock,
                feedOfferId,
                10758L,
                ReportDataFetcherTest.class.getResource("/report/report_answer_with_region.json")
        );

        WireMockHelper.mockReportGeo(
                reportMock,
                shopId,
                ReportDataFetcherTest.class.getResource("/report/empty_geo.json")
        );

        var fullAnswer = cartService.actualizeCartParameters(shopId, cartRequest);

        offerInfoService.fetchReport(10758, answerOfferInfoWithRids);

        assertThat(fullAnswer.getDeliveryRegionId(), equalTo(answerOfferInfoWithRids.getDeliveryRegionId()));
        assertThat(fullAnswer.getItems(), equalTo(answerOfferInfoWithRids.getItems()));
        assertThat(fullAnswer.getCurrency(), equalTo(answerOfferInfoWithRids.getCurrency()));
    }

    @Test
    public void shouldPreferDeliveryOptionsOverLocalDelivery() throws Exception {
        mockReport(reportMock, "/report/report_answer_with_delivery_options.json");

        CartParameters cartParameters = createCartParameters();

        offerInfoService.fetchReport(10740, cartParameters);

        ItemDeliveryOption onlyOption = getSingleDeliveryOption(cartParameters);
        assertThat(onlyOption.getPrice(), Matchers.comparesEqualTo(BigDecimal.ONE));
    }

    @Test
    public void shouldFilterUnknownPaymentOptions() throws Exception {
        mockReport(reportMock, "/report/report_answer_with_unknown_payment_methods.json");

        CartParameters cartParameters = createCartParameters();

        offerInfoService.fetchReport(10740, cartParameters);

        Collection<PaymentMethod> paymentMethods = getSingleDeliveryOption(cartParameters).getPaymentMethods();
        Assertions.assertEquals(Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY),
                paymentMethods);
    }

    @AfterEach
    public void tearDown() {
        reportMock.resetAll();
    }

    private void mockReport(WireMockServer reportMock, String resource) throws IOException {
        WireMockHelper.mockReport(
                reportMock,
                OFFER_KEY.getFeedOfferPart(),
                10740L,
                ReportDataFetcherTest.class.getResource(resource)
        );
    }

    private ItemDeliveryOption getSingleDeliveryOption(CartParameters cartParameters) {
        List<ItemDeliveryOption> deliveryOptions = cartParameters.getItems().get(OFFER_KEY).getDeliveryOptions();
        return Iterables.getOnlyElement(deliveryOptions);
    }

    private CartParameters createCartParameters() {
        Item item = new Item();
        CartParameters cartParameters = new CartParameters();
        cartParameters.setRgb(Color.BLUE);
        cartParameters.setShopId(774L);
        cartParameters.setItems(new HashMap<>(ImmutableMap.of(OFFER_KEY, item)));
        item.setFeedId(OFFER_KEY.getFeedId());
        item.setOfferId(OFFER_KEY.getOfferId());
        cartParameters.setCurrency("RUR");
        cartParameters.setDeliveryCurrency(Currency.USD);
        return cartParameters;
    }
}
