package ru.yandex.market.common.report;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.common.report.model.AddressInfoRq;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.common.report.model.SearchType;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.common.report.BaseMarketReportServiceSupport.tryEncode;

public class DefaultMarketServiceSupportTest {

    private static final long YANDEX_UID = 135135L;

    @Test
    public void shouldAddYandexUidIfItIsPresent() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.OUTLETS);
        marketSearchRequest.setPp(MarketSearchRequest.INVISIBLE_PP);
        marketSearchRequest.setRegionId(213L);
        marketSearchRequest.setShopId(242102L);
        marketSearchRequest.setYandexUid(YANDEX_UID);
        marketSearchRequest.setOutletIds(Collections.singletonList(123L));

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        System.out.printf("paramString=" + paramString);

        Assert.assertTrue(paramString.contains("puid=" + YANDEX_UID));
    }

    @Test
    public void shouldNotAddYandexUidIfItIsNotPresent() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.OUTLETS);
        marketSearchRequest.setPp(MarketSearchRequest.INVISIBLE_PP);
        marketSearchRequest.setRegionId(213L);
        marketSearchRequest.setShopId(242102L);
        marketSearchRequest.setOutletIds(Collections.singletonList(123L));

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        System.out.printf("paramString=" + paramString);

        Assert.assertFalse(paramString.contains("puid="));
    }

    @Test
    public void shouldNotAddYandexUidForMainreport() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.MAIN);
        marketSearchRequest.setPp(MarketSearchRequest.INVISIBLE_PP);
        marketSearchRequest.setRegionId(213L);
        marketSearchRequest.setShopId(242102L);
        marketSearchRequest.setYandexUid(123L);

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        System.out.printf("paramString=" + paramString);

        Assert.assertFalse(paramString.contains("puid="));
    }

    @Test
    public void shouldContainClientAndCoFromIfPresent() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.MAIN);
        marketSearchRequest.setPp(MarketSearchRequest.INVISIBLE_PP);
        marketSearchRequest.setRegionId(213L);
        marketSearchRequest.setShopId(242102L);
        marketSearchRequest.setYandexUid(123L);
        marketSearchRequest.setClient("checkout");
        marketSearchRequest.setCoFrom("checkouter");

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        Assert.assertTrue(paramString.contains("client=checkout"));
        Assert.assertTrue(paramString.contains("co-from=checkouter"));
    }

    @Test
    public void shouldNotContainClientAndCoFromIfNotPresent() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.MAIN);
        marketSearchRequest.setPp(MarketSearchRequest.INVISIBLE_PP);
        marketSearchRequest.setRegionId(213L);
        marketSearchRequest.setShopId(242102L);
        marketSearchRequest.setYandexUid(123L);

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        Assert.assertFalse(paramString.contains("client="));
        Assert.assertFalse(paramString.contains("co-from="));
    }

    @Test
    public void shouldContainWareId() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.MAIN);
        marketSearchRequest.setWareIds(Arrays.asList("123", "qwe"));

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        Assert.assertTrue(paramString.contains("&offerid=123"));
        Assert.assertTrue(paramString.contains("&offerid=qwe"));
    }

    /**
     * Тест проверяет, что при переданном {@link MarketSearchRequest#marketSku marketSku} корректно формируется
     * поисковый запрос.
     */
    @Test
    public void shouldContainMarketSku() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.PRICE_RECOMMENDER);
        marketSearchRequest.setMarketSku("Iphone X 64Gb \"Серый космос\"");
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        MatcherAssert.assertThat(
                paramString,
                allOf(
                        containsString("place=price_recommender"),
                        containsString("market-sku=Iphone X 64Gb \"Серый космос\"")
                )
        );
    }

    /**
     * Тест проверяет корректность сформированного запроса в плейс рекоммендаций с PRICE_RECOMMENDER_LOW_LATENCY.
     */
    @Test
    public void shouldContainMarketLowLatencySku() {
        MarketSearchRequest marketSearchRequest =
                new MarketSearchRequest(MarketReportPlace.PRICE_RECOMMENDER_LOW_LATENCY);
        marketSearchRequest.setMarketSku("Iphone X 64Gb \"Серый космос\"");
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        MatcherAssert.assertThat(
                paramString,
                allOf(
                        containsString("place=price_recommender"),
                        containsString("market-sku=Iphone X 64Gb \"Серый космос\"")
                )
        );
    }

    @Test
    public void shoudSendShofferAs64() {
        Set<FeedOfferId> offerIds = Sets.newHashSet(
                new FeedOfferId("id1", 1L),
                new FeedOfferId("id2", 2L),
                new FeedOfferId("Конь в пальто, размерXXL", 3L)

        );

        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.OFFER_INFO);
        marketSearchRequest.setOfferIds(offerIds);
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(
                paramString,
                allOf(
                        containsString(format("feed_shoffer_id=%s", tryEncode("1-id1"))),
                        containsString(format("feed_shoffer_id=%s", tryEncode("2-id2"))),
                        containsString(format(
                                "feed_shoffer_id_base64=%s", tryEncode(encodeBase64String("1-id1".getBytes())))),
                        containsString(format(
                                "feed_shoffer_id_base64=%s", tryEncode(encodeBase64String("2-id2".getBytes())))),
                        containsString(format(
                                "feed_shoffer_id=%s", tryEncode("3-Конь в пальто, размерXXL"))),
                        containsString(format(
                                "feed_shoffer_id_base64=%s", tryEncode(encodeBase64String(("3-Конь в пальто, " +
                                        "размерXXL").getBytes()))))
                )
        );
    }

    @Test
    public void shouldSendAllowedPromos() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.OFFER_INFO);
        marketSearchRequest.setAllowedPromos(Collections.singleton("such_promo"));
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(
                paramString,
                containsString(format("allowed-promos=%s", "such_promo"))
        );
    }

    @Test
    public void shouldSendCalculateDelivery() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.SHOP_OFFERS);
        marketSearchRequest.setCalculateDelivery(true);

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        MatcherAssert.assertThat(paramString, containsString(format("calculate-delivery=%s", "1")));
    }

    @Test
    public void shouldNotContainUseVirtShopZero() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.CHECK_PRICES);

        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        Assert.assertFalse(paramString.contains("use-virt-shop=0"));
    }

    @Test
    public void shouldSendShowCredit() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.CREDIT_INFO);
        marketSearchRequest.setShowCredits(true);
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(
                paramString,
                containsString("show-credits=1")
        );
    }

    @Test
    public void shouldSendAddress() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.ACTUAL_DELIVERY);
        marketSearchRequest.setAddresses(
                Stream.of(new AddressInfoRq(
                                "courier", "1", 111222L, null, null, new BigDecimal("55.2"), new BigDecimal("66.42")),
                        new AddressInfoRq(
                                "pickup", "2", null, 123456L, null, new BigDecimal("55.4"), new BigDecimal("66.34")),
                        new AddressInfoRq(
                                "post", "2", null, null, 12312L, new BigDecimal("55.4"), new BigDecimal("66.34"))
                )
                        .collect(toList()));
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(
                paramString,
                containsString("address=type:courier;id:1;rid:111222;lat:55.2;lon:66.42&address=type:pickup;id:2;" +
                        "outlet:123456;lat:55.4;lon:66.34&address=type:post;id:2;post_code:12312;lat:55.4;lon:66.34")
        );
    }

    @Test
    public void shouldSupportSearchTypeOverriding() {
        DefaultMarketServiceSupport tested = new DefaultMarketServiceSupport();
        Map<SearchType, String> urls = new HashMap<>();
        urls.put(SearchType.MARKET, "http://int");
        urls.put(SearchType.MARKET_LOW_LATENCY, "http://api");
        tested.setUrlsBySearchType(urls);

        /*
        без оверрайдинга
         */
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.ACTUAL_DELIVERY);
        assertEquals("http://int", tested.getReportUrl(marketSearchRequest));

        /*
        c оверрайдингом
         */
        marketSearchRequest.setSearchType(SearchType.MARKET_LOW_LATENCY);
        assertEquals("http://api", tested.getReportUrl(marketSearchRequest));
    }

    @Test
    public void shouldSupportDynamicFilters() {
        /*
        по дефолту параметр dynamic-filters не используется
         */
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.ACTUAL_DELIVERY);
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(paramString, not(containsString("dynamic-filters")));

        /*
        с параметром
         */
        marketSearchRequest.setDynamicFilters(MarketSearchRequest.DISABLE_DYNAMIC_FILTERS);
        paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(paramString, containsString("dynamic-filters=0"));
    }

    @Test
    public void shouldSupportShowSubscriptionGoods() {
        /*
        по дефолту параметр show-subscription-goods не используется
         */
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.ACTUAL_DELIVERY);
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(paramString, not(containsString("show-subscription-goods")));

        /*
        с параметром
         */
        marketSearchRequest.setShowSubscriptionGoods(true);
        paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);

        MatcherAssert.assertThat(paramString, containsString("show-subscription-goods=1"));
    }

    @Test
    public void shouldSupportCpaReal() {
        MarketSearchRequest marketSearchRequest = new MarketSearchRequest(MarketReportPlace.ACTUAL_DELIVERY);
        marketSearchRequest.setRgb(Color.BLUE);
        marketSearchRequest.setUseCpaReal(true);
        String paramString = DefaultMarketServiceSupport.searchRequestToParams(marketSearchRequest);
        MatcherAssert.assertThat(paramString, containsString("cpa=real"));
    }
}
