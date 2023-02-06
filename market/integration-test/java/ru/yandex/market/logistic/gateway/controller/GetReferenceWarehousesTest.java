package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.GetReferenceWarehousesRequest;
import ru.yandex.market.logistic.gateway.utils.CommonDtoFactory;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_REFERENCE_WAREHOUSES_DS;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetReferenceWarehousesTest extends AbstractIntegrationTest {

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_REFERENCE_WAREHOUSES_DS);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccess() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_warehouses/delivery_get_reference_warehouses.xml",
            "fixtures/response/delivery/get_reference_warehouses/delivery_get_reference_warehouses.xml"
        );

        GetReferenceWarehousesRequest request = new GetReferenceWarehousesRequest(createPartner());
        executeScenario(request);
    }

    @Test
    public void executeSuccessAndGetInvalidPoint() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_warehouses/delivery_get_reference_warehouses.xml",
            "fixtures/response/delivery/get_reference_warehouses/delivery_get_reference_warehouses_with_invalid_warehouse.xml"
        );

        GetReferenceWarehousesRequest request = new GetReferenceWarehousesRequest(
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    @Test(expected = NestedServletException.class)
    public void executeAndGetEmptyWarehouses() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/delivery/get_reference_warehouses/delivery_get_reference_warehouses.xml",
            "fixtures/response/delivery/get_reference_warehouses/delivery_get_reference_warehouses_empty.xml"
        );

        GetReferenceWarehousesRequest request = new GetReferenceWarehousesRequest(
            CommonDtoFactory.createPartner()
        );

        executeScenario(request);
    }

    private void executeScenario(GetReferenceWarehousesRequest request) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/delivery/getReferenceWarehouses")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest);

        String expectedJson = getFileContent("fixtures/response/delivery/get_reference_warehouses/delivery_get_reference_warehouses.json");

        String actualJson = mockMvc.perform(requestBuilder)
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }
}
