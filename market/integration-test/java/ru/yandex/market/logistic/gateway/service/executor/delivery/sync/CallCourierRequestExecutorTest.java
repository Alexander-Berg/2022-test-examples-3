package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CALL_COURIER_DS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/state/partners_properties.xml")
public class CallCourierRequestExecutorTest extends AbstractIntegrationTest {
    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        mockServer = createMockServerByRequest(CALL_COURIER_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/call_courier/delivery_call_courier.xml",
            "fixtures/response/delivery/call_courier/delivery_call_courier.xml"
        );

        mockMvc.perform(post("/delivery/callCourier")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/call_courier/delivery_call_courier.json")))
            .andExpect(status().isOk())
            .andExpect(jsonContent("fixtures/response/delivery/call_courier/delivery_call_courier.json"));
    }
}
