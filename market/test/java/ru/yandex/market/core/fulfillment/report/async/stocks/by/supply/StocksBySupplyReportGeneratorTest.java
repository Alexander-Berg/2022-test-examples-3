package ru.yandex.market.core.fulfillment.report.async.stocks.by.supply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.fulfillment.FulfillmentSupplyType;
import ru.yandex.market.core.fulfillment.billing.storage.dao.StorageBillingBilledAmountDao;
import ru.yandex.market.core.fulfillment.billing.storage.dao.StorageBillingBilledAmountYtDao;
import ru.yandex.market.core.fulfillment.billing.storage.model.ReportStorageBillingBilledAmount;
import ru.yandex.market.core.fulfillment.billing.storage.model.StorageBillingBilledAmount;
import ru.yandex.market.core.fulfillment.model.FulfillmentOperationType;
import ru.yandex.market.core.fulfillment.report.async.stocks.by.supply.excel.JxlsStocksBySupply;
import ru.yandex.market.core.offer.mapping.MarketCategoryInfo;
import ru.yandex.market.core.offer.mapping.MarketSkuInfo;
import ru.yandex.market.core.offer.mapping.MarketSkuMappingInfo;
import ru.yandex.market.core.offer.mapping.MboMappingService;
import ru.yandex.market.core.offer.mapping.ShopOffer;
import ru.yandex.market.core.offer.mapping.SkuType;
import ru.yandex.market.core.supplier.SupplierInfo;
import ru.yandex.market.core.supplier.SupplierService;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertCellValues;

@DbUnitDataSet(before = "StocksBySupplyReportServiceTest.before.csv")
class StocksBySupplyReportGeneratorTest extends FunctionalTest {
    private static final LocalDate FIRST_JANUARY_2019 = LocalDate.of(2019, Month.JANUARY, 1);
    private static final LocalDate FIRST_NOVEMBER_2018 = LocalDate.of(2018, Month.NOVEMBER, 1);
    private static final long SUPPLIER_ID = 3L;

    @Autowired
    private StocksBySupplyReportGenerator stocksBySupplyReportGenerator;

    @Autowired
    private MboMappingService mboMappingService;

    @Autowired
    private StorageBillingBilledAmountYtDao ytDao;

    @Autowired
    private SupplierService supplierServiceMock;

    @Value("${mbi.report.fulfillment.stocks-by-supply.res-path:/reports/stocks-by-supply-template.xlsx}")
    private String stocksBySupplyTemplateResourcePath;

    private static Stream<Arguments> moveSupplyParam() {
        return Stream.of(
                Arguments.of(FulfillmentSupplyType.SUPPLY, FulfillmentOperationType.MOVE, "Перемещение"),
                Arguments.of(FulfillmentSupplyType.SUPPLY, FulfillmentOperationType.INBOUND, "Поставка"),
                Arguments.of(FulfillmentSupplyType.CROSSDOCK_SUPPLY, FulfillmentOperationType.INBOUND, "Поставка под " +
                        "заказ"),
                Arguments.of(FulfillmentSupplyType.CUSTOMER_RETURN_SUPPLY, FulfillmentOperationType.OUTBOUND,
                        "Возврат или невыкуп")
        );
    }

    @BeforeEach
    void beforeEach() {
        ytDao = Mockito.mock(StorageBillingBilledAmountYtDao.class);
        supplierServiceMock = Mockito.mock(SupplierService.class);
        mboMappingService = Mockito.mock(MboMappingService.class);
        mockSupplierService();
        mockMboMappingService();
        stocksBySupplyReportGenerator = new StocksBySupplyReportGenerator(
                null, "xslx", supplierServiceMock, stocksBySupplyTemplateResourcePath,
                ytDao, mboMappingService);
    }

    @Test
    void test_createAndStoreReport() throws IOException {
        mockDao();
        final File file = TempFileUtils.createTempFile();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            stocksBySupplyReportGenerator.generateReport(SUPPLIER_ID, FIRST_JANUARY_2019, fos);
            checkReport(WorkbookFactory.create(file));
        }
    }

    @Test
    @DbUnitDataSet(before = "StocksBySupplyReportGeneratorTest.generatorOnYtFlagOn.csv")
    void test_generatorOnYtFlagOn() {
        StorageBillingBilledAmountDao daoMock = Mockito.mock(StorageBillingBilledAmountDao.class);
        OutputStream osMock = Mockito.mock(OutputStream.class);

        Assertions.assertThatCode(() ->
                        stocksBySupplyReportGenerator.generateReport(SUPPLIER_ID, FIRST_JANUARY_2019, osMock))
                .isExactlyInstanceOf(EmptyReportException.class).hasMessage("Report is empty");

        Mockito.verifyNoMoreInteractions(daoMock);
        Mockito.verify(ytDao, times(1))
                .getReportWithAmount(
                        eq(SUPPLIER_ID),
                        eq(FIRST_JANUARY_2019)
                );
    }

    @ParameterizedTest()
    @DisplayName("если operationType == MOVE, то название типа должно быть перемещение. " +
            "В остальных случаях берётся на основе поля type.")
    @MethodSource("moveSupplyParam")
    void test_displayTypeInSupply(
            FulfillmentSupplyType type,
            FulfillmentOperationType operationType,
            String expectedTypeTitle
    ) {
        MatcherAssert.assertThat(
                getTestMoveSupply(type, operationType),
                MbiMatchers.<JxlsStocksBySupply>newAllOfBuilder()
                        .add(JxlsStocksBySupply::getType, expectedTypeTitle)
                        .build()
        );
    }

    private void checkReport(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        JxlsStocksBySupply data = getTestJxlsStocksBySupply();
        List<List<Object>> expectedValues = Collections.singletonList(
                Arrays.asList(data.getShopSku(), data.getMarketSku(), data.getTitle(), data.getSupplyId(),
                        data.getSupplyUpdateDate(), data.getType(), data.getItemsInSupply(), data.getItemsOnStock(),
                        data.getLength(), data.getWidth(), data.getHeight(), data.getWeight(),
                        data.getDaysOnStock(), data.getDaysOfPaidStorage(), data.getDaysToPay())
        );

        assertCellValues(expectedValues, sheet, 8, 0);
    }

    private JxlsStocksBySupply getTestJxlsStocksBySupply() {
        ReportStorageBillingBilledAmount reportBilledAmount = getReportStorageBillingBilledAmount();
        return JxlsStocksBySupply.from(reportBilledAmount, getMarketSkuMappingInfo());
    }

    private ReportStorageBillingBilledAmount getReportStorageBillingBilledAmount() {
        StorageBillingBilledAmount storageBillingBilledAmount = StorageBillingBilledAmount.builder()
                .setSupplyId(44L)
                .setSupplierId(3L)
                .setShopSku("sku_1")
                .setBillingDate(FIRST_JANUARY_2019)
                .setStartDate(FIRST_NOVEMBER_2018)
                .setItemsInSupply(12L)
                .setItemsOnStock(7L)
                .setLength(30)
                .setWidth(50)
                .setHeight(60)
                .setWeight(BigDecimal.valueOf(40))
                .setTariff(BigDecimal.valueOf(5L))
                .setTotalAmount(0)
                .setExportedToTlog(false)
                .build();

        return ReportStorageBillingBilledAmount.builder()
                .setBilledAmount(storageBillingBilledAmount)
                .setType(FulfillmentSupplyType.SUPPLY)
                .setDaysOnStock(61)
                .setDaysOfPaidStorage(0)
                .setDaysToPay(0)
                .build();
    }

    private MarketSkuMappingInfo getMarketSkuMappingInfo() {
        final MarketCategoryInfo category = MarketCategoryInfo.of(90403, "Category_90403");
        final MarketSkuInfo marketSkuInfo = MarketSkuInfo.of(100L, "товар", category, SkuType.MARKET);

        final ShopOffer shopOffer = new ShopOffer.Builder()
                .setShopSku("sku_1")
                .setSupplierId(SUPPLIER_ID)
                .setCategoryName("сад и огород")
                .setTitle("новая вещь")
                .addBarcode("barcode_513")
                .setVendor("super_vendor")
                .build();

        return MarketSkuMappingInfo.of(shopOffer, marketSkuInfo);
    }

    private JxlsStocksBySupply getTestMoveSupply(FulfillmentSupplyType type, FulfillmentOperationType operationType) {
        StorageBillingBilledAmount storageBillingBilledAmount = StorageBillingBilledAmount.builder()
                .setSupplyId(1L)
                .setSupplierId(2L)
                .setShopSku("SKU")
                .setBillingDate(FIRST_JANUARY_2019)
                .setStartDate(FIRST_NOVEMBER_2018)
                .setItemsInSupply(12L)
                .setItemsOnStock(7L)
                .setTariff(BigDecimal.valueOf(5L))
                .setTotalAmount(0)
                .setExportedToTlog(false)
                .build();

        ReportStorageBillingBilledAmount reportBilledAmount = ReportStorageBillingBilledAmount.builder()
                .setBilledAmount(storageBillingBilledAmount)
                .setType(type)
                .setOperationType(operationType)
                .setDaysOnStock(61)
                .setDaysOfPaidStorage(0)
                .setDaysToPay(0)
                .build();

        return JxlsStocksBySupply.from(reportBilledAmount, null);
    }

    private void mockMboMappingService() {
        MarketSkuMappingInfo mapping =
                MarketSkuMappingInfo.of(
                        ShopOffer.builder()
                                .setSupplierId(SUPPLIER_ID)
                                .setShopSku("sku_1")
                                .setTitle("новая вещь")
                                .build(),
                        MarketSkuInfo.of(100L, "товар",
                                MarketCategoryInfo.of(90403, "cat"), null)
                );
        Mockito.when(mboMappingService.createActiveMarketSkuMappingStream(ArgumentMatchers.anyLong()))
                .thenReturn(Stream.of(mapping));
    }

    private void mockDao() {
        var reportStorageBillingBilledAmounts =
                Collections.singletonList(getReportStorageBillingBilledAmount());
        when(ytDao.getReportWithAmount(eq(SUPPLIER_ID), eq(FIRST_JANUARY_2019)))
                .thenReturn(reportStorageBillingBilledAmounts);
    }

    private void mockSupplierService() {
        Mockito.when(supplierServiceMock.getSupplier(anyLong()))
                .thenReturn(new SupplierInfo(SUPPLIER_ID, "Supplier name"));
    }
}
