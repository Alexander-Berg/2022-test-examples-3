package ru.yandex.market.partner.mvc.controller.stock;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.SupplierSkuDto;
import ru.yandex.market.ff.client.dto.SupplierSkuItemCountDTO;
import ru.yandex.market.ff.client.dto.SupplierSkuItemsCountDTO;
import ru.yandex.market.ff.client.dto.UtilizationItemCountRequestDto;
import ru.yandex.market.ff.client.dto.WarehouseItemCountDTO;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Korobyte;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Тесты для {@link CurrentStocksOfferController}.
 */
@DbUnitDataSet(before = "currentStocksOfferControllerTest.before.csv")
class CurrentStocksOfferControllerTest extends FunctionalTest {

    @Autowired
    private StockStorageSearchClient stockStorageSearchClient;

    @Autowired
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi;

    @BeforeEach
    void init() {
        willReturn(SearchSkuResponse.of(Collections.emptyList(), ResultPagination.builder().build(),
                SearchSkuFilter.builder().build()))
                .given(stockStorageSearchClient).searchSku(any());
    }

    @Test
    void checkEmptyStockSS() {
        willReturn(SearchSkuResponse.of(
                List.of(
                        Sku.builder()
                                .withUnitId(SSItem.of("teabag1", 0, 104))
                                .withRefilled(OffsetDateTime.now())
                                .withStocks(List.of())
                                .build()
                ),
                ResultPagination.builder().build(),
                SearchSkuFilter.builder().build()
        )).given(stockStorageSearchClient).searchSku(any());
        long campaignId = 112L;
        final String url = stockUrl(campaignId, "OFFER_ID");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(response, getClass(), "currentStocksOfferController.emptyStocks.response.json");
    }

    @Test
    void checkNotLinkedWarehousesSS() {
        long campaignId = 111L;
        final String url = stockUrl(campaignId, "OFFER_ID");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        String expectedResponse = "{" +
                "  \"measurements\": null," +
                "  \"warehouses\": []" +
                "}";
        JsonTestUtil.assertEquals(response, expectedResponse);
        verifyNoInteractions(stockStorageSearchClient);
    }

    @Test
    void checkSS_stockStorageRequestFails_endpointReturns500() {
        long campaignId = 113L;
        final String url = stockUrl(campaignId, "OFFER_ID");
        willThrow(new RuntimeException()).given(stockStorageSearchClient).searchSku(any());
        assertThrows(HttpServerErrorException.InternalServerError.class, () -> FunctionalTestHelper.get(url));
    }

    @Test
    void checkSS() {
        willReturn(SearchSkuResponse.of(
                List.of(
                        Sku.builder()
                                .withUnitId(SSItem.of("teabag1", 13, 101))
                                .withKorobyte(new Korobyte(2, 3, 1, new BigDecimal("0.999")))
                                .withRefilled(OffsetDateTime.now())
                                .withStocks(List.of(
                                        Stock.of(10, 4, 10, "FIT"),
                                        Stock.of(1, 0, 0, "EXPIRED"),
                                        Stock.of(30, 0, 0, "DEFECT"),
                                        Stock.of(20, 0, 0, "QUARANTINE")
                                ))
                                .build(),
                        Sku.builder()
                                .withUnitId(SSItem.of("teabag1", 13, 102))
                                .withKorobyte(new Korobyte(2, 3, 1, new BigDecimal("0.999")))
                                .withRefilled(OffsetDateTime.now())
                                .withStocks(List.of(
                                        Stock.of(10, 5, 10, "FIT"),
                                        Stock.of(2, 0, 0, "EXPIRED"),
                                        Stock.of(31, 0, 0, "DEFECT"),
                                        Stock.of(21, 0, 0, "QUARANTINE")
                                ))
                                .build(),
                        Sku.builder()
                                .withUnitId(SSItem.of("teabag1", 13, 103))
                                .withKorobyte(new Korobyte(2, 3, 1, new BigDecimal("0.999")))
                                .withRefilled(OffsetDateTime.now())
                                .withStocks(List.of())
                                .build(),
                        Sku.builder()
                                .withUnitId(SSItem.of("teabag1", 13, 105))
                                .withRefilled(OffsetDateTime.now())
                                .withStocks(List.of())
                                .build()

                ),
                ResultPagination.builder().build(),
                SearchSkuFilter.builder().build()
        )).given(stockStorageSearchClient).searchSku(any());
        SupplierSkuItemsCountDTO fulfillmentWorkflowApiResponse = new SupplierSkuItemsCountDTO();
        fulfillmentWorkflowApiResponse.setSupplierSkuItemCountDTOs(List.of(new SupplierSkuItemCountDTO(
                13,
                "teabag1",
                List.of(
                        new WarehouseItemCountDTO(101, 3),
                        new WarehouseItemCountDTO(102, 17)
                )
        )));
        willReturn(fulfillmentWorkflowApiResponse)
                .given(fulfillmentWorkflowClientApi)
                .findUtilizationTransferItemsCount(
                        refEq(UtilizationItemCountRequestDto.builder()
                                .addSupplierSkuKey(new SupplierSkuDto(13L, "teabag1"))
                                .build())
                );
        long campaignId = 113L;
        final String url = stockUrl(campaignId, "teabag1");
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(response, getClass(), "currentStocksOfferControllerTest.response.direct.json");
    }

    private String stockUrl(long campaignId, String offerId) {
        return baseUrl + String.format("/v1/campaigns/%d/offer/stocks?shopSku=%s", campaignId, offerId);
    }
}
