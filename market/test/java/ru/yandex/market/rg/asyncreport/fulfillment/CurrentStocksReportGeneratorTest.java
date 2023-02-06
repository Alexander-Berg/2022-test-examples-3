package ru.yandex.market.rg.asyncreport.fulfillment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import Market.DataCamp.SyncAPI.OffersBatch;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.fulfillment.CurrentStocksReportGenerator;
import ru.yandex.market.core.fulfillment.DeadStockStatus;
import ru.yandex.market.core.fulfillment.SkuStockInfo;
import ru.yandex.market.core.fulfillment.StockYtDao;
import ru.yandex.market.core.fulfillment.model.FulfillmentStockFilter;
import ru.yandex.market.core.fulfillment.model.ShopSkuInfo;
import ru.yandex.market.core.fulfillment.model.Stock;
import ru.yandex.market.core.order.SupplierWarehouseShopSkuKey;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link CurrentStocksReportGenerator}.
 */
@DbUnitDataSet(before = "CurrentStocksReportGeneratorTest.before.csv")
class CurrentStocksReportGeneratorTest extends FunctionalTest {

    /*
      Если нужно увидеть локально сгенерированные отчеты из тестов,
          нужно выставить true и дописать в путь PATH_TO_HOME_DIR
    */
    private static final boolean WRITE_TO_HOME_DIRECTORY_ENABLED = false;
    private static final String PATH_TO_HOME_DIR = "/Users/**/Desktop/dir";

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private CurrentStocksReportGenerator generator;

    @Autowired
    StockYtDao stockYtDao; // spy

    @Test
    @DisplayName("Проверка отчета с двумя офферами в одном/единственном ответе от датакэмпа.")
    void checkCurrentStocksReportGenerator_withTwoOffers_ok() {
        var filter = new FulfillmentStockFilter(null, 3L);
        fillStockData(filter);
        var mockedResponse = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "CurrentStocksReportGeneratorTest.business505.proto.json",
                getClass()
        );
        doReturn(mockedResponse)
                .when(dataCampShopClient)
                .getBusinessUnitedOffers(eq(505L), eq(Set.of("bMTRY1fM1", "sprayer")), eq(3L));

        var sheet = generateReport(filter);
        assertHeader(sheet, "все", "одуванчик");
        assertRow(sheet, 5, "bMTRY1fM1", "100256654403", "Универсальная коляска Anex Sport (3 в 1) Ab-06 волны",
                3.0, 1.0, 2.0, 2.0, 1.0, 1.0, 2.0, 11.0,
                12.0, 13.0, 14.0, "Marschroute"
        );
        assertRow(sheet, 6, "sprayer", "987612345", "sprayer", 3.0, 1.0, 2.0,
                2.0, 1.0, 1.0, 2.0, 11.0, 12.0, 13.0, 14.0, "Marschroute"
        );
    }

    @DisplayName("При включенном stream mode делается два запроса в датакэмп с одним оффером в каждом.")
    @Test
    @DbUnitDataSet(before = "CurrentStocksReportGeneratorTest.streamModeAndBatchSize.csv")
    void supplierUCat_streamMode_ok() {
        var filter = new FulfillmentStockFilter(null, 3L);
        fillStockData(filter);
        var firstOffer = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "CurrentStocksReportGeneratorTest.before.offer1.json",
                getClass()
        );
        var secondOffer = ProtoTestUtil.getProtoMessageByJson(
                OffersBatch.UnitedOffersBatchResponse.class,
                "CurrentStocksReportGeneratorTest.before.offer2.json",
                getClass()
        );
        when(dataCampShopClient.getBusinessUnitedOffers(eq(505L), eq(Set.of("bMTRY1fM1")), eq(3L))).
                thenReturn(firstOffer);
        when(dataCampShopClient.getBusinessUnitedOffers(eq(505L), eq(Set.of("sprayer")), eq(3L))).
                thenReturn(secondOffer);
        var sheet = generateReport(filter);
        verify(dataCampShopClient, times(WRITE_TO_HOME_DIRECTORY_ENABLED ? 2 : 1))
                .getBusinessUnitedOffers(eq(505L), eq(Set.of("bMTRY1fM1")), eq(3L));
        verify(dataCampShopClient, times(WRITE_TO_HOME_DIRECTORY_ENABLED ? 2 : 1))
                .getBusinessUnitedOffers(eq(505L), eq(Set.of("sprayer")), eq(3L));
        assertHeader(sheet, "все", "одуванчик");
        assertRow(sheet, 5, "bMTRY1fM1", "100256654403", "Универсальная коляска Anex Sport (3 в 1) Ab-06 волны", 3.0,
                1.0, 2.0, 2.0, 1.0, 1.0, 2.0, 11.0, 12.0, 13.0, 14.0, "Marschroute"
        );
        assertRow(sheet, 6, "sprayer", "987612345", "sprayer", 3.0, 1.0, 2.0,
                2.0, 1.0, 1.0, 2.0, 11.0, 12.0, 13.0, 14.0, "Marschroute"
        );
    }

    @Test
    @DisplayName("Пустой отчет, если по partnerId нет данных.")
    void checkCurrentStocksReportGenerator_empty_ok() {
        var filter = new FulfillmentStockFilter(null, 4L);
        doReturn(List.of())
                .when(stockYtDao)
                .getCurrentStocks(filter);
        assertThatThrownBy(() -> generateReport(filter))
                .hasCauseInstanceOf(EmptyReportException.class);
    }

    @Test
    @DisplayName("Проверка выставления флага на yt-генерацию")
    void ytFlagTest() {
        var filter = new FulfillmentStockFilter(List.of(777L), 2L);
        var ex = new IllegalStateException("No list return");
        doThrow(ex)
                .when(stockYtDao)
                .getCurrentStocks(filter);
        assertThatThrownBy(() -> generateReport(filter))
                .hasCause(ex);
    }

    @Test
    @DisplayName("Не забудь убрать локальную запись")
    void dontForgetRemoveLocalHardcode() {
        Assertions.assertFalse(WRITE_TO_HOME_DIRECTORY_ENABLED);
    }

    private void fillStockData(FulfillmentStockFilter filter) {
        doReturn(List.of(
                new SkuStockInfo<>(
                        Stock.Builder.<SupplierWarehouseShopSkuKey>newBuilder()
                                .setWarehouseId(1L)
                                .setKey(new SupplierWarehouseShopSkuKey(3L, "bMTRY1fM1", "Marschroute"))
                                .setDateTime(LocalDateTime.of(2017, 1, 1, 0, 0))
                                .setAvailable(3)
                                .setQuarantine(2)
                                .setDefect(1)
                                .setFreeze(1)
                                .setExpired(2)
                                .setUtilization(1)
                                .setDeadStockStatus(DeadStockStatus.ALMOST_DEAD)
                                .build(),
                        ShopSkuInfo.builder()
                                .setSupplierId(3L)
                                .setShopSku("bMTRY1fM1")
                                .setLength(11)
                                .setWidth(12)
                                .setHeight(13)
                                .setWeight(BigDecimal.valueOf(14))
                                .build()
                ),
                new SkuStockInfo<>(
                        Stock.Builder.<SupplierWarehouseShopSkuKey>newBuilder()
                                .setWarehouseId(1L)
                                .setKey(new SupplierWarehouseShopSkuKey(3L, "sprayer", "Marschroute"))
                                .setDateTime(LocalDateTime.of(2017, 1, 1, 0, 0))
                                .setAvailable(3)
                                .setQuarantine(2)
                                .setDefect(1)
                                .setFreeze(1)
                                .setExpired(2)
                                .setUtilization(1)
                                .setDeadStockStatus(DeadStockStatus.DEAD)
                                .build(),
                        ShopSkuInfo.builder()
                                .setSupplierId(3L)
                                .setShopSku("sprayer")
                                .setLength(11)
                                .setWidth(12)
                                .setHeight(13)
                                .setWeight(BigDecimal.valueOf(14))
                                .build()
                )
        ))
                .when(stockYtDao)
                .getCurrentStocks(filter);
    }

    private static void assertHeader(Sheet sheet, String marschroute, String partnerName) {
        assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo(partnerName);
        assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo(marschroute);
        assertThat(sheet.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Ваш SKU");
        assertThat(sheet.getRow(3).getCell(1).getStringCellValue()).isEqualTo("SKU на Яндексе");
        assertThat(sheet.getRow(3).getCell(2).getStringCellValue()).isEqualTo("Название товара");
        assertThat(sheet.getRow(3).getCell(3).getStringCellValue()).isEqualTo("Годный");
        assertThat(sheet.getRow(3).getCell(4).getStringCellValue()).isEqualTo("Резерв");
        assertThat(sheet.getRow(3).getCell(5).getStringCellValue()).isEqualTo("Доступно для заказа");
        assertThat(sheet.getRow(3).getCell(6).getStringCellValue()).isEqualTo("Карантин");
        assertThat(sheet.getRow(3).getCell(7).getStringCellValue()).isEqualTo("Передан на утилизацию");
        assertThat(sheet.getRow(3).getCell(8).getStringCellValue()).isEqualTo("Брак");
        assertThat(sheet.getRow(3).getCell(9).getStringCellValue()).isEqualTo("Просрочен");
        assertThat(sheet.getRow(3).getCell(10).getStringCellValue()).isEqualTo("Длина, см");
        assertThat(sheet.getRow(3).getCell(11).getStringCellValue()).isEqualTo("Ширина, см");
        assertThat(sheet.getRow(3).getCell(12).getStringCellValue()).isEqualTo("Высота, см");
        assertThat(sheet.getRow(3).getCell(13).getStringCellValue()).isEqualTo("Вес, кг");
        assertThat(sheet.getRow(3).getCell(14).getStringCellValue()).isEqualTo("Склад");
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertRow(
            Sheet sheet, int rowNum,
            String sku, String marketSku, String title,
            double available, double freeze, double availableForOrder,
            double quarantine, double utilization, double defect, double expired,
            double length, double width, double height, double weight,
            String warehouseName
    ) {
        var row = sheet.getRow(rowNum);
        assertThat(row.getCell(0).getStringCellValue()).isEqualTo(sku);
        assertThat(row.getCell(1).getStringCellValue()).isEqualTo(marketSku);
        assertThat(row.getCell(2).getStringCellValue()).isEqualTo(title);
        assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(available); //годный
        assertThat(row.getCell(4).getNumericCellValue()).isEqualTo(freeze); //резерв
        assertThat(row.getCell(5).getNumericCellValue()).isEqualTo(availableForOrder); //доступны к заказу
        assertThat(row.getCell(6).getNumericCellValue()).isEqualTo(quarantine); //карантин
        assertThat(row.getCell(7).getNumericCellValue()).isEqualTo(utilization); //под утилизацию
        assertThat(row.getCell(8).getNumericCellValue()).isEqualTo(defect); //брак
        assertThat(row.getCell(9).getNumericCellValue()).isEqualTo(expired); //просрочен
        assertThat(row.getCell(10).getNumericCellValue()).isEqualTo(length); //длина
        assertThat(row.getCell(11).getNumericCellValue()).isEqualTo(width); //ширина
        assertThat(row.getCell(12).getNumericCellValue()).isEqualTo(height); //высота
        assertThat(row.getCell(13).getNumericCellValue()).isEqualTo(weight); //вес
        assertThat(row.getCell(14).getStringCellValue()).isEqualTo(warehouseName); //склад
    }

    private Sheet generateReport(FulfillmentStockFilter filter) {
        try {
            var outputStream = new ByteArrayOutputStream();
            generator.generateReport(filter, false, outputStream);
            if (WRITE_TO_HOME_DIRECTORY_ENABLED) {
                generator.generateReport(filter, false, toHomeDirectory());
            }
            var inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            var workbook = WorkbookFactory.create(inputStream);
            return workbook.getSheetAt(0);
        } catch (Exception ex) {
            throw new RuntimeException("Could not generate report", ex);
        }
    }

    private OutputStream toHomeDirectory() {
        try {
            String fileName = "report" + System.currentTimeMillis() + ".xlsx";
            Path tempFilePath = Path.of(PATH_TO_HOME_DIR + "/" + fileName);
            File reportFile = new File(tempFilePath.toString());
            return new FileOutputStream(reportFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
