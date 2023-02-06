package ru.yandex.market.shopadminstub.stub;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.apache.commons.io.IOUtils;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.shopadminstub.model.Item;

import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

public class StubPushApiTestUtils {
    public static final List<PaymentMethod> ALL_POSTPAID = Arrays.asList(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY);
    public static final List<PaymentMethod> ONLY_CASH = Collections.singletonList(PaymentMethod.CASH_ON_DELIVERY);
    static final List<PaymentMethod> ONLY_CARD = Collections.singletonList(PaymentMethod.CARD_ON_DELIVERY);
    public static final List<PaymentMethod> ONLY_YANDEX = Collections.singletonList(PaymentMethod.YANDEX);

    static final int SPB_REGION = 2;
    static final int MOSCOW_REGION = 213;
    static final String CONTENT_BODY = contentBody(new FeedOfferId("1", 383182L), "RUR");
    static final int DEFAULT_SHOP_ID = 242102;
    static final DateTimeFormatter STUB_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private StubPushApiTestUtils() {
    }

    static String contentBody(FeedOfferId feedOfferId, String currency) {
        return contentBody(currency, feedOfferId);
    }

    static String contentBody(String currency, FeedOfferId... feedOfferIds) {
        StringBuilder builder = new StringBuilder();
        builder.append("<cart currency=\"").append(currency).append("\">\n").append("  <items>\n");
        for (FeedOfferId feedOfferId : feedOfferIds) {
            builder.append("    <item feed-id=\"")
                    .append(feedOfferId.getFeedId())
                    .append("\" offer-id=\"")
                    .append(feedOfferId.getId())
                    .append("\" feed-category-id=\"{{feedcategory}}\" offer-name=\"{{offername}}\" count=\"1\"/>\n");
        }
        return builder.append(
                "  </items>\n" +
                        "    <delivery>\n" +
                        "      <region id=\"2\">\n" +
                        "            <parent id=\"10174\">\n" +
                        "                <parent id=\"17\">\n" +
                        "                  <parent id=\"225\"/>\n" +
                        "                </parent>\n" +
                        "            </parent>\n" +
                        "        </region>\n" +
                        "  </delivery>\n" +
                        "</cart>").toString();
    }

    static void mockFeedDispatcherFault(Stubbing feedDispatcherMock) {
        feedDispatcherMock.stubFor(get(urlPathEqualTo("/offer"))
                .willReturn(new ResponseDefinitionBuilder().withFault(Fault.EMPTY_RESPONSE)));
    }

    static void mockReport(Stubbing reportMock, URL resource) throws IOException {
        mockReport(reportMock, resource, new FeedOfferId("1", 383182L));
    }

    static void mockReport(Stubbing reportMock, URL resource, FeedOfferId feedOfferId) throws IOException {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("offerinfo"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("feed_shoffer_id", equalTo(feedOfferId.getFeedId() + "-" + feedOfferId.getId()))
                .withQueryParam("rids", equalTo("0"))
                .withQueryParam("geo", absent())
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toByteArray(resource))));

        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("offerinfo"))
                .withQueryParam("pp", equalTo("18"))
                .withQueryParam("feed_shoffer_id", equalTo(feedOfferId.getFeedId() + "-" + feedOfferId.getId()))
                .withQueryParam("rids", equalTo(String.valueOf(SPB_REGION)))
                .withQueryParam("geo", absent())
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toByteArray(resource))));
    }

    static void mockGeo(Stubbing reportMock, URL resourceGeo, String wareMd5, long shopId) throws IOException {
        reportMock.stubFor(get(urlPathEqualTo("/yandsearch"))
                .withQueryParam("place", equalTo("geo"))
                .withQueryParam("offerid", equalTo(wareMd5))
                .withQueryParam("fesh", equalTo(String.valueOf(shopId)))
                .withQueryParam("numdoc", equalTo("1000"))
                .willReturn(new ResponseDefinitionBuilder().withBody(IOUtils.toByteArray(resourceGeo))));
    }

    public static Clock getClock(String isoTime) {
        ZoneId systemZoneId = ZoneId.systemDefault();
        return Clock.fixed(LocalDateTime.parse(isoTime).atZone(systemZoneId).toInstant(), systemZoneId);
    }

    static ResultActions checkItemAbsent(ResultActions resultActions,
                                         int index,
                                         long feedId,
                                         String offerId) throws Exception {
        return resultActions
                .andExpect(xpath("/cart/items/item[%d]/@feed-id", index).string("" + feedId))
                .andExpect(xpath("/cart/items/item[%d]/@offer-id", index).string(offerId))
                .andExpect(xpath("/cart/items/item[%d]/@count", index).number(0d))
                .andExpect(xpath("/cart/items/item[%d]/@price", index).number(250d))
                .andExpect(xpath("/cart/items/item[%d]/@delivery", index).booleanValue(true));

    }

    public static ResultActions checkItem(ResultActions resultActions,
                                   int index,
                                   long feedId,
                                   String offerId) throws Exception {
        return checkItem(resultActions, index, feedId, offerId, true);
    }

    public static ResultActions checkItem(ResultActions resultActions,
                                          int index,
                                          long feedId,
                                          String offerId,
                                          boolean delivery) throws Exception {
        return resultActions
                .andExpect(xpath("/cart/items/item[%d]/@feed-id", index).string("" + feedId))
                .andExpect(xpath("/cart/items/item[%d]/@offer-id", index).string(offerId))
                .andExpect(xpath("/cart/items/item[%d]/@price", index).string("250"))
                .andExpect(xpath("/cart/items/item[%d]/@count", index).string("1"))
                .andExpect(xpath("/cart/items/item[%d]/@delivery", index).booleanValue(delivery));
    }

    public static void checkDeliveryOption(ResultActions resultActions,
                                    LocalDate today,
                                    int optionIndex,
                                    List<PaymentMethod> paymentMethods,
                                    String serviceName,
                                    String type,
                                    String price,
                                    int fromDay,
                                    Integer toDay) throws Exception {
        resultActions
                .andExpect(xpath("/cart/delivery-options/delivery[%d]/@service-name", optionIndex).string(serviceName))
                .andExpect(xpath("/cart/delivery-options/delivery[%d]/@type", optionIndex).string(type))
                .andExpect(xpath("/cart/delivery-options/delivery[%d]/@price", optionIndex).string(price))
                .andExpect(xpath("/cart/delivery-options/delivery[%d]/dates/@from-date", optionIndex).string(today.plusDays(fromDay).format(STUB_DATE_FORMAT)))
                .andExpect(xpath("/cart/delivery-options/delivery[%d]/payment-methods/payment-method/text()", optionIndex).nodeCount(paymentMethods.size()));

        if (toDay != null) {
            resultActions
                    .andExpect(xpath("/cart/delivery-options/delivery[%d]/dates/@to-date", optionIndex).string(today.plusDays(toDay).format(STUB_DATE_FORMAT)));
        }

        for (int i = 0; i < paymentMethods.size(); i++) {
            PaymentMethod paymentMethod = paymentMethods.get(i);
            resultActions
                    .andExpect(xpath("/cart/delivery-options/delivery[%d]/payment-methods/payment-method[%d]/text()", optionIndex, i + 1).string(paymentMethod.name()));
        }
    }

    public static void checkDeliveryOptionsCount(ResultActions resultActions, int count) throws Exception {
        resultActions.andExpect(xpath("/cart/delivery-options/delivery").nodeCount(count));
    }

    public static OfferDetails mapItemToOfferDetails(Item item) {
        OfferDetails offerDetails = new OfferDetails(
                new Date(),
                new Date(),
                item.getPrice().doubleValue(),
                true,
                item.getFeedId().intValue(),
                "feedSession"
        );
        offerDetails.setShopCurrency(Currency.findByName(item.getCurrency()));

        return offerDetails;
    }
}
