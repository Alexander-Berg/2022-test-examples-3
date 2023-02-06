package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.MethodArgumentNotValidException;

import ru.yandex.market.logistic.api.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.exceptions.ServiceInteractionResponseFormatException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_MOVEMENT_STATUS_HISTORY_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetMovementStatusHistoryRequestExecutorTest extends AbstractIntegrationTest {

    @SpyBean
    private FulfillmentClient fulfillmentClient;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_MOVEMENT_STATUS_HISTORY_FF);
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
            "fixtures/request/common/get_movement_status_history/request.xml",
            "fixtures/response/common/get_movement_status_history/response.xml"
        );

        String requestContent = getFileContent("fixtures/request/common/get_movement_status_history/request.json");
        String responseContent = getFileContent("fixtures/response/common/get_movement_status_history/response.json");

        mockMvc.perform(
            post("/fulfillment/getMovementStatusHistory")
                .contentType(APPLICATION_JSON)
                .content(requestContent)
        )
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(responseContent));
    }

    @Test
    public void executeResponseException() {
        doReturn(null).when(fulfillmentClient).getMovementStatusHistory(any(), any());

        String requestContent = getFileContent("fixtures/request/common/get_movement_status_history/request.json");

        softAssert.assertThatThrownBy(
            () -> mockMvc.perform(post("/fulfillment/getMovementStatusHistory")
                .contentType(APPLICATION_JSON)
                .content(requestContent))
        )
            .hasMessageContaining("Failed to get GetMovementStatusHistoryResponse")
            .hasCauseInstanceOf(ServiceInteractionResponseFormatException.class);
    }

    @Test
    public void executeWithoutPartnerId() throws Exception {
        String requestContent = getFileContent(
            "fixtures/request/common/get_movement_status_history/request_without_partner_id.json"
        );
        var exception = mockMvc.perform(post("/fulfillment/getMovementStatusHistory")
            .contentType(APPLICATION_JSON)
            .content(requestContent))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResolvedException();

        softAssert.assertThat(exception)
            .hasMessageContaining("on field 'movementIds[0].partnerId': rejected value [null];")
            .isInstanceOf(MethodArgumentNotValidException.class);
    }

}
