package ru.yandex.market.api.partner.controllers.order.v2.buyer;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectRequest;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectResponse;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.personal_market.PersonalRetrieveRequestBuilder;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.order.config.OrderControllerV2Config.ENV_USE_PERSONAL_GET_BUYER_INFO;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тесты для ручки "orders/{orderId}/buyer"
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class BuyerHandlerTest extends OrderControllerV2TestTemplate {

    @ParameterizedTest(name = "{0}, use personal = {1}")
    @CsvSource({
            "JSON,true,get-buyer-info.json",
            "JSON,false,get-buyer-info.json",
            "XML,true,get-buyer-info.xml",
            "XML,false,get-buyer-info.xml"
    })
    void getBuyerInfoTest(Format format, String usePersonal, String expectedContentFilePath) {
        environmentService.setValue(ENV_USE_PERSONAL_GET_BUYER_INFO, usePersonal);
        checkouterMockHelper.mockGetBuyerPhoneVisibleOrder(
                ORDER_ID,
                CLIENT_ID,
                resourceAsString("mocks/checkouter/orderForRedirect.json")
        );

        mockMarketId(151, "Стриж");

        when(personalMarketService.retrieve(new PersonalRetrieveRequestBuilder()
                .fullName("fullname_064d5366e6df84ddef2f0a9a34e0a2b4")
                .phone("phone_0f939ded548bcc2663b22e70f43f4b76")
        )).thenReturn(CompletableFuture.completedFuture(
                PersonalRetrieveResponse.builder()
                        .fullName("fullname_064d5366e6df84ddef2f0a9a34e0a2b4", "Даша", "Тимофеева", null)
                        .phone("phone_0f939ded548bcc2663b22e70f43f4b76", "+7 921 301-69-09")
                        .build()
        ));

        when(communicationProxyClient.createRedirect(eq(new CreateRedirectRequest()
                .orderId(1L)
                .partnerId(10281764L))))
                .thenReturn(new CreateRedirectResponse().proxyNumber("+79100050648"));

        var response = getBuyerInfo(CAMPAIGN_ID, ORDER_ID, format);
        assertResponse(response.getBody(), "expected/" + expectedContentFilePath, format);
    }

    @ParameterizedTest()
    @CsvSource({
            "true",
            "false",
    })
    void getBuyerInfoWithRealNumberTest(String usePersonal) {
        environmentService.setValue(ENV_USE_PERSONAL_GET_BUYER_INFO, usePersonal);
        checkouterMockHelper.mockGetBuyerPhoneVisibleOrder(
                ORDER_ID,
                CLIENT_ID,
                resourceAsString("mocks/checkouter/orderForRedirect.json")
        );

        mockMarketId(151, "Стриж");

        when(personalMarketService.retrieve(new PersonalRetrieveRequestBuilder()
                .fullName("fullname_064d5366e6df84ddef2f0a9a34e0a2b4")
                .phone("phone_0f939ded548bcc2663b22e70f43f4b76")
        )).thenReturn(CompletableFuture.completedFuture(
                PersonalRetrieveResponse.builder()
                        .fullName("fullname_064d5366e6df84ddef2f0a9a34e0a2b4", "Даша", "Тимофеева", null)
                        .phone("phone_0f939ded548bcc2663b22e70f43f4b76", "+7 921 301-69-09")
                        .build()
        ));

        when(communicationProxyClient.createRedirect(eq(new CreateRedirectRequest()
                .orderId(1L)
                .partnerId(10281764L))))
                .thenReturn(new CreateRedirectResponse().proxyNumber(null));

        var response = getBuyerInfo(CAMPAIGN_ID, ORDER_ID, Format.JSON);
        assertResponse(response.getBody(), "expected/get-buyer-info-real-phone.json", Format.JSON);
    }

    @Test
    void getBuyerInfoDeliveredTest() {
        checkouterMockHelper.mockGetBuyerPhoneVisibleOrder(
                ORDER_ID,
                CLIENT_ID,
                resourceAsString("mocks/checkouter/get_order_delivered.json")
        );
        mockMarketId(151, "Стриж");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBuyerInfo(CAMPAIGN_ID, ORDER_ID, Format.JSON)
        );

        assertThat(exception, hasErrorCode(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getBuyerInfoForDropshipTest() {
        checkouterMockHelper.mockGetBuyerPhoneVisibleOrder(
                ORDER_ID,
                CLIENT_ID,
                resourceAsString("mocks/checkouter/dropshipOrderForRedirect.json")
        );

        mockMarketId(151, "Стриж");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getBuyerInfo(CAMPAIGN_ID, ORDER_ID, Format.JSON)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.FORBIDDEN)
        );
        MbiAsserts.assertJsonEquals(
                //language=json
                "{\"errors\":[{\"code\":\"ACTION_FORBIDDEN\"," +
                        "\"message\":\"Action available only for DBS model\"}]," +
                        "\"status\":\"ERROR\"}",
                exception.getResponseBodyAsString()
        );
    }

    static Stream<Arguments> getBuyerInfoTestData() {
        return Stream.of(
                Arguments.of(Format.JSON, "expected/get-buyer-info.json"),
                Arguments.of(Format.XML, "expected/get-buyer-info.xml")
        );
    }

    private ResponseEntity<String> getBuyerInfo(long campaignId, long orderId, Format format) {
        return FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" +
                        campaignId +
                        "/orders/" +
                        orderId +
                        "/buyer",
                HttpMethod.GET,
                format);
    }
}
