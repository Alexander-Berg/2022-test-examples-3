package ru.yandex.market.ff4shops.service;

import java.math.BigDecimal;
import java.util.List;

import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.idx.IndexerApiClient;
import ru.yandex.market.common.idx.model.Dimensions;
import ru.yandex.market.common.idx.model.FeedOfferId;
import ru.yandex.market.common.idx.model.SupplierFeedOfferIds;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.delivery.dimension.DeliveryDimensionsService;
import ru.yandex.market.ff4shops.delivery.stocks.StocksService;
import ru.yandex.market.ff4shops.environment.EnvironmentService;
import ru.yandex.market.ff4shops.partner.service.StocksByPiExperiment;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.request.Token;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetReferenceItemsRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetReferenceItemsResponse;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Тестируется через сервис, а не контроллер потому что нужно находится в том же потоки при подсчете запросов.
 */
@DbUnitDataSet(before = "QueryCountTest.csv")
public class QueryCountTest extends FunctionalTest {
    @Autowired
    private DeliveryDimensionsService deliveryDimensionsService;

    @Autowired
    private StocksService unitedStocksService;

    @Autowired
    @Qualifier("indexerApiClient")
    private IndexerApiClient indexerApiClient;

    @Autowired
    @Qualifier("pushApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    @Autowired
    @Value("${market.checkout.pushapi.url}")
    private String pushApiUrl;

    @Autowired
    private StocksByPiExperiment stocksByPiExperiment;

    @Autowired
    private EnvironmentService environmentService;

    private MockRestServiceServer pushApiMockRestServiceServer;

    @BeforeEach
    public void initMock() {
        pushApiMockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
        stocksByPiExperiment.resetCachingVariables();
        environmentService.setValue(StocksByPiExperiment.STOCKS_BY_NEW_FLAG_VAR, "false");
    }

    @Test
    void testCountQueryReferenceItems() {
        QueryCountHolder.clear();
        expectRequestToIndexerByItems();
        RequestWrapper<GetReferenceItemsRequest> request = new RequestWrapper<>();
        request.setToken(new Token("ff_token_1"));
        GetReferenceItemsRequest content = new GetReferenceItemsRequest(
                null,
                null,
                List.of(
                        new UnitId("AAA", 100L, "AAA"),
                        new UnitId("BBB", 100L, "BBB"),
                        new UnitId("VVV", 100L, "VVV")
                )
        );
        request.setRequest(content);
        ResponseWrapper<GetReferenceItemsResponse> response = deliveryDimensionsService.getReferenceItems(request, 10);

        assertEquals(3, response.getResponse().getItemReferences().size());
        // сохранение результатов ответа batch saveAll dimensions
        assertEquals(0, QueryCountHolder.getGrandTotal().getInsert());

        // поиск по идентификатору склада постащика
        assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
    }

    private void expectRequestToIndexerByItems() {
        when(indexerApiClient.getDimensions(eq(new SupplierFeedOfferIds(100L, 145L, List.of(
                new FeedOfferId(500L, "AAA"),
                new FeedOfferId(500L, "BBB"),
                new FeedOfferId(500L, "VVV")
        )))))
                .thenReturn(List.of(
                        new Dimensions(500, "AAA",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(20001, 3)),
                        new Dimensions(500, "BBB",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(1, 3)),
                        new Dimensions(500, "VVV",
                                BigDecimal.valueOf(1001, 3),
                                BigDecimal.valueOf(21001, 3),
                                BigDecimal.valueOf(10001, 3),
                                BigDecimal.valueOf(1, 3))));
    }

    @Test
    void testCountQueryStocks() {
        QueryCountHolder.clear();
        expectRequestToPushApi();

        QueryCountHolder.clear();
        expectRequestToIndexerByItems();

        RequestWrapper<GetStocksRequest> request = new RequestWrapper<>();
        request.setToken(new Token("ff_token_1"));
        GetStocksRequest content = new GetStocksRequest(
                null,
                null,
                List.of(
                        new UnitId("AAA", 100L, "AAA"),
                        new UnitId("BBB", 99L, "BBB"),
                        new UnitId("VVV", 95L, "VVV")
                )
        );
        request.setRequest(content);
        ResponseWrapper<GetStocksResponse> response = unitedStocksService.getStocks(request, 10);
        assertEquals(3, response.getResponse().getItemStocksList().size());
        // сохранение статусов поставщика stocksRequestStatusService updateTimeOfRequestStocks
        assertEquals(0, QueryCountHolder.getGrandTotal().getInsert());
        // поиск по идентификатору склада постащика
        // 1 запрос на склад, 3 запроса на поиск оферов трех поставщиков
        // (+3 запроса флага на поход в ЛБ в тестах без кеша)
        // +1 флаг переключения на stocks_by_partner_interface
        // + 1 запрос на поиск второстепенных складов в группе
        assertEquals(9, QueryCountHolder.getGrandTotal().getSelect());
    }

    private void expectRequestToPushApi() {
        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                requestTo(String.format("%s/shops/100/stocks?context=MARKET&apiSettings=PRODUCTION&logResponse=false",
                        pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess( //language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"AAA\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                requestTo(String.format("%s/shops/95/stocks?context=MARKET&apiSettings=PRODUCTION&logResponse=false",
                        pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess( //language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"AAA\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));

        pushApiMockRestServiceServer.expect(ExpectedCount.manyTimes(),
                requestTo(String.format("%s/shops/99/stocks?context=MARKET&apiSettings=PRODUCTION&logResponse=false",
                        pushApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(//language=xml
                        "" +
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<stocksResponse>" +
                                "<stocks>" +
                                "<stock sku=\"CCC\" warehouseId=\"1\">" +
                                "<items>" +
                                "<item count=\"3\" type=\"FIT\" updatedAt=\"2018-12-11T17:44:08+03:00\" />" +
                                "</items>" +
                                "</stock>" +
                                "</stocks>" +
                                "</stocksResponse>", MediaType.APPLICATION_XML));
    }
}
