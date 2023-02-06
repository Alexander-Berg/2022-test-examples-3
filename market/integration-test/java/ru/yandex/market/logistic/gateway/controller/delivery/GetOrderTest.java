package ru.yandex.market.logistic.gateway.controller.delivery;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetOrderRequest;
import ru.yandex.market.logistic.gateway.config.properties.PersonalProperties;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_ORDER_DS;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("/repository/state/partners_properties.xml")
public class GetOrderTest extends AbstractIntegrationTest {

    private static final ResourceId ORDER_ID = ResourceId.builder()
        .setYandexId("100")
        .setPartnerId("ZKZ123456")
        .build();

    private static final String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";
    private MockRestServiceServer mockServer;

    @Autowired
    private PersonalProperties personalProperties;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_ORDER_DS);
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
            "fixtures/request/delivery/get_order/delivery_get_order.xml",
            "fixtures/response/delivery/get_order/delivery_get_order.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent("fixtures/response/delivery/get_order/delivery_get_order.json");
        String actualJson = mockMvc.perform(
            post("/delivery/getOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("GetOrderResponse should be correct")
            .isTrue();
    }

    @Test
    public void executeWithPassPersonalDataFromPartnersEnabled() throws Exception {
        personalProperties.setPassPersonalFieldsFromPartners(true);
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_order/delivery_get_order.xml",
            "fixtures/response/delivery/get_order/delivery_get_order.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());

        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent(
            "fixtures/response/delivery/get_order/delivery_get_order_with_personal_data.json"
        );
        String actualJson = mockMvc.perform(
                post("/delivery/getOrder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("GetOrderResponse should be correct")
            .isTrue();
    }

    @Test
    public void duplicateInstanceKeys() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_order/delivery_get_order.xml",
            "fixtures/response/delivery/get_order/delivery_get_order_duplicate_instance_keys.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());
        String jsonRequest = jsonMapper.writeValueAsString(request);

        softAssert
            .assertThatThrownBy(
                () -> mockMvc.perform(
                    post("/delivery/getOrder")
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
            "fixtures/request/delivery/get_order/delivery_get_order.xml",
            "fixtures/response/delivery/get_order/delivery_get_order_only_unencoded_instances.xml"
        );

        GetOrderRequest request = new GetOrderRequest(ORDER_ID, createPartner());
        String jsonRequest = jsonMapper.writeValueAsString(request);
        String expectedJson = getFileContent("fixtures/response/delivery/get_order/delivery_get_order.json");

        String actualJson = mockMvc.perform(
                post("/delivery/getOrder")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonRequest)
            )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("GetOrderResponse should be correct")
            .isTrue();
    }
}
