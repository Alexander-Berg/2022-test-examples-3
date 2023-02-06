package ru.yandex.market.rg.asyncreport.stocks.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStockInfo;
import Market.DataCamp.DataCampUnitedOffer;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuRequest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertEquals;

@DbUnitDataSet(before = "SharedStocksTemplateServiceTest.before.csv")
class SharedStocksTemplateServiceTest extends FunctionalTest {

    @Autowired
    SharedStocksTemplateService sharedStocksTemplateService;

    @Autowired
    DataCampService dataCampService;

    @Autowired
    StockStorageSearchClient stockStorageSearchClient;

    @ParameterizedTest
    @CsvSource({"false, WorkbookForStocksWarehouseTestReport.expected.Test.xlsx",
            "true, FreezedStocksWarehouseTestReport.expected.Test.xlsx"})
    void generateWorkBook(boolean excludeFreeze, String expectedFilename) throws IOException, InvalidFormatException {
        Path tempFilePath = Files.createTempFile("WorkbookForStocksWarehouseTestReportTest", ".xlsx");
        File reportFile = new File(tempFilePath.toString());
        var businessId = 10L;
        var partnerIds = List.of(20L, 21L, 22L);
        var mainWarehouseId = 31;

        when(dataCampService.streamDataCampOffers(SearchBusinessOffersRequest.builder()
                .setBusinessId(businessId)
                .addIncludePartnerIds(partnerIds)
                .setIsFull(true)
                .setPageRequest(SeekSliceRequest.firstN(100))
                .build()))
                .thenReturn(
                        Stream.of(
                                //офер есть только на главном складе. со стоком 1
                                createOffer("five", Map.of(Pair.of(21, 31), 1L)),
                                //офер есть в двух партнерах. главный склад сток 0
                                createOffer("one", Map.of(Pair.of(20, 30), 5L, Pair.of(21, 31), 0L)),
                                //офер есть только на НЕ главном партнере. со стоком 1
                                createOffer("three", Map.of(Pair.of(20, 30), 1L)),
                                //офер есть в двух партнерах. главный склад сток 6
                                createOffer("two", Map.of(Pair.of(20, 30), 0L, Pair.of(21, 31), 6L))
                        ));
        if (excludeFreeze) {
            when(stockStorageSearchClient.searchSku(any())).thenReturn(SearchSkuResponse.of(
                    // 1 фриз, в файле должно быть 5
                    List.of(Sku.builder().withUnitId(SSItem.of("two", 21, 101))
                                    .withStocks(List.of(Stock.of(10, 1, 9, "FIT"))).build(),
                            // 1 фриз, для двух складов
                            Sku.builder().withUnitId(SSItem.of("one", 21, 101))
                                    .withStocks(List.of(Stock.of(10, 1, 9, "FIT"))).build()),
                    ResultPagination.builder().build(),
                    SearchSkuFilter.builder().build()
            ));
        }

        try (OutputStream out = new FileOutputStream(reportFile)) {
            sharedStocksTemplateService.generateWorkBook(out, businessId, partnerIds, mainWarehouseId, excludeFreeze);
        }
        XSSFWorkbook expected = new XSSFWorkbook(Objects.requireNonNull(
                SharedStocksTemplateServiceTest.class.getResourceAsStream(expectedFilename)));
        XSSFWorkbook actual = new XSSFWorkbook(reportFile);
        assertEquals(expected, actual, new HashSet<>());

        if (excludeFreeze) {
            ArgumentCaptor<SearchSkuRequest> capture = ArgumentCaptor.forClass(SearchSkuRequest.class);
            verify(stockStorageSearchClient, times(2)).searchSku(capture.capture());
            assertThat(capture.getAllValues())
                    .hasSize(2)
                    .allMatch(r -> r.getFilter().getWithFreezeOnly())
                    .anyMatch(r -> {
                        SearchSkuFilter filter = r.getFilter();
                        return filter.getVendorId() == 21 && filter.getWarehouseId() == 102;
                    })
                    .anyMatch(r -> {
                        SearchSkuFilter filter = r.getFilter();
                        return filter.getVendorId() == 22 && filter.getWarehouseId() == 103;
                    });
        }
        verifyNoMoreInteractions(stockStorageSearchClient);
    }

    /**
     * @param offerId id оферв
     * @param stocks  <<partnerId, warehouseId>, stock>
     */
    private DataCampUnitedOffer.UnitedOffer createOffer(String offerId, Map<Pair<Integer, Integer>, Long> stocks) {
        var actual = stocks.entrySet().stream()
                .map(s -> Pair.of(s.getKey().getFirst(),
                        DataCampUnitedOffer.ActualOffers.newBuilder()
                                .putWarehouse(s.getKey().getSecond(), DataCampOffer.Offer.newBuilder()
                                        .setIdentifiers(
                                                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                                        .setShopId(s.getKey().getFirst())
                                                        .setWarehouseId(s.getKey().getSecond())
                                                        .setOfferId(offerId)
                                                        .build()
                                        )
                                        .setStockInfo(DataCampOfferStockInfo.OfferStockInfo.newBuilder()
                                                .setPartnerStocks(DataCampOfferStockInfo.OfferStocks.newBuilder()
                                                        .setCount(s.getValue())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                )
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(
                                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                        .setOfferId(offerId)
                                        .build()
                        )
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                        .setActual(DataCampOfferContent.ProcessedSpecification.newBuilder()
                                                .setTitle(DataCampOfferMeta.StringValue.newBuilder()
                                                        .setValue(offerId)
                                                        .build())
                                                .build())
                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                        .setValue(offerId)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .putAllActual(actual)
                .build();
    }
}
