package ru.yandex.market.logistic.gateway.service.executor.delivery.sync;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_ORDER_DS;

@DatabaseSetup("/repository/state/partners_properties.xml")
public class GetOrderRequestExecutorTest extends AbstractIntegrationTest {
    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private MockRestServiceServer mockServer;

    @Before
    public void setUp() {
        mockServer = createMockServerByRequest(GET_ORDER_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @Test
    public void executeSuccess() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_order/delivery_get_order.xml",
            "fixtures/response/delivery/get_order/delivery_get_order.xml"
        );

        mockMvc.perform(post("/delivery/getOrder")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("fixtures/request/delivery/get_order/delivery_get_order.json")))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andExpect(content().json(getFileContent("fixtures/response/delivery/get_order/delivery_get_order.json")));
    }
}
