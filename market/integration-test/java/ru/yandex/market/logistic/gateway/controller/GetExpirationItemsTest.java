package ru.yandex.market.logistic.gateway.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetExpirationItemsRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_EXPIRATION_ITEMS_FF;

/**
 * Интеграционный тест для метода getExpirationItems.
 */
@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetExpirationItemsTest extends AbstractIntegrationTest {

    private final static long TEST_PARTNER_ID = 145L;

    private final static int TEST_LIMIT = 10;

    private final static int TEST_OFFSET = 20;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws IOException {
        mockServer = createMockServerByRequest(GET_EXPIRATION_ITEMS_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeSuccessLimits() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_expiration_items/fulfillment_get_expiration_items_limits.xml",
            "fixtures/response/fulfillment/get_expiration_items/fulfillment_get_expiration_items_limits.xml");

        final GetExpirationItemsRequest request =
            new GetExpirationItemsRequest(TEST_LIMIT, TEST_OFFSET, null, new Partner(TEST_PARTNER_ID));
        executeScenario(request, "fixtures/response/fulfillment/get_expiration_items/fulfillment_get_expiration_items_limits.json");
    }

    @Test
    public void executeSuccessUnitIds() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_expiration_items/fulfillment_get_expiration_items_unitids.xml",
            "fixtures/response/fulfillment/get_expiration_items/fulfillment_get_expiration_items_unitids.xml");

        final GetExpirationItemsRequest request =
            new GetExpirationItemsRequest(null, null, createUnitIds(), new Partner(TEST_PARTNER_ID));
        executeScenario(request, "fixtures/response/fulfillment/get_expiration_items/fulfillment_get_expiration_items_unitids.json");
    }

    private void executeScenario(final GetExpirationItemsRequest request,
                                 final String expectedJsonPath) throws Exception {
        final String jsonRequest = jsonMapper.writeValueAsString(request);
        final String jsonResponse = mockMvc.perform(post("/fulfillment/getExpirationItems")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
            .andReturn()
            .getResponse()
            .getContentAsString();

        final String expectedJson = getFileContent(expectedJsonPath);

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(jsonResponse))
            .as("Asserting that the JSON response is correct")
            .isTrue();
    }

    private List<UnitId> createUnitIds() {
        return Arrays.asList(
            new UnitId(null, 1L, "iv3456"),
            new UnitId(null, 1L, "41127")
        );
    }
}
