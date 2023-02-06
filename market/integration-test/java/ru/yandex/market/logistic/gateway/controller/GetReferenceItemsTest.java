package ru.yandex.market.logistic.gateway.controller;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetReferenceItemsRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_REFERENCE_ITEMS_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetReferenceItemsTest extends AbstractIntegrationTest {
    private final static long TEST_PARTNER_ID = 145L;

    private final static int TEST_LIMIT = 20;

    private final static int TEST_OFFSET = 0;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_REFERENCE_ITEMS_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessUnitIds() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_reference_items/fulfillment_get_reference_items_unitids.xml",
            "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_unitids.xml"
        );

        List<UnitId> unitIds = Arrays.asList(
            new UnitId("id0", 0L, "article0"),
            new UnitId("id1", 1L, "article1")
        );

        GetReferenceItemsRequest request = new GetReferenceItemsRequest(
            null,
            null,
            unitIds,
            new Partner(TEST_PARTNER_ID)
        );
        executeScenario(
            request,
            "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_unitids.json"
        );
    }

    @Test
    public void executeSuccessLimits() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_reference_items/fulfillment_get_reference_items_limits.xml",
            "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_limits.xml"
        );

        GetReferenceItemsRequest request = new GetReferenceItemsRequest(
            TEST_LIMIT,
            TEST_OFFSET,
            null,
            new Partner(TEST_PARTNER_ID)
        );
        executeScenario(
            request,
            "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_limits.json"
        );
    }

    @Test
    public void executeSuccessWithItemElementInside() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_reference_items/fulfillment_get_reference_items_unitids.xml",
            "fixtures/response/fulfillment_get_reference_items_with_item_element.xml"
        );

        List<UnitId> unitIds = Arrays.asList(
            new UnitId("id0", 0L, "article0"),
            new UnitId("id1", 1L, "article1")
        );

        GetReferenceItemsRequest request = new GetReferenceItemsRequest(
            null,
            null,
            unitIds,
            new Partner(TEST_PARTNER_ID)
        );
        executeScenario(request, "fixtures/response/fulfillment_get_reference_items_with_item_element.json");
    }

    @Test
    public void executeSuccessWithoutElements() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_reference_items/fulfillment_get_reference_items_empty.xml",
            "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_empty.xml"
        );

        List<UnitId> unitIds = List.of(new UnitId("id9999", 0L, "article0"));

        GetReferenceItemsRequest request = new GetReferenceItemsRequest(
            null,
            null,
            unitIds,
            new Partner(TEST_PARTNER_ID)
        );
        executeScenario(request, "fixtures/response/fulfillment_get_reference_items_empty.json");
    }

    @Test
    public void executeSuccessWithOneEmptyItemReferenceSucceeded() throws Exception {
        prepareMockServerXmlScenario(
                mockServer,
                "https://localhost/query-gateway",
                "fixtures/request/fulfillment/get_reference_items/fulfillment_get_reference_items_empty.xml",
                "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_list_with_one_empty_item_reference.xml"
        );

        List<UnitId> unitIds = List.of(new UnitId("id9999", 0L, "article0"));

        GetReferenceItemsRequest request = new GetReferenceItemsRequest(
                null,
                null,
                unitIds,
                new Partner(TEST_PARTNER_ID)
        );
        executeScenario(request,
                "fixtures/response/fulfillment/get_reference_items/fulfillment_get_reference_items_list_with_one_empty_item_reference.json");
    }

    private void executeScenario(GetReferenceItemsRequest request, String expectedJsonPath) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/fulfillment/getReferenceItems")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest);

        String expectedJson = getFileContent(expectedJsonPath);

        String actualJson = mockMvc.perform(requestBuilder)
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("Asserting that JSON response is correct")
            .isTrue();
    }

}
