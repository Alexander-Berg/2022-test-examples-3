package ru.yandex.market.api.partner.controllers.order;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "OrderControllerTest.before.csv")
abstract class AbstractOrderItemControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {

    protected static final long SOME_ORDER_ID = 123L;
    protected static final int DROPSHIP_BY_SELLER_ID = 2001;
    protected static final long DROPSHIP_BY_SELLER_CAMPAIGN_ID = 20001L;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    protected RestTemplate checkouterRestTemplate;
    @Value("${market.checkouter.client.url}")
    protected String checkouterUrl;

    abstract String getResourcePath();

    abstract String getCheckouterRequestUrl();

    abstract String getApiRequestUrl();


    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекутера+ответ ПАПИ.
     */
    protected void testApiInteractionThroughCheckouter(
            String apiRequestFile,
            String expectedApiResponseFile,
            String mockedCheckouterResponseFile,
            String expectedCheckouterRequestFile,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            long orderId
    ) {
        String apiRequestBody = resourceAsString(getResourcePath() + apiRequestFile);
        String checkouterAnswerBody = resourceAsString(getResourcePath() + mockedCheckouterResponseFile);
        RequestMatcher expectedCheckouterRequestMatcher =
                json(Paths.get(getResourcePath() + expectedCheckouterRequestFile));

        testApiInteractionThroughCheckouter(
                apiRequestBody,
                checkouterAnswerBody,
                expectedCheckouterRequestMatcher,
                resourceAsString(getResourcePath() + expectedApiResponseFile),
                format,
                expectedStatus,
                campaignId,
                shopId,
                orderId
        );
    }

    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекаутера+ответ ПАПИ.
     */
    private void testApiInteractionThroughCheckouter(
            String apiRequestBody,
            String checkouterAnswerBody,
            RequestMatcher expectedCheckouterRequestMatcher,
            String expectedBody,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            long orderId
    ) {

        MockRestServiceServer.createServer(checkouterRestTemplate)
                .expect(method(HttpMethod.PUT))
                .andExpect(
                        requestTo(
                                String.format(
                                        getCheckouterRequestUrl(),
                                        checkouterUrl,
                                        orderId,
                                        shopId
                                )
                        )
                )
                .andExpect(expectedCheckouterRequestMatcher)
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(checkouterAnswerBody)
                );

        final String url = String.format(getApiRequestUrl(), urlBasePrefix,
                campaignId, orderId, format.formatName(), shopId);
        final ResponseEntity<String> response = doPut(url, apiRequestBody, format);
        assertThat(response.getStatusCode(), equalTo(expectedStatus));
        format.assertResult(expectedBody, response.getBody());
    }

    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекутера+ответ ПАПИ.
     */
    protected void testApiInteractionThroughCheckouterWithBadRequest(
            String apiRequestFile,
            String expectedApiResponseFile,
            String mockedCheckouterResponseFile,
            String expectedCheckouterRequestFile,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            long orderId
    ) {
        String apiRequestBody = resourceAsString(getResourcePath() + apiRequestFile);
        String checkouterAnswerBody = resourceAsString(getResourcePath() + mockedCheckouterResponseFile);
        RequestMatcher expectedCheckouterRequestMatcher =
                json(Paths.get(getResourcePath() + expectedCheckouterRequestFile));

        testApiInteractionThroughCheckouterWithBadRequest(
                apiRequestBody,
                checkouterAnswerBody,
                expectedCheckouterRequestMatcher,
                resourceAsString(getResourcePath() + expectedApiResponseFile),
                format,
                expectedStatus,
                campaignId,
                shopId,
                orderId
        );
    }

    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекаутера+ответ ПАПИ.
     */
    private void testApiInteractionThroughCheckouterWithBadRequest(
            String apiRequestBody,
            String checkouterAnswerBody,
            RequestMatcher expectedCheckouterRequestMatcher,
            String expectedBody,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            long orderId
    ) {

        MockRestServiceServer.createServer(checkouterRestTemplate)
                .expect(method(HttpMethod.PUT))
                .andExpect(
                        requestTo(
                                String.format(
                                        getCheckouterRequestUrl(),
                                        checkouterUrl,
                                        orderId,
                                        shopId
                                )
                        )
                )
                .andExpect(expectedCheckouterRequestMatcher)
                .andRespond(
                        withBadRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(checkouterAnswerBody)
                );

        final String url = String.format(getApiRequestUrl(), urlBasePrefix,
                campaignId, orderId, format.formatName(), shopId);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doPut(url, apiRequestBody, format));
        assertThat(exception.getStatusCode(), equalTo(expectedStatus));
        format.assertResult(expectedBody, exception.getResponseBodyAsString());
    }

    protected ResponseEntity<String> doPut(String url, String body, Format format) {
        try {
            return FunctionalTestHelper.makeRequest(new URI(url), HttpMethod.PUT, format, body);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
