package ru.yandex.market.rg.asyncreport.sku.movements;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.common.YtException;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.exception.EmptyReportException;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.rg.asyncreport.sku.movements.generator.SkuMovementReportFactory;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link SkuMovementGenerator}
 */
@DbUnitDataSet(before = "SkuMovementGeneratorTest.before.csv")
public class SkuMovementGeneratorTest extends FunctionalTest {

    private static final String HAHN = "hahn.yt.yandex.net";
    private static final String ARNOLD = "arnold.yt.yandex.net";
    private static final String REPORT_DATA_PATH = "//home/market/testing/mbi/sku_warehouse_movements/1d/latest";
    private static final int FIRST_BY_TYPE_ROW_NUM = 11;
    private static final int FIRST_BY_SKU_ROW_NUM = 9;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    YtHttpFactory ytHttpFactory;

    @Autowired
    private MboMappingsService mboMappingsService;

    @Autowired
    private ReportsDao<ReportsType> reportsDao;

    @Autowired
    private SkuMovementReportFactory generatorFactory;

    private final List<YTreeMapNode> ytData = readYtData();

    @Test
    @DisplayName("Вытащить параметры из базы и убедиться, что json извлекли корректно")
    @DbUnitDataSet(before = "SkuMovementGeneratorTest.paramsTest.before.csv")
    public void paramsTest() {
        ReportInfo<ReportsType> reportInfo = reportsDao.getPendingReportWithLock(
                Collections.singleton(ReportsType.SKU_MOVEMENTS));
        assertThat(reportInfo).isNotNull();
        assertThat(reportInfo.getReportRequest().getReportType()).isEqualTo(ReportsType.SKU_MOVEMENTS);
        assertThat(reportInfo.getReportRequest().getParams())
                .contains(
                        entry("partnerId", 2),
                        entry("entityId", 2),
                        entry("startDate", "2020-01-01"),
                        entry("endDate", "2020-12-31"),
                        entry("warehouseId", 214),
                        entry("shopSku", "some-sku")
                );
    }

    private static Yt mockYt(final List<YTreeMapNode> tableData) {
        final Yt yt = mock(Yt.class);
        final YtTables tables = mock(YtTables.class);
        when(yt.tables()).thenReturn(tables);
        if (CollectionUtils.isNotEmpty(tableData)) {
            doAnswer(invocation -> {
                final Consumer<YTreeMapNode> consumer = invocation.getArgument(2);
                tableData.forEach(consumer);
                return null;
            }).when(tables).read(any(), any(), any(Consumer.class));
        }
        return yt;
    }

    private static Yt mockYtThrowErr(Class<? extends Throwable> throwableType) {
        final Yt yt = mock(Yt.class);
        when(yt.tables()).thenThrow(throwableType);
        return yt;
    }

    private YTreeMapNode treeMapNode(final Map<String, YTreeNode> attributes) {
        final YTreeMapNodeImpl entries = new YTreeMapNodeImpl(null);
        attributes.forEach(entries::put);
        return entries;
    }

    private YTreeMapNode buildTreeMapNode(long supplierId, long warehouseId, String shopSku, String skuName,
                                          String eventtime, long count, long movementNumber, String movementType,
                                          Long orderId, Long oppositeWarehouseId) {
        ImmutableMap.Builder<String, YTreeNode> builder = ImmutableMap.<String, YTreeNode>builder()
                .put("supplier_id", new YTreeIntegerNodeImpl(true, supplierId, null))
                .put("warehouse_id", new YTreeIntegerNodeImpl(true, warehouseId, null))
                .put("shop_sku", new YTreeStringNodeImpl(shopSku, null))
                .put("sku_name", new YTreeStringNodeImpl(skuName, null))
                .put("eventtime", new YTreeStringNodeImpl(eventtime, null))
                .put("count", new YTreeIntegerNodeImpl(true, count, null))
                .put("movement_number", new YTreeIntegerNodeImpl(true, movementNumber, null))
                .put("movement_type", new YTreeStringNodeImpl(movementType, null));

        Optional.ofNullable(orderId)
                .ifPresent(order ->
                        builder.put("order_id", new YTreeIntegerNodeImpl(true, order, null)));

        Optional.ofNullable(oppositeWarehouseId)
                .ifPresent(id ->
                        builder.put("opposite_warehouse_id", new YTreeIntegerNodeImpl(true, id, null)));
        return treeMapNode(builder.build());
    }

    @BeforeEach
    void setUp() {
        MockUtil.resetMock(ytHttpFactory);
        MboMappings.SearchApprovedMappingsResponse mappingResponse =
                MboMappings.SearchApprovedMappingsResponse.newBuilder()
                        .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                                .setShopSku("100338121267")
                                .setMarketSkuId(123)
                                .setShopTitle("айфон 11 желтый")
                                .build())
                        .addMapping(MboMappings.ApprovedMappingInfo.newBuilder()
                                .setShopSku("100338121270")
                                .setMarketSkuId(124)
                                .setShopTitle("айфон 11 зеленый")
                                .build())
                        .build();
        when(mboMappingsService.searchApprovedMappingsByKeys(ArgumentMatchers.any())).thenReturn(mappingResponse);
    }

    @DisplayName("Отчет по всем складам и всем shop sku")
    @Test
    void testReportBySupplierId() {
        final Yt hahn = mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2019-02-28"))
                .build());

        ArgumentCaptor<YPath> pathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(hahn.tables()).read(pathArgumentCaptor.capture(), any(YTableEntryType.class), any(Consumer.class));
        Assertions.assertEquals(REPORT_DATA_PATH, pathArgumentCaptor.getValue().toTree().stringValue());
        assertBySkuHeaders(sheet);
        int rn = FIRST_BY_SKU_ROW_NUM;
        assertWarehouseSkuRow(sheet, rn++, "superwarehouse", 34.0, 1.0, 7.0, 8.0, 12.0, 5.0, 3.0);
        assertSkuRow(sheet, rn++, "100338121267", "Айфон 11 желтый", 13.0, 1.0, 3.0, 4.0, 0.0, 0.0, 2.0);
        assertSkuRow(sheet, rn++, "100338121270", "Айфон 11 зеленый", 21.0, 0.0, 4.0, 4.0, 12.0, 5.0, 1.0);
        assertWarehouseSkuRow(sheet, rn++, "Склад Ростов", 29.0, 0.0, 3.0, 8.0, 0.0, 2.0, 7.0);
        assertSkuRow(sheet, rn++, "100338121267", "Айфон 11 желтый", 19.0, 0.0, 1.0, 3.0, 0.0, 2.0, 4.0);
        assertSkuRow(sheet, rn++, "100338121270", "Айфон 11 зеленый", 10.0, 0.0, 2.0, 5.0, 0.0, 0.0, 3.0);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Отчет с указанным shopSku по всем складам")
    @Test
    void testReportBySupplierIdShopsku() {
        final Yt hahn = mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2019-02-28"))
                .setShopSku("100338121267")
                .build());

        ArgumentCaptor<YPath> pathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(hahn.tables()).read(pathArgumentCaptor.capture(), any(YTableEntryType.class), any(Consumer.class));
        Assertions.assertEquals(REPORT_DATA_PATH, pathArgumentCaptor.getValue().toTree().stringValue());

        assertByTypeHeaders(sheet);
        assertCommonFields(sheet, "ООО Петрушка", "20.02.2018 - 28.02.2019", "все", "100338121267",
                123.0, "айфон 11 желтый");
        assertWarehouseRow(sheet, 10, "superwarehouse", 17.0, 6.0);
        int rn = FIRST_BY_TYPE_ROW_NUM;
        assertRow(sheet, rn++, "Поставка", 123.0, "25.02.2018", 10.0, null, null);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 180.0, "25.02.2018", 3.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 184.0, "25.02.2018", null, 2.0, null);
        assertRow(sheet, rn++, "Заказ", 124.0, "25.03.2018", null, 3.0, null);
        assertRow(sheet, rn++, "Заказ", 128.0, "27.03.2018", null, 1.0, null);
        assertRow(sheet, rn++, "Возврат или невыкуп", 125.0, "28.03.2018", 1.0, null, 13456798.0);
        assertRow(sheet, rn++, "Поставка под заказ", 188.0, "01.04.2018", 3.0, null, null);
        assertWarehouseRow(sheet, rn++, "Склад Ростов", 20.0, 9.0);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 182.0, "25.02.2018", 1.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 186.0, "25.02.2018", null, 4.0, null);
        assertRow(sheet, rn++, "Поставка под заказ", 187.0, "25.02.2018", 4.0, null, null);
        assertRow(sheet, rn++, "Утилизация", 191.0, "22.03.2018", 0.0, 2.0, null);
        assertRow(sheet, rn++, "Поставка", 135.0, "29.03.2018", 15.0, null, null);
        assertRow(sheet, rn++, "Заказ", 137.0, "01.04.2018", null, 3.0, null);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Отчет с указанным warehouseId по всем shop sku")
    @Test
    void testReportBySupplierIdWarehouseId() {
        final Yt hahn = mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2019-02-28"))
                .setWarehouseId(145L)
                .build());

        ArgumentCaptor<YPath> pathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(hahn.tables()).read(pathArgumentCaptor.capture(), any(YTableEntryType.class), any(Consumer.class));
        Assertions.assertEquals(REPORT_DATA_PATH, pathArgumentCaptor.getValue().toTree().stringValue());

        assertBySkuHeaders(sheet);
        int rn = FIRST_BY_SKU_ROW_NUM;
        assertWarehouseSkuRow(sheet, rn++, "superwarehouse", 34.0, 1.0, 7.0, 8.0, 12.0, 5.0, 3.0);
        assertSkuRow(sheet, rn++, "100338121267", "Айфон 11 желтый", 13.0, 1.0, 3.0, 4.0, 0.0, 0.0, 2.0);
        assertSkuRow(sheet, rn++, "100338121270", "Айфон 11 зеленый", 21.0, 0.0, 4.0, 4.0, 12.0, 5.0, 1.0);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Отчет с указанным warehouseId и shopSku")
    @Test
    void testReportBySupplierIdWarehouseIdShopsku() {
        final Yt hahn = mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2019-02-28"))
                .setShopSku("100338121267")
                .setWarehouseId(145L)
                .build());

        ArgumentCaptor<YPath> pathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(hahn.tables()).read(pathArgumentCaptor.capture(), any(YTableEntryType.class), any(Consumer.class));
        Assertions.assertEquals(REPORT_DATA_PATH, pathArgumentCaptor.getValue().toTree().stringValue());

        assertByTypeHeaders(sheet);
        assertCommonFields(sheet, "ООО Петрушка", "20.02.2018 - 28.02.2019", "superwarehouse", "100338121267",
                123.0, "айфон 11 желтый");
        assertWarehouseRow(sheet, 10, "superwarehouse", 17.0, 6.0);
        int rn = FIRST_BY_TYPE_ROW_NUM;
        assertRow(sheet, rn++, "Поставка", 123.0, "25.02.2018", 10.0, null, null);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 180.0, "25.02.2018", 3.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 184.0, "25.02.2018", null, 2.0, null);
        assertRow(sheet, rn++, "Заказ", 124.0, "25.03.2018", null, 3.0, null);
        assertRow(sheet, rn++, "Заказ", 128.0, "27.03.2018", null, 1.0, null);
        assertRow(sheet, rn++, "Возврат или невыкуп", 125.0, "28.03.2018", 1.0, null, 13456798.0);
        assertRow(sheet, rn++, "Поставка под заказ", 188.0, "01.04.2018", 3.0, null, null);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Отчет с shopSku 100338121270 по всем складам")
    @Test
    void testReportBySupplierIdWarehouseIdShopsku2() {
        final Yt hahn = mockYt(ytData);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2019-02-28"))
                .setShopSku("100338121270")
                .build());

        ArgumentCaptor<YPath> pathArgumentCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(hahn.tables()).read(pathArgumentCaptor.capture(), any(YTableEntryType.class), any(Consumer.class));
        Assertions.assertEquals(REPORT_DATA_PATH, pathArgumentCaptor.getValue().toTree().stringValue());

        assertByTypeHeaders(sheet);
        assertCommonFields(sheet, "ООО Петрушка", "20.02.2018 - 28.02.2019", "все", "100338121270",
                124.0, "айфон 11 зеленый");
        assertWarehouseRow(sheet, 10, "superwarehouse", 25.0, 22.0);
        int rn = FIRST_BY_TYPE_ROW_NUM;
        assertRow(sheet, rn++, "Поставка", 101.0, "20.03.2018", 20.0, null, null);
        assertRow(sheet, rn++, "Заказ", 102.0, "22.03.2018", null, 4.0, null);
        assertRow(sheet, rn++, "Поставка под заказ", 190.0, "22.03.2018", 1.0, null, null);
        assertRow(sheet, rn++, "Утилизация", 192.0, "22.03.2018", null, 5.0, null);
        assertRow(sheet, rn++, "Вывоз со склада", 141.0, "04.04.2018", null, 2.0, null);
        assertRow(sheet, rn++, "Перемещение на склад Склад Ростов", 175.0, "15.05.2018", null, 8.0, null);
        assertRow(sheet, rn++, "Перемещение на другой склад", 176.0, "15.05.2018", null, 2.0, null);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 179.0, "21.05.2018", 4.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 183.0, "21.05.2018", null, 1.0, null);
        assertWarehouseRow(sheet, rn++, "Склад Ростов", 12.0, 8.0);
        assertRow(sheet, rn++, "Поставка под заказ", 189.0, "20.03.2018", 2.0, null, null);
        assertRow(sheet, rn++, "Заказ", 139.0, "30.03.2018", null, 5.0, null);
        assertRow(sheet, rn++, "Перемещение со склада superwarehouse", 177.0, "18.05.2018", 5.0, null, null);
        assertRow(sheet, rn++, "Перемещение со склада superwarehouse", 178.0, "21.05.2018", 3.0, null, null);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 181.0, "21.05.2018", 2.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 185.0, "21.05.2018", null, 3.0, null);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Тест запроса в реплику yt при ошибке от мастера yt")
    @Test
    void testReportBySupplierIdWarehouseIdShopskuReplication() {
        final Yt hahn = mockYtThrowErr(YtException.class);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);
        final Yt arnold = mockYt(ytData);
        when(ytHttpFactory.getYt(ARNOLD)).thenReturn(arnold);

        Sheet sheet = generateReport(SkuMovementParams.builder()
                .setEntityId(465221)
                .setStartDate(LocalDate.parse("2018-02-20"))
                .setEndDate(LocalDate.parse("2018-03-28"))
                .setWarehouseId(145L)
                .setShopSku("100338121267")
                .build());

        verify(arnold, times(1)).tables();
        verify(hahn, times(1)).tables();

        assertByTypeHeaders(sheet);
        assertCommonFields(sheet, "ООО Петрушка", "20.02.2018 - 28.03.2018", "superwarehouse", "100338121267",
                123.0, "айфон 11 желтый");
        assertWarehouseRow(sheet, 10, "superwarehouse", 17.0, 6.0);
        int rn = FIRST_BY_TYPE_ROW_NUM;
        assertRow(sheet, rn++, "Поставка", 123.0, "25.02.2018", 10.0, null, null);
        assertRow(sheet, rn++, "Излишки при инвентаризации", 180.0, "25.02.2018", 3.0, null, null);
        assertRow(sheet, rn++, "Недостача при инвентаризации", 184.0, "25.02.2018", null, 2.0, null);
        assertRow(sheet, rn++, "Заказ", 124.0, "25.03.2018", null, 3.0, null);
        assertRow(sheet, rn++, "Заказ", 128.0, "27.03.2018", null, 1.0, null);
        assertRow(sheet, rn++, "Возврат или невыкуп", 125.0, "28.03.2018", 1.0, null, 13456798.0);
        assertRow(sheet, rn++, "Поставка под заказ", 188.0, "01.04.2018", 3.0, null, null);
        Assertions.assertNull(sheet.getRow(rn));
    }

    @DisplayName("Тест пустого отчета")
    @Test
    void testEmptyReport() {
        final Yt hahn = mockYt(null);
        when(ytHttpFactory.getYt(HAHN)).thenReturn(hahn);

        assertThatExceptionOfType(EmptyReportException.class).isThrownBy(() -> {
            generateReport(SkuMovementParams.builder()
                    .setEntityId(465221)
                    .setStartDate(LocalDate.parse("2018-02-20"))
                    .setEndDate(LocalDate.parse("2018-02-28"))
                    .setWarehouseId(12L)
                    .setShopSku("123")
                    .build());
        }).withMessage("Report is empty");
    }

    private void assertWarehouseRow(
            Sheet sheet, int rowNum, String warehouseName, Double come, Double away) {
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(0).getStringCellValue(), is(warehouseName));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(4).getNumericCellValue(), is(come));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(5).getNumericCellValue(), is(away));
    }

    private void assertWarehouseSkuRow(
            Sheet sheet, int rowNum, String warehouseName, Double come,
            Double customerReturn, Double surplusInventory, Double away, Double withdraw,
            Double recycling, Double shortageInventory) {
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(0).getStringCellValue(), is(warehouseName));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(2).getNumericCellValue(), is(come));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(3).getNumericCellValue(), is(customerReturn));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(4).getNumericCellValue(), is(surplusInventory));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(5).getNumericCellValue(), is(away));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(6).getNumericCellValue(), is(withdraw));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(7).getNumericCellValue(), is(recycling));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(8).getNumericCellValue(), is(shortageInventory));
    }

    private void assertSkuRow(Sheet sheet, int rowNum,
                              String shopSku, String skuName,
                              Double come, Double customerReturn, Double surplusInventory,
                              Double away, Double withdraw, Double recycling, Double shortageInventory
    ) {
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(0).getStringCellValue(), is(shopSku));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(1).getStringCellValue(), is(skuName));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(2).getNumericCellValue(), is(come));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(3).getNumericCellValue(), is(customerReturn));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(4).getNumericCellValue(), is(surplusInventory));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(5).getNumericCellValue(), is(away));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(6).getNumericCellValue(), is(withdraw));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(7).getNumericCellValue(), is(recycling));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(8).getNumericCellValue(), is(shortageInventory));
    }

    private void assertRow(
            Sheet sheet, int rowNum, String movementType, double number, String date, Double come,
            Double away, Double orderId) {
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(0).getStringCellValue(), is(movementType));
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(1).getNumericCellValue(), is(number));

        if (orderId != null) {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(2).getNumericCellValue(), is(orderId));
        } else {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(2).getStringCellValue(), is(""));
        }
        MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(3).getStringCellValue(), is(date));

        if (come != null) {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(4).getNumericCellValue(), is(come));
        } else {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(4).getStringCellValue(), is(""));
        }

        if (away != null) {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(5).getNumericCellValue(), is(away));
        } else {
            MatcherAssert.assertThat(sheet.getRow(rowNum).getCell(5).getStringCellValue(), is(""));
        }
    }

    private void assertCommonFields(Sheet sheet, String supplierName, String dates, String warehouseName,
                                    String shopSku,
                                    Double marketSku, String shopSkuName) {
        MatcherAssert.assertThat(sheet.getRow(2).getCell(1).getStringCellValue(), is(supplierName));
        MatcherAssert.assertThat(sheet.getRow(3).getCell(1).getStringCellValue(), is(dates));
        MatcherAssert.assertThat(sheet.getRow(4).getCell(1).getStringCellValue(), is(warehouseName));
        MatcherAssert.assertThat(sheet.getRow(5).getCell(1).getStringCellValue(), is(shopSku));
        if (marketSku != null) {
            MatcherAssert.assertThat(sheet.getRow(6).getCell(1).getNumericCellValue(), is(marketSku));
        } else {
            MatcherAssert.assertThat(sheet.getRow(6).getCell(1).getStringCellValue(), is(""));
        }
        MatcherAssert.assertThat(sheet.getRow(7).getCell(1).getStringCellValue(), is(Objects.requireNonNullElse(shopSkuName, "")));
    }

    private void assertBySkuHeaders(Sheet sheet) {
        MatcherAssert.assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), is("Отчёт «Движение товаров»"));
        MatcherAssert.assertThat(sheet.getRow(2).getCell(0).getStringCellValue(), is("Магазин:"));
        MatcherAssert.assertThat(sheet.getRow(3).getCell(0).getStringCellValue(), is("Период:"));
        MatcherAssert.assertThat(sheet.getRow(4).getCell(0).getStringCellValue(), is("Склад:"));
        MatcherAssert.assertThat(sheet.getRow(5).getCell(0).getStringCellValue(), is("Товар:"));
        MatcherAssert.assertThat(sheet.getRow(7).getCell(0).getStringCellValue(), is("Ваш SKU"));
        MatcherAssert.assertThat(sheet.getRow(7).getCell(1).getStringCellValue(), is("Название товара"));
        MatcherAssert.assertThat(sheet.getRow(7).getCell(2).getStringCellValue(), is("Поступило за период"));
        MatcherAssert.assertThat(sheet.getRow(7).getCell(5).getStringCellValue(), is("Выбыло за период"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(0).getStringCellValue(), is(""));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(0).getStringCellValue(), is(""));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(1).getStringCellValue(), is(""));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(2).getStringCellValue(), is("Поставки"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(3).getStringCellValue(), is("Возвраты"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(4).getStringCellValue(), is("Излишки при инвентаризации"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(5).getStringCellValue(), is("Заказы"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(6).getStringCellValue(), is("Вывоз со склада"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(7).getStringCellValue(), is("Утилизация"));
        MatcherAssert.assertThat(sheet.getRow(8).getCell(8).getStringCellValue(), is("Недостача при инвентаризации"));
    }

    private void assertByTypeHeaders(Sheet sheet) {
        MatcherAssert.assertThat(sheet.getRow(0).getCell(0).getStringCellValue(), is("Отчёт «Движение товара»"));
        MatcherAssert.assertThat(sheet.getRow(2).getCell(0).getStringCellValue(), is("Магазин:"));
        MatcherAssert.assertThat(sheet.getRow(3).getCell(0).getStringCellValue(), is("Период:"));
        MatcherAssert.assertThat(sheet.getRow(4).getCell(0).getStringCellValue(), is("Склад:"));
        MatcherAssert.assertThat(sheet.getRow(5).getCell(0).getStringCellValue(), is("Ваш SKU:"));
        MatcherAssert.assertThat(sheet.getRow(6).getCell(0).getStringCellValue(), is("SKU на Яндексе:"));
        MatcherAssert.assertThat(sheet.getRow(7).getCell(0).getStringCellValue(), is("Товар:"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(0).getStringCellValue(), is("Склад и событие"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(1).getStringCellValue(), is("Номер документа"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(2).getStringCellValue(), is("Номер заказа (Невыкуп/Возврат)"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(3).getStringCellValue(), is("Дата"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(4).getStringCellValue(), is("Поступило"));
        MatcherAssert.assertThat(sheet.getRow(9).getCell(5).getStringCellValue(), is("Выбыло"));
    }

    private List<YTreeMapNode> readYtData() {
        JsonNode jsonRoot;
        try {
            jsonRoot = objectMapper.readTree(
                    getClass().getResourceAsStream("SkuMovementGeneratorTest.yt.data.json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<YTreeMapNode> dataset = new ArrayList<>();
        Iterator<JsonNode> elements = jsonRoot.elements();

        while (elements.hasNext()) {
            JsonNode jsonNode = elements.next();
            JsonNode oppositeWarehouseId = jsonNode.get("oppositeWarehouseId");
            JsonNode orderId = jsonNode.get("orderId");

            dataset.add(buildTreeMapNode(
                    jsonNode.get("supplierId").longValue(),
                    jsonNode.get("warehouseId").longValue(),
                    jsonNode.get("shopSku").asText(),
                    jsonNode.get("skuName").asText(),
                    jsonNode.get("eventtime").asText(),
                    jsonNode.get("count").longValue(),
                    jsonNode.get("movementNumber").longValue(),
                    jsonNode.get("movementType").asText(),
                    orderId != null ? orderId.longValue() : null,
                    oppositeWarehouseId != null ? oppositeWarehouseId.longValue() : null)
            );
        }
        return dataset;
    }

    private Sheet generateReport(SkuMovementParams params) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        generatorFactory.getReportGenerator(params).generateReport(params, outputStream);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            return workbook.getSheetAt(0);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
