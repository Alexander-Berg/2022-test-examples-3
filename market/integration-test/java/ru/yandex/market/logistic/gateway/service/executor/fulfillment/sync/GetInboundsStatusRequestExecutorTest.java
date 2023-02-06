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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_INBOUNDS_STATUS_FF;

/**
 * Интеграционный тест для {@link GetInboundsStatusRequestExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetInboundsStatusRequestExecutorTest extends AbstractIntegrationTest {

    private final static String PARTNER_URL =
        "http://partner-api-mock.tst.vs.market.yandex.net:80/1P-getInboundsStatus/mock/check";

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(GET_INBOUNDS_STATUS_FF);

        prepareMockServerXmlScenario(mockServer, PARTNER_URL,
            "fixtures/request/fulfillment/get_inbound_status/fulfillment_get_inbound_status.xml",
            "fixtures/response/fulfillment/get_inbound_status/fulfillment_get_inbound_status.xml");

        mockMvc.perform(post("/fulfillment/getInboundsStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_inbound_status/fulfillment_get_inbound_status.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/fulfillment/get_inbound_status/fulfillment_get_inbound_status.json")));

        mockServer.verify();
    }

    @Test
    public void executeResponseException() throws Exception {
        doReturn(null).when(fulfillmentClient).getInboundsStatus(any(), any());

        assertThatThrownBy(() -> mockMvc.perform(post("/fulfillment/getInboundsStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_inbound_status/fulfillment_get_inbound_status.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetInboundsStatusResponse"));
    }
}
