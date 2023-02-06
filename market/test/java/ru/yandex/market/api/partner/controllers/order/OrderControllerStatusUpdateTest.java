package ru.yandex.market.api.partner.controllers.order;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.model.OrderStatusDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderSubstatusDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DbUnitDataSet(before = "OrderControllerTest.before.csv")
class OrderControllerStatusUpdateTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final String RESOURCE_STATUS_UPDATE_PATH = "resources/status-update/";
    private static final long UNKNOWN_CAMPAIGN_ID = 123999L;
    private static final long DROPSHIP_BY_SELLER_CAMPAIGN_ID = 20001L;
    private static final int DROPSHIP_BY_SELLER_ID = 2001;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    /**
     * При успешном обновлении заказов в чекаутере, ПАПИ отдает HTTP 200 OK и заказы с новыми статусами.
     * Формат данных - XML
     */
    @Test
    void testUpdateStatusOrdersXml() {
        testApiInteractionThroughCheckouter(
                "api-post-update-statuses-ok-request.xml",
                "api-post-update-statuses-ok-answer.xml",
                "checkouter-post-update-statuses-ok-request.json",
                "checkouter-post-update-statuses-ok-answer.json",
                Format.XML,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                OrderStatusDTO.PROCESSING,
                OrderSubstatusDTO.STARTED
        );
    }

    /**
     * При успешном обновлении заказов в чекаутере, ПАПИ отдает HTTP 200 OK и заказы с новыми статусами.
     * Формат данных - JSON
     */
    @Test
    void testUpdateStatusOrdersJson() {
        testApiInteractionThroughCheckouter(
                "api-post-update-statuses-ok-request.json",
                "api-post-update-statuses-ok-answer.json",
                "checkouter-post-update-statuses-ok-request.json",
                "checkouter-post-update-statuses-ok-answer.json",
                Format.JSON,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                OrderStatusDTO.PROCESSING,
                OrderSubstatusDTO.STARTED
        );
    }

    /**
     * Если чекаутер не может обновить статусы у заказов, то ПАПИ отдает HTTP 200 OK,
     * заказы с такими же сообщениями об ошибках и статусами, если их проставил чекаутер.
     */
    @Test
    void testStatusNotUpdatedByCheckouter() {
        testApiInteractionThroughCheckouter(
                "api-post-update-statuses-ok-request.json",
                "api-post-update-statuses-not-ok-answer.json",
                "checkouter-post-update-statuses-ok-request.json",
                "checkouter-post-update-statuses-not-ok-answer.json",
                Format.JSON,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                OrderStatusDTO.PROCESSING,
                OrderSubstatusDTO.STARTED
        );
    }

    /**
     * Если чекаутер возвращает пустой список, то ПАПИ отдает HTTP 200 OK и пустой список в orders.
     */
    @Test
    void testEmptyResponseFromCheckouter() {
        testApiInteractionThroughCheckouter(
                "api-post-update-statuses-ok-request.json",
                "api-post-update-statuses-empty-answer.json",
                "checkouter-post-update-statuses-ok-request.json",
                "checkouter-post-update-statuses-empty-answer.json",
                Format.JSON,
                HttpStatus.OK,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                DROPSHIP_BY_SELLER_ID,
                OrderStatusDTO.PROCESSING,
                OrderSubstatusDTO.STARTED
        );
    }

    /**
     * Если в запросе содержится больше чем 30 заказов, то ПАПИ отдает HTTP 400 BAD REQUEST.
     */
    @Test
    void testLimitOrdersInRequest() {
        Assertions.assertThrows(
                Exception.class,
                () -> testApiInteractionThroughCheckouter(
                        "api-post-update-statuses-large-request.xml",
                        "api-post-update-statuses-ok-answer.xml",
                        "checkouter-post-update-statuses-ok-request.json",
                        "checkouter-post-update-statuses-ok-answer.json",
                        Format.XML,
                        HttpStatus.BAD_REQUEST,
                        DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                        DROPSHIP_BY_SELLER_ID,
                        OrderStatusDTO.PROCESSING,
                        OrderSubstatusDTO.STARTED)
        );
    }

    /**
     * Батчинг по 10 заказов.
     */
    @Test
    void testBatchOrdersInRequest() {
        String apiRequestBody = resourceAsString(RESOURCE_STATUS_UPDATE_PATH +
                "api-post-update-statuses-batch-ok-request.json");
        String checkouterAnswerBodyBatch1 = resourceAsString(RESOURCE_STATUS_UPDATE_PATH +
                "checkouter-post-update-statuses-batch-1-ok-answer.json");
        RequestMatcher expectedCheckouterRequestMatcherBatch1 =
                json(Paths.get(RESOURCE_STATUS_UPDATE_PATH +
                        "checkouter-post-update-statuses-batch-1-ok-request.json"));
        String checkouterAnswerBodyBatch2 = resourceAsString(RESOURCE_STATUS_UPDATE_PATH +
                "checkouter-post-update-statuses-batch-2-ok-answer.json");
        RequestMatcher expectedCheckouterRequestMatcherBatch2 =
                json(Paths.get(RESOURCE_STATUS_UPDATE_PATH +
                        "checkouter-post-update-statuses-batch-2-ok-request.json"));

        var server = MockRestServiceServer.createServer(checkouterRestTemplate);

        List<String> answers = List.of(checkouterAnswerBodyBatch1, checkouterAnswerBodyBatch2);
        List<RequestMatcher> requests = List.of(
                expectedCheckouterRequestMatcherBatch1,
                expectedCheckouterRequestMatcherBatch2
        );
        for (int i = 0; i < 2; i++) {
            server.expect(method(HttpMethod.POST))
                    .andExpect(
                            requestTo(
                                    String.format(
                                            "%s/orders/status?clientRole=SHOP&clientId=%s&shopId=" +
                                                    "&status=%s&substatus=%s",
                                            checkouterUrl,
                                            DROPSHIP_BY_SELLER_ID,
                                            OrderStatusDTO.PROCESSING,
                                            OrderSubstatusDTO.STARTED
                                    )
                            )
                    )
                    .andExpect(requests.get(i))
                    .andRespond(
                            withSuccess()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(answers.get(i))
                    );
        }

        final String url = String.format(
                "%s/campaigns/%s/orders/status-update",
                urlBasePrefix,
                DROPSHIP_BY_SELLER_CAMPAIGN_ID
        );
        final ResponseEntity<String> response = doPost(url, apiRequestBody, Format.JSON);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        Format.JSON.assertResult(
                resourceAsString(RESOURCE_STATUS_UPDATE_PATH + "api-post-update-statuses-batch-ok-answer.json"),
                response.getBody()
        );
    }

    /**
     * Если кампании с заданным ID нет, то ПАПИ отдает HTTP 404 NOT FOUND.
     */
    @Test
    void testCampaignNotFound() {
        Assertions.assertThrows(
                Exception.class,
                () -> testApiInteractionThroughCheckouter(
                        "api-post-update-statuses-ok-request.xml",
                        "api-post-update-statuses-ok-answer.xml",
                        "checkouter-post-update-statuses-ok-request.json",
                        "checkouter-post-update-statuses-ok-answer.json",
                        Format.XML,
                        HttpStatus.NOT_FOUND,
                        UNKNOWN_CAMPAIGN_ID,
                        DROPSHIP_BY_SELLER_ID,
                        OrderStatusDTO.PROCESSING,
                        OrderSubstatusDTO.STARTED)
        );
    }

    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекутера+ответ ПАПИ.
     */
    private void testApiInteractionThroughCheckouter(
            String apiRequestFile,
            String expectedApiResponseFile,
            String expectedCheckouterRequestFile,
            String mockedCheckouterResponseFile,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            OrderStatusDTO status,
            OrderSubstatusDTO substatus
    ) {
        String apiRequestBody = resourceAsString(RESOURCE_STATUS_UPDATE_PATH + apiRequestFile);
        String checkouterAnswerBody = resourceAsString(RESOURCE_STATUS_UPDATE_PATH + mockedCheckouterResponseFile);
        RequestMatcher expectedCheckouterRequestMatcher =
                json(Paths.get(RESOURCE_STATUS_UPDATE_PATH + expectedCheckouterRequestFile));

        testApiInteractionThroughCheckouter(
                apiRequestBody,
                resourceAsString(RESOURCE_STATUS_UPDATE_PATH + expectedApiResponseFile),
                expectedCheckouterRequestMatcher,
                checkouterAnswerBody,
                format,
                expectedStatus,
                campaignId,
                shopId,
                status,
                substatus
        );

    }

    /**
     * Вспомогательный метод, для проверки полного сценария ПАПИ+проверки+запрос в чекаутер+ответ чекаутера+ответ ПАПИ.
     */
    private void testApiInteractionThroughCheckouter(
            String apiRequestBody,
            String apiExpectedResponseBody,
            RequestMatcher expectedCheckouterRequestMatcher,
            String checkouterAnswerBody,
            Format format,
            HttpStatus expectedStatus,
            long campaignId,
            long shopId,
            OrderStatusDTO status,
            OrderSubstatusDTO substatus
    ) {

        MockRestServiceServer.createServer(checkouterRestTemplate)
                .expect(method(HttpMethod.POST))
                .andExpect(
                        requestTo(
                                String.format(
                                        "%s/orders/status?clientRole=SHOP&clientId=%s&shopId=&status=%s&substatus=%s",
                                        checkouterUrl,
                                        shopId,
                                        status,
                                        substatus
                                )
                        )
                )
                .andExpect(expectedCheckouterRequestMatcher)
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(checkouterAnswerBody)
                );

        final String url = String.format("%s/campaigns/%s/orders/status-update", urlBasePrefix, campaignId);
        final ResponseEntity<String> response = doPost(url, apiRequestBody, format);
        assertThat(response.getStatusCode(), equalTo(expectedStatus));
        format.assertResult(apiExpectedResponseBody, response.getBody());
    }

    private ResponseEntity<String> doPost(String url, String body, Format format) {
        try {
            return FunctionalTestHelper.makeRequest(new URI(url), HttpMethod.POST, format, body);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
