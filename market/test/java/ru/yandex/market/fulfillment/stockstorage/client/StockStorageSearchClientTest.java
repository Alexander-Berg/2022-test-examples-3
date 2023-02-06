package ru.yandex.market.fulfillment.stockstorage.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Korobyte;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.Pagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuRequest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchTopSkuDepthResponse;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static java.util.Collections.singletonList;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientTests.buildUrl;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientTests.checkBody;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.SEARCH;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.SKU;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.TOP_DEPTH_SKU;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@ExtendWith(SpringExtension.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageSearchClientTest extends BaseIntegrationTest {

    public static final Pagination PAGINATION =
            Pagination.builder().withLimit(10).withOffset(20).withNoCount(true).withOffsetId(25L).build();
    public static final Pagination PAGINATION_OLD = Pagination.of(10, 20);
    public static final SSItem EXPECTED_UNIT_ID = SSItem.of("sku1", 2L, 3);
    private static final String FIXTURE_SKU_SEARCH = "fixture/sku/search/";
    @Value("${fulfillment.stockstorage.api.host:}")
    private String host;
    @Autowired
    private StockStorageClientConfiguration configuration;
    private MockRestServiceServer mockServer;
    @Autowired
    private StockStorageSearchClient client;

    @BeforeEach
    public void init() {
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void positiveRequestSkuSearchFirstConstructor() {
        SearchSkuFilter expectedFilter = SearchSkuFilter.builder(
                singletonList(EXPECTED_UNIT_ID))
                .withEnabled(false)
                .withUpdatable(true)
                .withSkipWithEmptyStocks(true)
                .withStockType(SSStockType.DEFECT)
                .build();
        positiveRequestSkuSearch(expectedFilter, "1st_constructor", PAGINATION);
    }

    @Test
    public void positiveRequestSkuSearchOldFirstConstructor() {
        SearchSkuFilter expectedFilter = SearchSkuFilter.of(
                singletonList(EXPECTED_UNIT_ID),
                false,
                true,
                true);
        positiveRequestSkuSearch(expectedFilter, "1st_old_constructor", PAGINATION_OLD);
    }

    @Test
    public void positiveRequestSkuSearchSecondConstructor() {

        SearchSkuFilter expectedFilter = SearchSkuFilter.builder(
                5L,
                10)
                .withEnabled(false)
                .withUpdatable(true)
                .withSkipWithEmptyStocks(true)
                .withStockType(SSStockType.DEFECT)
                .build();
        positiveRequestSkuSearch(expectedFilter, "2nd_constructor", PAGINATION);
    }

    @Test
    public void positiveRequestSkuSearchSecondOldConstructor() {

        SearchSkuFilter expectedFilter = SearchSkuFilter.of(
                5L,
                10,
                false,
                true,
                true);
        positiveRequestSkuSearch(expectedFilter, "2nd_old_constructor", PAGINATION_OLD);
    }

    @Test
    public void positiveRequestSkuSearchEmptyConstructors() {

        SearchSkuFilter expectedFilter = SearchSkuFilter.empty();
        positiveRequestSkuSearch(expectedFilter, "empty_constructors", Pagination.empty());
    }

    private void positiveRequestSkuSearch(SearchSkuFilter expectedFilter, String scenario, Pagination pagination) {
        mockServer.expect(requestTo(buildUrl(host, SEARCH + SKU)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_SKU_SEARCH + "positive_request_" + scenario + ".json")))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_SKU_SEARCH + "positive_response_" + scenario + ".json")));

        SearchSkuResponse response = client.searchSku(SearchSkuRequest.of(
                expectedFilter,
                pagination
        ));


        List<Sku> skus = response.getSkus();
        softly.assertThat(skus).hasSize(1);

        Sku actualSku = skus.get(0);

        softly.assertThat(actualSku.getUnitId()).isEqualTo(EXPECTED_UNIT_ID);
        softly.assertThat(actualSku.getKorobyte()).isEqualTo(
                new Korobyte(
                        5,
                        10,
                        20,
                        BigDecimal.valueOf(30),
                        BigDecimal.valueOf(40),
                        BigDecimal.valueOf(50)
                )
        );
        softly.assertThat(actualSku.getFfAvailable()).isEqualTo(300);
        softly.assertThat(actualSku.isEnabled()).isTrue();
        softly.assertThat(actualSku.isUpdatable()).isFalse();
        softly.assertThat(actualSku.getRefilled())
                .isEqualTo(OffsetDateTime.of(
                        2018,
                        4,
                        18,
                        9,
                        0,
                        0,
                        0,
                        ZoneOffset.ofHours(3))
                );


        softly.assertThat(actualSku.getStocks())
                .containsExactlyInAnyOrder(
                        Stock.of(100, 0, 101, "FIT"),
                        Stock.of(200, 0, 202, "DEFECT"),
                        Stock.of(300, 0, 303, "QUARANTINE")
                );

        softly.assertThat(response.getPagination()).isEqualTo(
                ResultPagination
                        .builder()
                        .withLimit(pagination.getLimit())
                        .withOffset(pagination.getOffset())
                        .withLastId(39L)
                        .withResultAmount(1)
                        .withTotalAmount(1)
                        .build()
        );

        SearchSkuFilter filter = response.getFilter();

        softly.assertThat(filter).isEqualTo(expectedFilter);

        mockServer.verify();
    }

    @Test
    public void positiveRequestTopSkuSearch() {
        String expectedUri = buildUrl(host, SEARCH + TOP_DEPTH_SKU + "?limit=1&stockType=PREORDER&warehouseId");
        mockServer.expect(requestTo(expectedUri))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\n" +
                                "  \"stockType\": 60,\n" +
                                "  \"stocks\": [\n" +
                                "    {\n" +
                                "      \"sku\": \"station-1\",\n" +
                                "      \"vendorId\": 10263850,\n" +
                                "      \"shopSku\": \"station-1\",\n" +
                                "      \"quantity\": 700000000,\n" +
                                "      \"warehouseId\": 1\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"limit\": 1\n" +
                                "}"));
        SearchTopSkuDepthResponse response = client.getTopDepthSku(1, StockType.PREORDER, null);
        mockServer.verify();

        softly.assertThat(response.getLimit()).isEqualTo(1);
        softly.assertThat(response.getStocks().size()).isEqualTo(1);
        softly.assertThat(response.getStockType().getCode()).isEqualTo(60);
    }
}
