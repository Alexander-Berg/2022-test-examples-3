package ru.yandex.market.api.partner.controllers.order.feature;

import java.util.concurrent.CompletableFuture;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.api.partner.controllers.util.request.OrderControllerHelper;
import ru.yandex.market.api.partner.controllers.util.request.OrderRequestBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author apershukov
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
class GenericBundleFeatureOrderControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final long DROPSHIP_CAMPAIGN_ID = 1000571241L;
    private static final long DROPSHIP_ORDER_ID = 124L;
    private static final int DROPSHIP_SHOP_ID = 666;

    @Autowired
    private PersonalMarketService personalMarketService;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    private CheckouterMockHelper checkouterMockHelper;
    private OrderControllerHelper orderControllerHelper;

    @BeforeEach
    void configure() {
        orderControllerHelper = new OrderControllerHelper(urlBasePrefix);
        checkouterMockHelper = new CheckouterMockHelper(
                checkouterRestTemplate,
                checkouterUrl
        );
        when(personalMarketService.retrieve(any())).thenReturn(CompletableFuture.completedFuture(
                PersonalRetrieveResponse.builder().build()
        ));
    }

    @Test
    void shouldGetOrderWithBundlesFields() {
        checkouterMockHelper.mockGetOrderReturnsBody(
                DROPSHIP_ORDER_ID, DROPSHIP_SHOP_ID,
                resourceAsString("order-with-generic-bundle-promo.json")
        );
        String resultJson = orderControllerHelper.getOrderRequest(
                OrderRequestBuilder.builder()
                        .campaignId(DROPSHIP_CAMPAIGN_ID)
                        .orderId(DROPSHIP_ORDER_ID)
                        .format(Format.JSON)
        );

        assertThat(JsonPath.read(resultJson, "$.order.items[*].bundleId"), everyItem(is("some bundle")));
        assertThat(JsonPath.read(resultJson, "$.order.items[*].promos"), hasItems(
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 499),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                )),
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 1),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                ))
        ));
    }

    @Test
    void shouldGetOrdersWithBundlesFields() {
        checkouterMockHelper.mockGetOrdersReturnsBodyItems(
                DROPSHIP_SHOP_ID,
                resourceAsString("order-with-generic-bundle-promo.json")
        );

        String resultJson = orderControllerHelper.getOrdersRequest(
                OrderRequestBuilder.builder()
                        .campaignId(DROPSHIP_CAMPAIGN_ID)
                        .orderId(DROPSHIP_ORDER_ID)
                        .format(Format.JSON)
        );

        assertThat(JsonPath.read(resultJson, "$.orders[0].items[*].bundleId"), everyItem(is("some bundle")));
        assertThat(JsonPath.read(resultJson, "$.orders[0].items[*].promos"), hasItems(
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 499),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                )),
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 1),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                ))
        ));
    }

    @Test
    void shouldGetOrderWithBundlesOnStatusUpdate() {
        checkouterMockHelper.mockOrderStatusChange(
                DROPSHIP_ORDER_ID,
                DROPSHIP_SHOP_ID,
                OrderSubstatus.STARTED,
                withSuccess(
                        resourceAsString("order-with-generic-bundle-promo.json")
                                .replaceAll("(?<=\"substatus\":\\s?\")[_A-Z]+", OrderSubstatus.STARTED.name())
                                .replaceAll("(?<=\"status\":\\s?\")[_A-Z]+",
                                        OrderSubstatus.STARTED.getStatus().name()),
                        MediaType.APPLICATION_JSON)
        );

        String resultJson = orderControllerHelper.updateOrderStatusRequest(
                OrderRequestBuilder.builder()
                        .campaignId(DROPSHIP_CAMPAIGN_ID)
                        .orderId(DROPSHIP_ORDER_ID)
                        .format(Format.JSON),
                OrderSubstatus.STARTED
        );

        assertThat(JsonPath.read(resultJson, "$.order.items[*].bundleId"), everyItem(is("some bundle")));
        assertThat(JsonPath.read(resultJson, "$.order.items[*].promos"), hasItems(
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 499),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                )),
                hasItem(allOf(
                        has("type", "GENERIC_BUNDLE"),
                        has("discount", 1),
                        has("subsidy", 0),
                        has("shopPromoId", "some shop id"),
                        has("marketPromoId", "some market md5")
                ))
        ));
    }

    private static Matcher<java.util.Map<String, Object>> has(String key, Object value) {
        Matcher m = hasEntry(key, value);
        return m;
    }
}
