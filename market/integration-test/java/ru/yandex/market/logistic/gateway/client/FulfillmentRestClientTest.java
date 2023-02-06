package ru.yandex.market.logistic.gateway.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.ResponseCreator;

import ru.yandex.market.logistic.gateway.client.config.ClientTestConfig;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.ConsolidatedTransactionService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.DataExchangeService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.InboundService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.InboundXDocService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.OrderService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.OutboundService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.ShipmentService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.StocksService;
import ru.yandex.market.logistic.gateway.client.service.fulfillment.TransferService;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetInboundStatusHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetInboundStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetOutboundStatusHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.common.response.GetOutboundStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetConsolidatedTransactionsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetExpirationItemsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOrderResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOrdersStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetOutboundsStatusResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetReferenceItemsResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetStocksResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetTransactionsOrdersResponse;
import ru.yandex.market.logistic.gateway.utils.JsonMatcher;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;
import static ru.yandex.market.logistics.util.client.HttpTemplate.USER_TICKET_HEADER;

public class FulfillmentRestClientTest extends AbstractRestTest {

    private FulfillmentClient fulfillmentClient;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        fulfillmentClient = new FulfillmentClientImpl(
            new DataExchangeService(null, httpTemplate),
            new InboundService(null, httpTemplate),
            new OrderService(null, httpTemplate),
            new OutboundService(null, httpTemplate),
            new StocksService(httpTemplate),
            new TransferService(null, httpTemplate),
            new InboundXDocService(httpTemplate),
            new ShipmentService(null),
            new ConsolidatedTransactionService(httpTemplate),
            null
        );
    }

    @Test
    public void getStocksLimits() throws IOException {
        executePositiveScenario(
            "fulfillment/getStocks",
            "fulfillment/get_stocks/get_stocks_limits_request.json",
            "fulfillment/get_stocks/get_stocks_limits_response.json",
            GetStocksResponse.class,
            () -> fulfillmentClient.getStocks(10, 20, new Partner(20L)),
            (expectedResponse, actualResult) ->
                assertions.assertThat(actualResult)
                    .as("Asserting that the stocks list is correct")
                    .isEqualTo(expectedResponse.getItemStocksList())
        );
    }

    @Test
    public void getStocksUnitIds() throws IOException {
        executePositiveScenario(
            "fulfillment/getStocks",
            "fulfillment/get_stocks/get_stocks_unitids_request.json",
            "fulfillment/get_stocks/get_stocks_unitids_response.json",
            GetStocksResponse.class,
            () -> fulfillmentClient.getStocks(
                Arrays.asList(new UnitId(null, 1111L, "41111"), new UnitId(null, 1111L, "41127")),
                new Partner(20L)
            ),
            (expectedResponse, actualResult) ->
                assertions.assertThat(actualResult)
                    .as("Asserting that the stocks list is correct")
                    .isEqualTo(expectedResponse.getItemStocksList())
        );
    }

    @Test
    public void getExpirationItemsLimits() throws IOException {
        executePositiveScenario(
            "fulfillment/getExpirationItems",
            "fulfillment/get_expiration_items/get_expiration_items_limits_request.json",
            "fulfillment/get_expiration_items/get_expiration_items_limits_response.json",
            GetExpirationItemsResponse.class,
            () -> fulfillmentClient.getExpirationItems(10, 20, new Partner(20L)),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult).isNotNull();
                assertions.assertThat(actualResult).isEqualTo(expectedResponse.getItemExpirationList());
            }
        );
    }

    @Test
    public void getExpirationItemsUnitIds() throws IOException {
        executePositiveScenario(
            "fulfillment/getExpirationItems",
            "fulfillment/get_expiration_items/get_expiration_items_unitids_request.json",
            "fulfillment/get_expiration_items/get_expiration_items_unitids_response.json",
            GetExpirationItemsResponse.class,
            () -> fulfillmentClient.getExpirationItems(
                Arrays.asList(new UnitId(null, 1111L, "41111"), new UnitId(null, 1111L, "41127")),
                new Partner(20L)
            ),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult).isNotNull();
                assertions.assertThat(actualResult).isEqualTo(expectedResponse.getItemExpirationList());
            }
        );
    }

    @Test
    public void getReferenceItemsLimits() throws Exception {
        Partner partner = new Partner(42L);
        executePositiveScenario(
            "fulfillment/getReferenceItems",
            "fulfillment/get_reference_items/get_reference_items_limits_request.json",
            "fulfillment/get_reference_items/get_reference_items_limits_response.json",
            GetReferenceItemsResponse.class,
            () -> fulfillmentClient.getReferenceItems(5, 0, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult).isNotNull();
                assertions.assertThat(actualResult).isEqualTo(expectedResponse.getItemReferences());
            }
        );
    }

    @Test
    public void getReferenceItemsUnitIds() throws Exception {
        List<UnitId> unitIds = Arrays.asList(
            new UnitId("id0", 0L, "article0"),
            new UnitId("id1", 1L, "article1")
        );
        Partner partner = new Partner(42L);
        executePositiveScenario(
            "fulfillment/getReferenceItems",
            "fulfillment/get_reference_items/get_reference_items_unitids_request.json",
            "fulfillment/get_reference_items/get_reference_items_unitids_response.json",
            GetReferenceItemsResponse.class,
            () -> fulfillmentClient.getReferenceItems(unitIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult).isNotNull();
                assertions.assertThat(actualResult).isEqualTo(expectedResponse.getItemReferences());
            }
        );
    }

    @Test
    public void getOutboundsStatus() throws Exception {
        List<ResourceId> outboundsId = Collections.singletonList(ResourceId.builder().setYandexId("123").setPartnerId("ABC123").build());
        Partner partner = new Partner(42L);
        executePositiveScenario(
            "fulfillment/getOutboundsStatus",
            "fulfillment/get_outbounds_status/get_outbounds_status_request.json",
            "fulfillment/get_outbounds_status/get_outbounds_status_response.json",
            GetOutboundsStatusResponse.class,
            () -> fulfillmentClient.getOutboundsStatus(outboundsId, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult).isNotNull();
                assertions.assertThat(actualResult).isEqualTo(expectedResponse.getOutboundsStatus());
            }
        );
    }

    @Test
    public void getOrderHistory() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "fulfillment/getOrderHistory",
            "fulfillment/get_order_history/request.json",
            "fulfillment/get_order_history/response.json",
            GetOrderHistoryResponse.class,
            () -> fulfillmentClient.getOrderHistory(orderId, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not null")
                    .isNotNull();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .isEqualToComparingFieldByFieldRecursively(expectedResponse.getOrderStatusHistory());
            }
        );
    }

    @Test
    public void getOrdersStatus() throws Exception {
        ResourceId orderId = ResourceId.builder().setYandexId("12345").setPartnerId("ABC12345").build();
        Partner partner = new Partner(654L);
        executePositiveScenario(
            "fulfillment/getOrdersStatus",
            "fulfillment/get_orders_status/request.json",
            "fulfillment/get_orders_status/response.json",
            GetOrdersStatusResponse.class,
            () -> fulfillmentClient.getOrdersStatus(Collections.singletonList(orderId), partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getOrderStatusHistories());
            }
        );
    }

    @Test
    public void getTransactionsOrdersByOrderId() throws Exception {
        executePositiveScenario(
            "fulfillment/getTransactionsOrders",
            "fulfillment/get_transactions_orders/by_order_id/request.json",
            "fulfillment/get_transactions_orders/by_order_id/response.json",
            GetTransactionsOrdersResponse.class,
            () -> fulfillmentClient.getTransactionsOrders(
                ResourceId.builder().setYandexId("123").build(),
                10,
                0,
                new Partner(145L)
            ),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getTransactions());
            }
        );
    }

    @Test
    public void getTransactionsOrdersByInterval() throws Exception {
        executePositiveScenario(
            "fulfillment/getTransactionsOrders",
            "fulfillment/get_transactions_orders/by_interval/request.json",
            "fulfillment/get_transactions_orders/by_interval/response.json",
            GetTransactionsOrdersResponse.class,
            () -> fulfillmentClient.getTransactionsOrders(
                DateTimeInterval.fromFormattedValue("2019-08-07T00:00:00+03:00/2019-08-10T00:00:00+03:00"),
                10,
                0,
                new Partner(145L)
            ),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getTransactions());
            }
        );
    }

    @Test
    public void getConsolidatedTransactions() throws Exception {
        executePositiveScenario(
            "fulfillment/getConsolidatedTransactions",
            "fulfillment/get_consolidated_transactions/request.json",
            "fulfillment/get_consolidated_transactions/response.json",
            GetConsolidatedTransactionsResponse.class,
            () -> fulfillmentClient.getConsolidatedTransactions(
                DateTimeInterval.fromFormattedValue("2019-08-07T00:00:00+03:00/2019-08-10T00:00:00+03:00"),
                10,
                0,
                new Partner(145L)
            ),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getTransactions());
            }
        );
    }

    @Test
    public void getOrder() throws Exception {
        executePositiveScenario(
            "fulfillment/getOrder",
            "fulfillment/get_order/request.json",
            "fulfillment/get_order/response.json",
            GetOrderResponse.class,
            () -> fulfillmentClient.getOrderSync(
                ResourceId.builder().setYandexId("100").build(),
                new Partner(145L)
            ),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not null")
                    .isNotNull();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse.getOrder());
            }
        );
    }

    @Test
    public void getInboundStatus() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "fulfillment/getInboundStatus",
            "common/get_inbound_status/request.json",
            "common/get_inbound_status/response.json",
            GetInboundStatusResponse.class,
            () -> fulfillmentClient.getInboundStatus(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getInboundStatuses());
            }
        );
    }

    @Test
    public void getInboundStatusHistory() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "fulfillment/getInboundStatusHistory",
            "common/get_inbound_status_history/request.json",
            "common/get_inbound_status_history/response.json",
            GetInboundStatusHistoryResponse.class,
            () -> fulfillmentClient.getInboundStatusHistory(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getInboundStatusHistories());
            }
        );
    }

    @Test
    public void getOutboundStatus() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "fulfillment/getOutboundStatus",
            "common/get_outbound_status/request.json",
            "common/get_outbound_status/response.json",
            GetOutboundStatusResponse.class,
            () -> fulfillmentClient.getOutboundStatus(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getOutboundStatuses());
            }
        );
    }

    @Test
    public void getOutboundStatusHistory() throws Exception {
        List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> resourceIds = createEntityResourceIds();
        Partner partner = new Partner(145L);
        executePositiveScenario(
            "fulfillment/getOutboundStatusHistory",
            "common/get_outbound_status_history/request.json",
            "common/get_outbound_status_history/response.json",
            GetOutboundStatusHistoryResponse.class,
            () -> fulfillmentClient.getOutboundStatusHistory(resourceIds, partner),
            (expectedResponse, actualResult) -> {
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is not empty")
                    .isNotEmpty();
                assertions.assertThat(actualResult)
                    .as("Asserting that actual result is correct")
                    .hasSameElementsAs(expectedResponse.getOutboundStatusHistories());
            }
        );
    }

    private List<ru.yandex.market.logistic.gateway.common.model.common.ResourceId> createEntityResourceIds() {
        return Arrays.asList(
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("111424")
                .setPartnerId("111525")
                .build(),
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("222424")
                .setPartnerId("222525")
                .build(),
            ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder()
                .setYandexId("333424")
                .setPartnerId("333525")
                .build()
        );
    }

    private <ExpectedResponseType, ActualResultType> void executePositiveScenario(
        String method,
        String expectedRequestPath,
        String responsePath,
        Class<ExpectedResponseType> responseTypeClass,
        Supplier<ActualResultType> clientCall,
        BiConsumer<ExpectedResponseType, ActualResultType> responseMatchingFunction
    ) throws IOException {
        ResponseCreator taskResponseCreator = withStatus(OK)
            .contentType(APPLICATION_JSON)
            .body(getFileContent(responsePath));

        mock.expect(requestTo(uri + "/" + method))
            .andExpect(content().string(JsonMatcher.getMatcherFunction()
                .apply(getFileContent(expectedRequestPath))))
            .andExpect(header(SERVICE_TICKET_HEADER, ClientTestConfig.TVM_SERVICE_TICKET))
            .andExpect(header(USER_TICKET_HEADER, ClientTestConfig.TVM_USER_TICKET))
            .andRespond(taskResponseCreator);

        ActualResultType actualResult = clientCall.get();
        ExpectedResponseType expectedResponse =
            objectMapper.readValue(getFileContent(responsePath), responseTypeClass);

        responseMatchingFunction.accept(expectedResponse, actualResult);
    }
}
