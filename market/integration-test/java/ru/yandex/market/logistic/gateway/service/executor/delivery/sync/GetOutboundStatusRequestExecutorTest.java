package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.MethodArgumentNotValidException;

import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_OUTBOUND_STATUS_DS;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOutboundStatusRequestExecutorTest extends AbstractIntegrationTest {

    @SpyBean
    private DeliveryServiceClient deliveryClient;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_OUTBOUND_STATUS_DS);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/common/get_outbound_status/request.xml",
            "fixtures/response/common/get_outbound_status/response.xml"
        );

        String requestContent = getFileContent("fixtures/request/common/get_outbound_status/request.json");
        String responseContent = getFileContent("fixtures/response/common/get_outbound_status/response.json");

        mockMvc.perform(post("/delivery/getOutboundStatus").contentType(APPLICATION_JSON).content(requestContent))
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(responseContent));
    }

    @Test
    public void executeResponseException() {
        doReturn(null).when(deliveryClient).getOutboundStatus(any(), any());

        String requestContent = getFileContent("fixtures/request/common/get_outbound_status/request.json");

        softAssert.assertThatThrownBy(
            () -> mockMvc.perform(post("/delivery/getOutboundStatus")
                .contentType(APPLICATION_JSON)
                .content(requestContent))
        )
            .hasMessageContaining("Failed to get GetOutboundStatusResponse")
            .hasCauseInstanceOf(ServiceInteractionResponseFormatException.class);
    }

    @Test
    public void executeWithoutPartnerId() throws Exception {
        String requestContent = getFileContent(
            "fixtures/request/common/get_outbound_status/request_without_partner_id.json"
        );
        var exception = mockMvc.perform(post("/delivery/getOutboundStatus")
            .contentType(APPLICATION_JSON)
            .content(requestContent))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResolvedException();

        softAssert.assertThat(exception)
            .hasMessageContaining("on field 'outboundIds[0].partnerId': rejected value [null];")
            .isInstanceOf(MethodArgumentNotValidException.class);
    }

}
