package ru.yandex.market.logistic.gateway.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetTransactionsOrdersRequest;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistic.api.model.common.PartnerMethod.GET_TRANSACTIONS_ORDERS_FF;
import static ru.yandex.market.logistic.gateway.utils.CommonDtoFactory.createPartner;

@DatabaseSetup("classpath:repository/state/partners_properties.xml")
public class GetTransactionsOrdersFulfillmentTest extends AbstractIntegrationTest {

    private static final ResourceId ORDER_ID = ResourceId.builder().setYandexId("123").build();
    private static final DateTimeInterval DATE_TIME_INTERVAL =
        DateTimeInterval.fromFormattedValue("2019-08-07/2019-08-10");
    private static final Integer TRANSACTION_LIMIT = 1;
    private static final Integer TRANSACTION_OFFSET = 0;

    private MockRestServiceServer mockServer;

    @Before
    public void setup() throws Exception {
        mockServer = createMockServerByRequest(GET_TRANSACTIONS_ORDERS_FF);
    }

    @After
    public void tearDown() {
        mockServer.verify();
    }

    @Test
    public void executeWithOrderId() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_orderid.xml",
            "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.xml"
        );

        GetTransactionsOrdersRequest request = new GetTransactionsOrdersRequest(
            ORDER_ID, null, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.json"
        );
    }

    @Test
    public void executeWithInterval() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_interval.xml",
            "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.xml"
        );

        GetTransactionsOrdersRequest request = new GetTransactionsOrdersRequest(
            null, DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders.json"
        );
    }

    @Test
    public void executeWithEmptyParamsInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_interval.xml",
            "fixtures/response/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_empty_params.xml"
        );

        GetTransactionsOrdersRequest request = new GetTransactionsOrdersRequest(
            null, DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_transactions_orders/" +
                "fulfillment_get_transactions_orders_empty_params.json"
        );
    }

    @Test
    public void executeWithEmptyParamsWithoutTagInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_interval.xml",
            "fixtures/response/fulfillment/get_transactions_orders/" +
                "fulfillment_get_transactions_orders_empty_params_without_tag.xml"
        );

        GetTransactionsOrdersRequest request = new GetTransactionsOrdersRequest(
            null, DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_transactions_orders/" +
                "fulfillment_get_transactions_orders_empty_params.json"
        );
    }

    @Test
    public void executeWithEmptyTransactionsInResponse() throws Exception {
        prepareMockServerXmlScenario(
            mockServer,
            "https://localhost/query-gateway",
            "fixtures/request/fulfillment/get_transactions_orders/fulfillment_get_transactions_orders_interval.xml",
            "fixtures/response/fulfillment/get_transactions_orders/" +
                "fulfillment_get_transactions_orders_empty_transactions.xml"
        );

        GetTransactionsOrdersRequest request = new GetTransactionsOrdersRequest(
            null, DATE_TIME_INTERVAL, TRANSACTION_LIMIT, TRANSACTION_OFFSET, createPartner()
        );

        executeScenario(
            request,
            "fixtures/response/fulfillment/get_transactions_orders/" +
                "fulfillment_get_transactions_orders_empty_transactions.json"
        );
    }

    private void executeScenario(GetTransactionsOrdersRequest request, String jsonPath) throws Exception {
        String jsonRequest = jsonMapper.writeValueAsString(request);

        String expectedJson = getFileContent(jsonPath);
        String actualJson = mockMvc.perform(
            post("/fulfillment/getTransactionsOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest)
        )
            .andReturn()
            .getResponse()
            .getContentAsString();

        softAssert.assertThat(new JsonMatcher(expectedJson).matches(actualJson))
            .as("GetTransactionsOrders response should be correct")
            .isTrue();
    }
}
