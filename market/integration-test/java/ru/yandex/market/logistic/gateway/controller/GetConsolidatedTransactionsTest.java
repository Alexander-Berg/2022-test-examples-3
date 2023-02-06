package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetConsolidatedTransactionsRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_CONSOLIDATED_TRANSACTIONS_FF;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetConsolidatedTransactionsTest extends AbstractIntegrationTest {

    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-07/2019-08-10");
    private static final Integer TRANSACTION_LIMIT = 1;
    private static final Integer TRANSACTION_OFFSET = 0;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws Exception {
        mockServer = createMockServerByRequest(GET_CONSOLIDATED_TRANSACTIONS_FF);
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
            "fixtures/request/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.xml",
            "fixtures/response/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.xml"
        );

        GetConsolidatedTransactionsRequest request = new GetConsolidatedTransactionsRequest(
            DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.json"
        );
    }

    @Test
    public void executeWithEmptyParamsInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.xml",
            "fixtures/response/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions_empty_params.xml"
        );

        GetConsolidatedTransactionsRequest request = new GetConsolidatedTransactionsRequest(
            DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_consolidated_transactions/" +
                "fulfillment_get_consolidated_transactions_empty_params.json"
        );
    }

    @Test
    public void executeWithEmptyParamsWithoutTagInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.xml",
            "fixtures/response/fulfillment/get_consolidated_transactions/" +
                "fulfillment_get_consolidated_transactions_empty_params_without_tag.xml"
        );

        GetConsolidatedTransactionsRequest request = new GetConsolidatedTransactionsRequest(
            DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_consolidated_transactions/" +
                "fulfillment_get_consolidated_transactions_empty_params.json"
        );
    }

    @Test
    public void executeWithEmptyTransactionsInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_consolidated_transactions/fulfillment_get_consolidated_transactions.xml",
            "fixtures/response/fulfillment/get_consolidated_transactions/" +
                "fulfillment_get_consolidated_transactions_empty_transactions.xml"
        );

        GetConsolidatedTransactionsRequest request = new GetConsolidatedTransactionsRequest(
            DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_consolidated_transactions/" +
                "fulfillment_get_consolidated_transactions_empty_transactions.json"
        );
    }

    private void executeScenario(GetConsolidatedTransactionsRequest request, String jsonPath) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent(jsonPath);
        String actualJson = mockMvc.perform(
            post("/fulfillment/getConsolidatedTransactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("GetConsolidatedTransactionsResponse should be correct")
            .isTrue();
    }
}
