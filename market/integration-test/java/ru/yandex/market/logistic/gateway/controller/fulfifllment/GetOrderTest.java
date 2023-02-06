package ru.yandex.market.logistic.gateway.controller.fulfifllment;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetOrderRequest;
import ru.yandex.market.logistic.gateway.config.properties.PersonalProperties;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_ORDER_FF;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetOrderTest extends AbstractIntegrationTest {

    private static final ResourceId ORDER_ID = ResourceId.builder()
        .setYandexId("100")
        .setPartnerId("ZKZ123456")
        .setFulfillmentId("ZKZ123456")
        .build();

    private static final String UNIQ = "6ea161f870ba6574d3bd9bdd19e1e9d8";

    private MockRestServiceServer mockServer;
    @Autowired
    private PersonalProperties personalProperties;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_ORDER_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
        personalProperties.setPassPersonalFieldsFromPartners(false);
    }

    @Test
    public void execute() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_without_personal_data.json"
        );
    }

    @Test
    public void executeWithPassPersonalDataFromPartnersEnabled() throws Exception {
        personalProperties.setPassPersonalFieldsFromPartners(true);
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_order/fulfillment_get_order.json"
        );
    }

    @Test
    public void executeWithOnlyOrderIdAndPlaces() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_with_only_order_id_and_places.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_with_only_order_id_and_places.json"
        );
    }

    @Test(expected = NestedServletException.class)
    public void executeWithInvalidUndefinedCount() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_with_invalid_undefined_count.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        mockMvc.perform(
            post("/fulfillment/getOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        );
    }

    @Test
    public void executeError() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_error.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        mockMvc.perform(
            post("/fulfillment/getOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().string("code 4000: Order not found"));
    }

    @Test
    public void duplicateInstanceKeys() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_duplicate_instance_keys.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());
        String jsonRequest = jsonMapper.writeValueAsString(request);

        softAssert
            .assertThatThrownBy(
                () -> mockMvc.perform(
                    post("/fulfillment/getOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest)
                )
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; nested exception is java.lang.IllegalStateException: " +
                "Duplicate key CIS (attempted merging values qwerty0987 and qwerty0987)"
            );
    }

    @Test
    public void onlyUnencodedInstances() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_order/fulfillment_get_order.xml",
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_only_unencoded_instances.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());
        String jsonRequest = jsonMapper.writeValueAsString(request);
        String expectedJson = getFileContent(
            "fixtures/response/fulfillment/get_order/fulfillment_get_order_without_personal_data.json"
        );

        String actualJson = mockMvc.perform(
                post("/fulfillment/getOrder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(actualJson).matches(expectedJson))
            .as("GetOrderResponse should be correct")
            .isTrue();
    }

    private void executeScenario(GetOrderRequest request, String jsonPath) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent(jsonPath);
        String actualJson = mockMvc.perform(
            post("/fulfillment/getOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(actualJson).matches(expectedJson))
            .as("GetOrderResponse should be correct")
            .isTrue();
    }
}
