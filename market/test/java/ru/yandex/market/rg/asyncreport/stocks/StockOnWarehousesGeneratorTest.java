package ru.yandex.market.rg.asyncreport.stocks;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.fulfillment.StockYtDao;
import ru.yandex.market.core.fulfillment.model.FulfillmentStockFilter;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональные тесты для {@link StocksOnWarehousesGenerator}.
 *
 * @author don-dron Zvorygin Andrey don-dron@yandex-team.ru
 */
@DbUnitDataSet(before = "testStockOnWarehouse.csv")
@ParametersAreNonnullByDefault
@DisplayName("Проверка получения файла отчета остатков складов")
class StockOnWarehousesGeneratorTest extends FunctionalTest {

    private static final String URL_TO_DOWNLOAD = "http://path/to";
    private static final String REPORT_ID = "1";
    private static final long EMPTY_SUPPLIER_ID = 1L;
    private static final long SUPPLIER_ID = 774L;
    private static final long FAIL_SUPPLIER_ID = 4L;
    private static final long WAREHOUSE_ID = 1L;
    private static final long NEXT_WAREHOUSE_ID = 2L;

    @Autowired
    StockYtDao stockYtDao; // spy

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private StocksOnWarehousesGenerator stocksOnWarehousesGenerator;

    @BeforeEach
    void setUp() {
        doReturn(List.of())
                .when(stockYtDao)
                .getCurrentStocks(any());
    }

    static Stream<Arguments> jsons() {
        return Stream.of(
                Arguments.of("" +
                        "{\n" +
                        "        \"entityId\": \"774\"\n" +
                        "}", null, "" +
                        "{" +
                        "\"entityId\":774," +
                        "\"warehouseIds\":null," +
                        "\"ora2pg_experimental_yt_gen\":false" +
                        "}"),
                Arguments.of("" +
                        "{\n" +
                        "        \"entityId\": \"774\",\n" +
                        "        \"warehouseIds\": [1,2]\n" +
                        "}\n", List.of(1L, 2L), "" +
                        "{" +
                        "\"entityId\":774," +
                        "\"warehouseIds\":[1,2]," +
                        "\"ora2pg_experimental_yt_gen\":false" +
                        "}"),
                Arguments.of("" +
                        "{\n" +
                        "        \"entityId\": \"774\",\n" +
                        "        \"warehouseIds\": [1]\n" +
                        "}\n", List.of(1L), "" +
                        "{" +
                        "\"entityId\":774," +
                        "\"warehouseIds\":[1]," +
                        "\"ora2pg_experimental_yt_gen\":false" +
                        "}"),
                Arguments.of("" +
                        "{\n" +
                        "        \"entityId\": \"774\",\n" +
                        "        \"warehouseIds\": []\n" +
                        "}\n", Collections.emptyList(), "" +
                        "{" +
                        "\"entityId\":774," +
                        "\"warehouseIds\":[]," +
                        "\"ora2pg_experimental_yt_gen\":false" +
                        "}")
        );
    }

    static Stream<Arguments> fbsJsons() {
        return Stream.of(
                Arguments.of("" +
                        "{\n" +
                        "        \"entityId\": \"775\",\n" +
                        "        \"warehouseIds\": []\n" +
                        "}\n", Collections.emptyList(), "" +
                        "{" +
                        "\"entityId\":775," +
                        "\"warehouseIds\":[]," +
                        "\"ora2pg_experimental_yt_gen\":false" +
                        "}")
        );
    }

    @ParameterizedTest
    @MethodSource("jsons")
    @DisplayName("Чтение и запись в JSON")
    void jsonReadingTest(String jsonInString, List<Long> argument, String toJson) throws IOException {
        var mapper = new ObjectMapper();
        var params = mapper.readValue(jsonInString, StocksOnWarehousesParams.class);

        assertThat(params.getPartnerId()).isEqualTo(774);
        assertThat(params.getWarehouseIds()).isEqualTo(argument);
        assertThat(mapper.writeValueAsString(params)).isEqualTo(toJson);
    }

    @ParameterizedTest
    @MethodSource("fbsJsons")
    @DisplayName("Чтение и запись в JSON для FBS")
    void jsonFBSReadingTest(String jsonInString, List<Long> argument, String toJson) throws IOException {
        var mapper = new ObjectMapper();
        var params = mapper.readValue(jsonInString, StocksOnWarehousesParams.class);

        assertThat(params.getPartnerId()).isEqualTo(775);
        assertThat(params.getWarehouseIds()).isEqualTo(argument);
        assertThat(mapper.writeValueAsString(params)).isEqualTo(toJson);
    }

    @DisplayName("Отчет не подготовлен")
    @Test
    void testWithEmptyResult() throws IOException {
        var params = new StocksOnWarehousesParams(EMPTY_SUPPLIER_ID, List.of(WAREHOUSE_ID));
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate(REPORT_ID, params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.DONE);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }

    @DisplayName("Отчет не подготовлен, если у дропшип-партнера нет склада")
    @Test
    void testWithEmptyResultForFBSEmptyLinks() throws IOException {
        var params = new StocksOnWarehousesParams(776, Collections.emptyList());
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate("2", params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.DONE);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }

    @DisplayName("Задан некорректный supplier_id.")
    @Test
    void testFailSupplierId() throws IOException {
        var params = new StocksOnWarehousesParams(FAIL_SUPPLIER_ID, List.of(WAREHOUSE_ID));
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate(REPORT_ID, params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.FAILED);
        assertThat(reportResult.getReportGenerationInfo().getDescription()).contains("Failed to render report.");
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }

    @DisplayName("Проверка чтения данных.")
    @Test
    void testStockReading() throws IOException {
        var params = new StocksOnWarehousesParams(SUPPLIER_ID, List.of(WAREHOUSE_ID));
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate(REPORT_ID, params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.DONE);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }

    @DisplayName("Проверка чтения данных FBS.")
    @Test
    void testStockReadingFBS() {
        var params = new StocksOnWarehousesParams(775, Collections.emptyList());

        ArgumentCaptor<FulfillmentStockFilter> captorFilter = ArgumentCaptor.forClass(FulfillmentStockFilter.class);

        stocksOnWarehousesGenerator.generate(REPORT_ID, params);

        Mockito.verify(stockYtDao).getCurrentStocks(captorFilter.capture());
        FulfillmentStockFilter actualFilter = captorFilter.getValue();
        assertThat(actualFilter.getWarehouseIds().size()).isEqualTo(1);
        assertThat(actualFilter.getWarehouseIds()).contains(12345L);
        assertThat(actualFilter.getSupplierId()).isEqualTo(775);
    }

    @DisplayName("Проверка чтения данных для всех складов.")
    @Test
    void testStockAllReading() throws IOException {
        var params = new StocksOnWarehousesParams(SUPPLIER_ID, null);
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate(REPORT_ID, params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.DONE);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }

    @DisplayName("Проверка чтения данных для нескольких складов.")
    @Test
    void testStockMultiplyReading() throws IOException {
        var params = new StocksOnWarehousesParams(SUPPLIER_ID, List.of(WAREHOUSE_ID, NEXT_WAREHOUSE_ID));
        doReturn(new URL(URL_TO_DOWNLOAD)).
                when(mdsS3Client).getUrl(any());

        var reportResult = stocksOnWarehousesGenerator.generate(REPORT_ID, params);
        assertThat(reportResult.getNewState()).isEqualTo(ReportState.DONE);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isNull();
    }
}
