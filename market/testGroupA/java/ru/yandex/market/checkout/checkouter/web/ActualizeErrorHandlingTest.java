package ru.yandex.market.checkout.checkouter.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.limit.HitRateGroupRateLimitsChecker;
import ru.yandex.market.checkout.common.ratelimit.RateLimitCheckerHelper;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.test.providers.CartResponseProvider;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.X_HIT_RATE_GROUP;

public class ActualizeErrorHandlingTest extends AbstractWebTestBase {

    private static final long UID = 123L;
    private static final String SHOP_ID = "4545";
    private static final int SUCCESS_CODE = 200;

    @Autowired
    private TestSerializationService testSerializationService;

    @Autowired
    private HitRateGroupRateLimitsChecker userActualizationLimits;

    @Test
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем, что чекаутер возвращает 422, если пушапи вернёт 422")
    public void shouldReturn422IfPushApiResponsesWith422() throws Exception {
        mockReport();

        pushApiMock.stubFor(
                WireMock.post(urlPathEqualTo("/shops/" + SHOP_ID + "/cart"))
                        .willReturn(aResponse()
                                .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
                                .withBody("<error>\n" +
                                        "   <code>CONNECTION_TIMED_OUT</code>\n" +
                                        "   <message>oolol</message>\n" +
                                        "   <shop-admin>false</shop-admin>\n" +
                                        "</error>\n"))
        );


        mockMvc.perform(post("/actualize?uid={uid}", UID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "  \"buyerRegionId\": 2," +
                        "  \"shopId\": " + SHOP_ID + "," +
                        "  \"feedId\": 200305173," +
                        "  \"offerId\": \"4\"" +
                        "}")
        ).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(HttpStatus.UNPROCESSABLE_ENTITY.value()))
                .andExpect(jsonPath("$.code").value(ErrorSubCode.CONNECTION_TIMED_OUT.name()));
    }

    @Disabled
    @Test
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем, что чекаутер вернет 400, если будет превышен лимит по запросам")
    public void shouldReturn4xxWhenReachedHitLimit() throws Exception {
        mockReport();
        mockPushApi();

        makeRequests();

        MockHttpServletRequestBuilder request = createRequestBuilder(HitRateGroup.LIMIT);
        for (int i = 0; i < 50; ++i) {
            mockMvc.perform(request);
        }

        RateLimitCheckerHelper.awaitEmptyQueue(userActualizationLimits.getExecutorService());

        mockMvc.perform(createRequestBuilder()).andExpect(status().is4xxClientError());
    }

    @Test
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CART)
    @Story(Stories.CHECKOUT)
    @DisplayName("Проверяем, что чекаутер вернее 200, если передана HitRateGroup=UNLIMIT")
    public void shouldReturn2xxWhenHitLimitIsUnlimit() throws Exception {
        mockReport();
        mockPushApi();

        makeRequests(HitRateGroup.UNLIMIT);

        mockMvc.perform(createRequestBuilder(HitRateGroup.UNLIMIT)).andExpect(status().is2xxSuccessful());
    }

    private void mockPushApi() {
        Delivery delivery = createDelivery();

        CartResponse order = CartResponseProvider.getCartResponse(delivery);

        pushApiMock.stubFor(
                WireMock.post(urlPathEqualTo("/shops/" + SHOP_ID + "/cart"))
                        .willReturn(aResponse()
                                .withStatus(SUCCESS_CODE)
                                .withHeader("Content-Type", "application/xml")
                                .withBody(testSerializationService.serializePushApiObject(order))
                        )
        );
    }

    private void makeRequests() throws Exception {
        makeRequests(null);
    }

    private void makeRequests(HitRateGroup rateGroup) throws Exception {
        MockHttpServletRequestBuilder request = createRequestBuilder(rateGroup);
        for (int i = 0; i < 100; ++i) {
            mockMvc.perform(request).andExpect(status().is2xxSuccessful());
        }
    }

    private MockHttpServletRequestBuilder createRequestBuilder() {
        return createRequestBuilder(null);
    }

    private MockHttpServletRequestBuilder createRequestBuilder(HitRateGroup rateGroup) {
        MockHttpServletRequestBuilder request = post("/actualize?uid={uid}", UID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "  \"buyerRegionId\": 2," +
                        "  \"shopId\": " + SHOP_ID + "," +
                        "  \"feedId\": 200305173," +
                        "  \"offerId\": \"4\"" +
                        "}");
        if (rateGroup != null) {
            request.header(X_HIT_RATE_GROUP, rateGroup.name());
        }
        return request;
    }

    private Delivery createDelivery() {
        Date shipmentDate = Date.from(LocalDate.now().plus(3, ChronoUnit.DAYS).atStartOfDay()
                .toInstant(ZoneOffset.UTC));

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setServiceName("Foreign Service");
        delivery.setPrice(BigDecimal.valueOf(1.11));
        delivery.setBuyerPrice(BigDecimal.valueOf(1.11));
        delivery.setDeliveryServiceId(99L);
        delivery.setDeliveryDates(new DeliveryDates(shipmentDate, shipmentDate));
        return delivery;
    }

    private void mockReport() throws IOException {
        String offerInfo = IOUtils.toString(ActualizeErrorHandlingTest.class.getResourceAsStream("single_offer.json"));
        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("offerinfo"))
                        .willReturn(
                                aResponse()
                                        .withStatus(SUCCESS_CODE)
                                        .withBody(offerInfo)
                        )
        );

        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo("shop_info"))
                        .willReturn(
                                aResponse()
                                        .withStatus(SUCCESS_CODE)
                                        .withBody("{\"results\": []}")
                        )
        );

        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo(MarketReportPlace.OUTLETS.getId()))
                        .willReturn(
                                aResponse()
                                        .withBody(IOUtils.toString(
                                                ReportConfigurer.class.getResource("/files/report/outlets.xml")))
                                        .withTransformers("response-template")
                        )
        );

        reportMockWhite.stubFor(
                get(urlPathEqualTo("/yandsearch"))
                        .withQueryParam("place", equalTo(MarketReportPlace.ACTUAL_DELIVERY.getId()))
                        .willReturn(
                                aResponse()
                                        .withStatus(SUCCESS_CODE)
                                        .withBody("{\"results\": []}")
                        )
        );
    }
}
