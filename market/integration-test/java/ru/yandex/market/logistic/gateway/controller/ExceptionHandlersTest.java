package ru.yandex.market.logistic.gateway.controller;

import org.junit.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.delivery.tracker.api.client.entity.errors.TrackerException;
import ru.yandex.market.delivery.tracker.domain.entity.GenericCallResponse;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.processing.LogisticApiRequestProcessingService;
import ru.yandex.market.logistics.test.integration.matchers.XmlMatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты на перехват исключений в {@link LogisticApiController}.
 */
public class ExceptionHandlersTest extends AbstractIntegrationTest {

    private static final String EXCEPTION_TEXT = "My Exception";

    @MockBean(name = "fulfillmentLogisticApiRequestProcessingService")
    private LogisticApiRequestProcessingService fulfillmentLogisticApiRequestProcessingService;

    @Test
    public void textBadRequestTrackerException() throws Exception {

        when(fulfillmentLogisticApiRequestProcessingService.process(any()))
            .thenThrow(buildTrackerException(HttpStatus.BAD_REQUEST));

        String xmlResponse = makeRequest();
        String expectedXml = getFileContent("fixtures/response/bad_request_response.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();

    }

    @Test
    public void textServerErrorTrackerException() throws Exception {

        when(fulfillmentLogisticApiRequestProcessingService.process(any()))
            .thenThrow(buildTrackerException(HttpStatus.INTERNAL_SERVER_ERROR));

        String xmlResponse = makeRequest();
        String expectedXml = getFileContent("fixtures/response/server_error_response.xml");

        softAssert.assertThat(new XmlMatcher(expectedXml).matches(xmlResponse))
            .as("Asserting that the XML response is correct")
            .isTrue();

    }

    private String makeRequest() throws Exception {
        return mockMvc.perform(post("/fulfillment/query-gateway")
            .contentType(MediaType.TEXT_XML))
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }

    private TrackerException buildTrackerException(HttpStatus httpStatus) {
        return new TrackerException(new GenericCallResponse(false, EXCEPTION_TEXT), httpStatus);
    }
}
