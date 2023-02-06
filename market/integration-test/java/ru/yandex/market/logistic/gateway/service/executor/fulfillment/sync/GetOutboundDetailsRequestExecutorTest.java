package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_OUTBOUND_DETAILS_FF;

/**
 * Интеграционный тест для {@link GetOutboundDetailsRequestExecutor}.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOutboundDetailsRequestExecutorTest extends AbstractIntegrationTest {

    private final static String UNIQ = "avoO3ysgj4byu8b1eGlawg3jmP0JSJEj";

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    @Before
    public void setup() throws Exception {
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        MockRestServiceServer mockServer = createMockServerByRequest(GET_OUTBOUND_DETAILS_FF);

        prepareMockServerXmlScenario(mockServer, GATEWAY_URL,
            "fixtures/request/fulfillment/get_outbound_details/fulfillment_get_outbound_details.xml",
            "fixtures/response/fulfillment/get_outbound_details/fulfillment_get_outbound_details.xml");

        mockMvc.perform(post("/fulfillment/getOutboundDetails")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_outbound_details/fulfillment_get_outbound_details.json")))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/fulfillment/get_outbound_details/fulfillment_get_outbound_details.json")));

        mockServer.verify();
    }

    @Test
    public void executeResponseException() throws Exception {
        doReturn(null).when(fulfillmentClient).getOutboundDetails(any(), any());

        assertThatThrownBy(() -> mockMvc.perform(post("/fulfillment/getOutboundDetails")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("fixtures/request/fulfillment/get_outbound_details/fulfillment_get_outbound_details.json"))))
            .hasCause(new ServiceInteractionResponseFormatException("Failed to get GetOutboundDetailsResponse"));
    }
}
