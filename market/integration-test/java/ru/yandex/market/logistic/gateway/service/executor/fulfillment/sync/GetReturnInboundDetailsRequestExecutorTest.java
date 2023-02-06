package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_RETURN_INBOUND_DETAILS_FF;

/**
 * Интеграционный тест для {@link GetReturnInboundDetailsRequestExecutor}
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetReturnInboundDetailsRequestExecutorTest extends AbstractIntegrationTest {

    private final static String PARTNER_URL =
        "http://partner-api-mock.tst.vs.market.yandex.net:80/1P-getReturnInboundDetails/mock/check";

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(GET_RETURN_INBOUND_DETAILS_FF);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/get_return_inbound_details/fulfillment_get_return_inbound_details.xml",
            "fixtures/response/fulfillment/get_return_inbound_details/fulfillment_get_return_inbound_details.xml");

        mockMvc.perform(post("/fulfillment/getReturnInboundDetails")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_return_inbound_details/fulfillment_get_return_inbound_details.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/fulfillment/get_return_inbound_details/fulfillment_get_return_inbound_details.json")));

        mockServer.verify();
    }

    @Test
    public void executeResponseException() {
        doReturn(null).when(fulfillmentClient).getReturnInboundDetails(any(), any());

        assertThatThrownBy(() -> mockMvc.perform(post("/fulfillment/getReturnInboundDetails")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_return_inbound_details/fulfillment_get_return_inbound_details.json"))))
            .hasCause(new ServiceInteractionResponseFormatException(
                "Failed to get GetReturnInboundDetailsResponse response"));
    }
}
