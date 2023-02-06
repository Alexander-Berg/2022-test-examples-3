package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.CallCourierRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.CALL_COURIER_DS;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("/repository/state/partners_properties.xml")
public class CallCourierTest extends AbstractIntegrationTest {

    private static final ResourceId ORDER_ID = ResourceId.builder()
        .setYandexId("yId123")
        .setPartnerId("pId123")
        .build();

    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";
    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(CALL_COURIER_DS);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void execute() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/call_courier/delivery_call_courier.xml",
            "fixtures/response/delivery/call_courier/delivery_call_courier.xml"
        );

        CallCourierRequest request = new CallCourierRequest(ORDER_ID, 30L, createPartner());

        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent("fixtures/response/delivery/call_courier/delivery_call_courier.json");
        String actualJson = mockMvc.perform(
            post("/delivery/callCourier")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("CallCourierResponse should be correct")
            .isTrue();
    }
}
