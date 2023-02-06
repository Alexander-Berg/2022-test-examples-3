package ru.yandex.market.mboc.app.logisticsparams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.io.ByteStreams;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.FullExcelFile;
import ru.yandex.market.mboc.common.config.LogisticParamsConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SkuLogisticsParams;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.WhLogisticsParams;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.enrichment.LogisticParamsEnrichmentService;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.logisticsparams.repository.WhLogisticParamsRepository;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.excel.LogisticParamsHeaders;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Transactional
public class ImportLogisticParamsServiceTest extends BaseMbocAppTest {

    private static final Consumer<String> NULL_CONSUMER = s -> {
    };

    private static final String NEW_SHOP_SKU = "12221";
    private static final String SHOP_SKU_FOR_UPDATE = "12225";
    private static final String SHOP_SKU_FROM_DB = "91226";

    private static final int SUPPLIER_ID = OfferTestUtils.TEST_SUPPLIER_ID;
    private static final String NEW_AUTHOR = "test-user";
    private static final String OLD_AUTHOR = "test";
    private static final String DESTINATION = "ЛП Маршрут";

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private SkuLogisticParamsRepository skuLogisticParamsRepository;
    @Autowired
    private WhLogisticParamsRepository whLogisticParamsRepository;
    @Autowired
    private SupplierRepository supplierRepository;

    private ImportLogisticParamsService importExcelService;

    @Before
    public void setUp() {
        supplierRepository = new SupplierRepositoryMock();
        LogisticParamsConfig config = new LogisticParamsConfig();

        importExcelService = new ImportLogisticParamsService(
            config.skuParamsExcelFileConverter(),
            config.whParamsExcelFileConverter(),
            new ExcelS3ServiceMock(),
            supplierRepository,
            transactionHelper,
            new LogisticParamsEnrichmentService(),
            skuLogisticParamsRepository,
            whLogisticParamsRepository);

        supplierRepository.insert(OfferTestUtils.simpleSupplier());
    }

    @Test
    public void testParseCorrectExcelFile() {
        FullExcelFile.Builder excelFileBuilder = FullExcelFile.builder()
            .addSheet(LogisticParamsHeaders.SKU_PARAMS_SHEET_NAME,
                ExcelFile.Builder
                    .withHeaders(getSkuLPSheetHeaders())
                    .addLine(getSkuLPValuesRow())
                    .build());

        LogisticParamsHeaders.WH_PARAMS_SHEET_NAMES
            .forEach(sheetName -> excelFileBuilder.addSheet(sheetName,
                ExcelFile.Builder
                    .withHeaders(getWhLPSheetHeaders())
                    .build()));

        ImportLogisticParamsService.ImportResult result = importExcelService.parseExcel(
            excelFileBuilder.build(), SUPPLIER_ID, "test", NULL_CONSUMER);

        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    @Test
    public void testParseExcelFileWithoutAnyHeader() {
        List<String> allHeaders = getSkuLPSheetHeaders();
        List<String> headers = allHeaders.subList(0, allHeaders.size() - 1);
        FullExcelFile.Builder excelFileBuilder = FullExcelFile.builder()
            .addSheet(LogisticParamsHeaders.SKU_PARAMS_SHEET_NAME,
                ExcelFile.Builder
                    .withHeaders(headers)
                    .addLine(getSkuLPValuesRow())
                    .build());

        LogisticParamsHeaders.WH_PARAMS_SHEET_NAMES
            .forEach(sheetName -> excelFileBuilder.addSheet(sheetName,
                ExcelFile.Builder
                    .withHeaders(getWhLPSheetHeaders())
                    .build()));

        ImportLogisticParamsService.ImportResult result = importExcelService.parseExcel(
            excelFileBuilder.build(), SUPPLIER_ID, "test", NULL_CONSUMER);

        assertTrue(result.hasErrors());

        List<String> errors = result.getErrorsAsStrings(ErrorInfo.Level.ERROR);
        assertEquals(1, errors.size());
        assertEquals("Файл не содержит обязательного заголовка '" +
            allHeaders.get(allHeaders.size() - 1) + "' или его алиасов ", errors.get(0));
    }

    @Test
    public void testParseExcelFileWithoutAnyValue() {
        List<String> allHeaders = getSkuLPSheetHeaders();
        List<String> allValues = getSkuLPValuesRow();
        List<String> values = allValues.subList(1, allValues.size());
        values.add(0, null);
        FullExcelFile.Builder excelFileBuilder = FullExcelFile.builder()
            .addSheet(LogisticParamsHeaders.SKU_PARAMS_SHEET_NAME,
                ExcelFile.Builder
                    .withHeaders(allHeaders)
                    .addLine(values)
                    .build());

        LogisticParamsHeaders.WH_PARAMS_SHEET_NAMES
            .forEach(sheetName -> excelFileBuilder.addSheet(sheetName,
                ExcelFile.Builder
                    .withHeaders(getWhLPSheetHeaders())
                    .build()));

        ImportLogisticParamsService.ImportResult result = importExcelService.parseExcel(
            excelFileBuilder.build(), SUPPLIER_ID, "test", NULL_CONSUMER);

        assertTrue(result.hasErrors());

        List<String> errors = result.getErrorsAsStrings(ErrorInfo.Level.ERROR);
        assertEquals(1, errors.size());
        assertEquals("Ошибка на строке 2: Отсутствует значение для колонки '" +
            allHeaders.get(0) + "'", errors.get(0));
    }

    @Test
    public void testImportLogisticParamsFromExcelFile() throws IOException {
        skuLogisticParamsRepository.save(existingSkuLp().setShopSku(SHOP_SKU_FROM_DB));
        skuLogisticParamsRepository.save(existingSkuLp().setShopSku(SHOP_SKU_FOR_UPDATE));

        whLogisticParamsRepository.save(existingWhLp().setShopSku(SHOP_SKU_FROM_DB));
        whLogisticParamsRepository.save(existingWhLp().setShopSku(SHOP_SKU_FOR_UPDATE));

        Set<ShopSkuKey> keys = new HashSet<>();
        keys.add(new ShopSkuKey(SUPPLIER_ID, NEW_SHOP_SKU));
        keys.add(new ShopSkuKey(SUPPLIER_ID, SHOP_SKU_FOR_UPDATE));
        keys.add(new ShopSkuKey(SUPPLIER_ID, SHOP_SKU_FROM_DB));

        byte[] bytes = ByteStreams.toByteArray(
            getClass().getClassLoader().getResourceAsStream("excel/CorrectLPSample.xls"));

        ImportLogisticParamsService.ImportResult result = importExcelService.importExcel(SUPPLIER_ID,
            "someFile.xlsx", bytes, NULL_CONSUMER, NEW_AUTHOR);

        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());

        List<SkuLogisticsParams> skuLpsResult = skuLogisticParamsRepository
            .findByShopSkuKeys(SkuLogisticParamsRepository.Filter.all(), keys);
        assertEquals(keys.size(), skuLpsResult.size());
        checkSkuLps(skuLpsResult);

        List<WhLogisticsParams> whLpsResult = whLogisticParamsRepository
            .findByShopSkuKeys(WhLogisticParamsRepository.Filter.all(), keys);
        assertEquals(keys.size(), whLpsResult.size());
        checkWhLps(whLpsResult);
    }

    private void checkSkuLps(List<SkuLogisticsParams> skuLpsResult) {
        Map<String, SkuLogisticsParams> skuLspByShopSku = new HashMap<>();

        skuLpsResult.forEach(skuLp -> skuLspByShopSku.put(skuLp.getShopSku(), skuLp));

        SkuLogisticsParams newRecord = skuLspByShopSku.get(NEW_SHOP_SKU);
        assertNotNull(newRecord);
        assertEquals(SUPPLIER_ID, newRecord.getSupplierId().intValue());
        assertEquals(NEW_AUTHOR, newRecord.getAuthor());
        SkuLogisticsParams updatedRecord = skuLspByShopSku.get(SHOP_SKU_FOR_UPDATE);
        assertNotNull(updatedRecord);
        assertEquals(SUPPLIER_ID, updatedRecord.getSupplierId().intValue());
        assertEquals("Author must be changed.", NEW_AUTHOR, updatedRecord.getAuthor());
        SkuLogisticsParams recordFromBd = skuLspByShopSku.get(SHOP_SKU_FROM_DB);
        assertNotNull(recordFromBd);
        assertEquals("Author must not be changed.", OLD_AUTHOR, recordFromBd.getAuthor());
    }

    private void checkWhLps(List<WhLogisticsParams> whLpsResult) {
        Map<String, WhLogisticsParams> whLspByShopSku = new HashMap<>();

        whLpsResult.forEach(whLp -> whLspByShopSku.put(whLp.getShopSku(), whLp));

        WhLogisticsParams newRecord = whLspByShopSku.get(NEW_SHOP_SKU);
        assertNotNull(newRecord);
        assertEquals(SUPPLIER_ID, newRecord.getSupplierId().intValue());
        assertEquals(NEW_AUTHOR, newRecord.getAuthor());
        assertEquals(DESTINATION, newRecord.getDestination());
        WhLogisticsParams updatedRecord = whLspByShopSku.get(SHOP_SKU_FOR_UPDATE);
        assertNotNull(updatedRecord);
        assertEquals(SUPPLIER_ID, updatedRecord.getSupplierId().intValue());
        assertEquals("Author must be changed.", NEW_AUTHOR, updatedRecord.getAuthor());
        WhLogisticsParams recordFromBd = whLspByShopSku.get(SHOP_SKU_FROM_DB);
        assertNotNull(recordFromBd);
        assertEquals("Author must not be changed.", OLD_AUTHOR, recordFromBd.getAuthor());
    }

    private List<String> getSkuLPSheetHeaders() {
        List<String> result = new ArrayList<>();
        result.add("Ваш sku");

        result.add("Единица измерения основная");
        result.add("Единица измерения поставки (шт)");
        result.add("Единица измерения хранения(шт)");
        result.add("Единица измерения продажи (шт)");

        result.add("Количество в спайке");
        result.add("Количество в коробке");
        result.add("Количество в слое на паллете");
        result.add("Количество на паллете");

        result.add("ШК единицы измерения поставки");
        result.add("ШК спайки");
        result.add("ШК коробки");

        result.add("Вес нетто единицы");
        result.add("Вес брутто паллета");

        result.add("Наличие серийного номера (да/нет)");
        result.add("Наличие IMEI кода (да/нет)");
        result.add("Наличие срока годности");
        result.add("Сырье (да/нет)");

        result.add("Тип индивидуальной упаковки");

        result.add("Требуется заполнение документации при продаже (да/нет)");
        result.add("Требуется спецучет при продаже и/или перемещении (да/нет)");
        result.add("Возвратный товар (да/нет)");

        return result;
    }

    private List<String> getSkuLPValuesRow() {
        List<String> result = new ArrayList<>();
        result.add("shopSkuId");

        result.add("шт");
        result.add("1");
        result.add("2");
        result.add("3");

        result.add("4");
        result.add("5");
        result.add("6");
        result.add("7");

        result.add("12345671");
        result.add("12345672");
        result.add("12345673");

        result.add("1.9");
        result.add("1,8");

        result.add("да");
        result.add("нет");
        result.add("применим");
        result.add("нет");

        result.add("коробка");

        result.add("нет");
        result.add("нет");
        result.add("да");

        return result;
    }

    private List<String> getWhLPSheetHeaders() {
        List<String> result = new ArrayList<>();
        result.add("Ваш sku");

        result.add("Дни поставки");
        result.add("Минимальная поставка");
        result.add("Срок поставки, дней");

        return result;
    }

    private WhLogisticsParams existingWhLp() {
        WhLogisticsParams result = new WhLogisticsParams();
        result.setSupplierId(SUPPLIER_ID);
        result.setDestination(DESTINATION);
        result.setAuthor(OLD_AUTHOR);
        return result;
    }

    private SkuLogisticsParams existingSkuLp() {
        SkuLogisticsParams result = new SkuLogisticsParams();
        result.setSupplierId(SUPPLIER_ID);
        result.setAuthor(OLD_AUTHOR);
        return result;
    }
}

