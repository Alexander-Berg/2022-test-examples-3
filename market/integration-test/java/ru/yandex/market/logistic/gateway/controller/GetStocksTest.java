package ru.yandex.market.logistic.gateway.controller;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_STOCKS_FF;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetStocksTest extends AbstractIntegrationTest {

    private final static long TEST_PARTNER_ID = 145L;

    private final static int TEST_LIMIT = 10;

    private final static int TEST_OFFSET = 20;

    private final static String UNIQ = "64c9579de53cb19afe9f96edb8c59fca";

    private MockRestServiceServer mockServer;

    @Before
    public void setup() {
        mockServer = createMockServerByRequest(GET_STOCKS_FF);
        when(uniqService.generate()).thenReturn(UNIQ);
    }

    @After
    public void tearDown() throws IOException, ServletException {
        mockServer.verify();
    }

    @Test
    public void executeSuccessLimits() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_stocks/fulfillment_get_stocks_limits.xml",
            "fixtures/response/fulfillment/get_stocks/fulfillment_get_stocks_limits.xml");

        GetStocksRequest request = new GetStocksRequest(TEST_LIMIT, TEST_OFFSET, null, new Partner(TEST_PARTNER_ID));
        assertRequestExecutedAsExpected(request, "fixtures/response/fulfillment/get_stocks/fulfillment_get_stocks_limits.json");
    }

    @Test
    public void executeSuccessUnitIds() throws Exception {
        prepareMockServerXmlScenario(mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_stocks/fulfillment_get_stocks_unitids.xml",
            "fixtures/response/fulfillment/get_stocks/fulfillment_get_stocks_unitids.xml");

        GetStocksRequest request = new GetStocksRequest(null, null, createUnitIds(), new Partner(TEST_PARTNER_ID));
        assertRequestExecutedAsExpected(request, "fixtures/response/fulfillment/get_stocks/fulfillment_get_stocks_unitids.json");
    }

    private void assertRequestExecutedAsExpected(GetStocksRequest request, String expectedPath) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);
        String jsonResponse = mockMvc.perform(post("/fulfillment/getStocks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String expectedJson = IOUtils.toString(getSystemResourceAsStream(expectedPath), "UTF-8");

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(jsonResponse))
            .as("Asserting that the JSON response is correct")
            .isTrue();
    }

    private List<UnitId> createUnitIds() {
        return Arrays.asList(
            new UnitId(null, 1111L, "41111"),
            new UnitId(null, 1111L, "41127")
        );
    }
}
