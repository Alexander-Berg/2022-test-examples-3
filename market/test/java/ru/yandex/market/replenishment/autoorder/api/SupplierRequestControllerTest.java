package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.exception.BadRequestException;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalesRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.client.FfwfApiClient;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.service.excel.SupplierRequestItemsWriter.getCatalogLink;

@WithMockLogin
public class SupplierRequestControllerTest extends ControllerTest {

    private static final String URL_PREFIX = "/api/v1/supplier-request";

    private static final String REQUEST_ACCEPT = "{ \"accept\": true }";
    private static final String REQUEST_DECLINE = "{ \"accept\": false }";

    private static final long SUPPLIER_ID = 1337;
    private static final int WAREHOUSE_ID = 145;
    private static final String DEADLINE_DATE = "2021-04-23";
    private static final String CREATED_AT = "2021-04-20T00:00:00";
    private static final String RESPONSE_DATE = "2021-05-22";

    private static final String[] headersExpected = new String[] {
        "Ваш SKU",
        "Название товара",
        "Штрихкоды",
        "Количество товаров в поставке",
        "Объявленная ценность одного товара, руб.",
        "Комментарий для склада",
        "Сейчас на складах, шт.",
        "Запланировано к поставке, шт.",
        "Заказано за 7 дней, шт.",
        "Не было на складах за 7 дней",
        "Заказано за 28 дней, шт.",
        "Не было на складах за 28 дней",
        "(Обновить данные о товаре) https://yandex.ru/support/marketplace/shipments/market-requests" +
            ".html#market-requests__recomendations",
        "Количество товаров в упаковке, шт.",
        "Минимальная партия поставки, шт.",
        "Добавочная партия, шт."
    };

    @Autowired
    FfwfApiClient ffwfApiClient;

    @Autowired
    SalesRepository salesRepository;

    @Before
    public void mockWorkbookConfig() {
        setTestTime(LocalDateTime.of(2021, 4, 28, 13, 34));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.get_by_supplier.before.csv")
    public void testGetBySupplierId() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(5)))

                .andExpect(jsonPath("$.requests[4].id").value("RPL-1"))
                .andExpect(jsonPath("$.requests[4].supplierId").value(SUPPLIER_ID))
                .andExpect(jsonPath("$.requests[4].warehouse.id").value(WAREHOUSE_ID))
                .andExpect(jsonPath("$.requests[4].deliveryDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[4].responseDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[4].created").value(CREATED_AT))
                .andExpect(jsonPath("$.requests[4].responseDate").value(RESPONSE_DATE))
                .andExpect(jsonPath("$.requests[4].status").value("SUPPLIER_ACCEPTED"))
                .andExpect(jsonPath("$.requests[4].requestSskus").value(11))
                .andExpect(jsonPath("$.requests[4].realSskus").value(8))

                .andExpect(jsonPath("$.requests[3].id").value("RPL-2"))
                .andExpect(jsonPath("$.requests[3].supplierId").value(SUPPLIER_ID))
                .andExpect(jsonPath("$.requests[3].warehouse.id").value(WAREHOUSE_ID))
                .andExpect(jsonPath("$.requests[3].deliveryDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[3].responseDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[3].created").value(CREATED_AT))

                .andExpect(jsonPath("$.requests[3].responseDate").isEmpty())
                .andExpect(jsonPath("$.requests[3].requestSskus").value(12))
                .andExpect(jsonPath("$.requests[3].realSskus").value(9))

                .andExpect(jsonPath("$.requests[2].id").value("RPL-3"))
                .andExpect(jsonPath("$.requests[2].supplierId").value(SUPPLIER_ID))
                .andExpect(jsonPath("$.requests[2].warehouse.id").value(WAREHOUSE_ID))
                .andExpect(jsonPath("$.requests[2].deliveryDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[2].responseDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[2].created").value(CREATED_AT))
                .andExpect(jsonPath("$.requests[2].requestSskus").value(13))
                .andExpect(jsonPath("$.requests[2].realSskus").value(10))

                .andExpect(jsonPath("$.requests[1].id").value("RPL-4"))
                .andExpect(jsonPath("$.requests[1].supplierId").value(SUPPLIER_ID))
                .andExpect(jsonPath("$.requests[1].warehouse.id").value(WAREHOUSE_ID))
                .andExpect(jsonPath("$.requests[1].deliveryDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[1].responseDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[1].created").value(CREATED_AT))
                .andExpect(jsonPath("$.requests[1].requestSskus").value(14))
                .andExpect(jsonPath("$.requests[1].realSskus").value(11))

                .andExpect(jsonPath("$.requests[0].id").value("RPL-5"))
                .andExpect(jsonPath("$.requests[0].supplierId").value(SUPPLIER_ID))
                .andExpect(jsonPath("$.requests[0].warehouse.id").value(WAREHOUSE_ID))
                .andExpect(jsonPath("$.requests[0].deliveryDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[0].responseDeadline").value(DEADLINE_DATE))
                .andExpect(jsonPath("$.requests[0].created").value(CREATED_AT))
                .andExpect(jsonPath("$.requests[0].requestSskus").value(15))
                .andExpect(jsonPath("$.requests[0].realSskus").value(12));

    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testGetBySupplierIdAndStatusFound() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337&status=SUPPLIER_ACCEPTED&status=NEW")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(1)));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testGetBySupplierIdAndStatusNotFound() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337&status=SUPPLIER_ACCEPTED")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.paging.before.csv")
    public void testGetBySupplierWithPaging() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337&status=NEW&count=2&page=2")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(2)))
                .andExpect(jsonPath("$.count").value(10))
                .andExpect(jsonPath("$.requests[0].id").value("RPL-8"))
                .andExpect(jsonPath("$.requests[1].id").value("RPL-7"))
        ;
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testGetBySupplierIdNonExistent() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=228"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Поставщик с указанным id не существует 228"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testGetBySubstringId() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337&idSubstring=1")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(1)))
                .andExpect(jsonPath("$.requests[0].id").value("RPL-1"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv",
            after = "SupplierRequestControllerTest.before.csv")
    public void testAcceptNonExistent() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-2/response")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_ACCEPT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Запроса #2 не существует"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv",
            after = "SupplierRequestControllerTest.accept.after.csv")
    public void testAccept() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_ACCEPT)).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.decline.after.csv",
            after = "SupplierRequestControllerTest.accept.after.csv")
    public void testAcceptDeclined() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_ACCEPT)).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv",
            after = "SupplierRequestControllerTest.decline.after.csv")
    public void testDecline() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_DECLINE)).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.accept.after.csv",
            after = "SupplierRequestControllerTest.decline.after.csv")
    public void testDeclineAccepted() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_DECLINE)).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.outdated.before.csv",
            after = "SupplierRequestControllerTest.outdated.before.csv")
    public void testOutdated() throws Exception {
        mockMvc.perform(post(URL_PREFIX + "/RPL-1/response")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST_ACCEPT)).andExpect(status().isOk());
    }

    @Test
    public void getExcelWithAbsentIdThrowError() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-request/RPL-1/excel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Не существует запрос поставщика с ид 1"));
    }

    @Test
    public void getByWrongId1() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-request/RPL-RPL/excel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("id have to be of RPL-123 format"));
    }

    @Test
    public void getByWrongId2() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-request/NE_RPL-RPL/excel"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("id have to be of RPL-123 format"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_excel.before.csv")
    @DbUnitDataSet(after = "SupplierRequestControllerTest_excel.after.csv")
    public void getExcelGetCorrectExcel() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/supplier-request/RPL-1/excel")
                        .header("x-supplier-id", 1))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        checkWorkbookStyle(excelData);

        List<List<Object>> lists = BaseExcelReader.extractFromExcel(
                new ByteArrayInputStream(excelData),
                headersExpected.length
        );

        assertEquals(4, lists.size());
        assertEquals(Arrays.asList(headersExpected), lists.get(0));
        lists.remove(0);

        List<Object> row = lists.get(0);

        int i = 0;
        assertEquals("000001.200\\0", row.get(i++));
        assertEquals("кепка", row.get(i++));
        assertEquals("bar2", row.get(i++));
        assertEquals(101.0, row.get(i++));
        assertEquals(12.34, row.get(i++));
        assertEquals("", row.get(i++));

        assertEquals(13.0, row.get(i++));
        assertEquals(10.0, row.get(i++));
        assertEquals(6.0, row.get(i++));
        assertEquals(3.0, row.get(i++));
        assertEquals(23.0, row.get(i++));
        assertEquals(7.0, row.get(i++));

        assertEquals("(Перейти к товару) " + getCatalogLink(1111L, "000001.200%5C0"), row.get(i++));
        assertEquals(2.0, row.get(i++));
        assertEquals(30.0, row.get(i++));
        assertEquals(3.0, row.get(i));

        i = 0;
        row = lists.get(1);
        assertEquals("000001.300", row.get(i++));
        assertEquals("мешок", row.get(i++));
        assertEquals("bar3", row.get(i++));
        assertEquals(102.0, row.get(i++));
        assertEquals(15.0, row.get(i++));
        assertEquals("", row.get(i++));

        assertEquals(5.0, row.get(i++));
        assertEquals(7.0, row.get(i++));
        assertEquals(3.0, row.get(i++));
        assertEquals(5.0, row.get(i++));
        assertEquals(5.0, row.get(i++));
        assertEquals(11.0, row.get(i++));

        assertEquals("(Перейти к товару) " + getCatalogLink(1111L, "000001.300"), row.get(i++));
        assertEquals(3.0, row.get(i++));
        assertEquals(100.0, row.get(i++));
        assertEquals(10.0, row.get(i));

        i = 0;
        row = lists.get(2);
        assertEquals("100", row.get(i++));
        assertEquals("шапка", row.get(i++));
        assertEquals("bar1", row.get(i++));
        assertEquals(0.0, row.get(i++));
        assertEquals(123.45, row.get(i++));
        assertEquals("", row.get(i++));

        assertEquals(7.0, row.get(i++));
        assertEquals(2.0, row.get(i++));
        assertEquals(1.0, row.get(i++));
        assertEquals(2.0, row.get(i++));
        assertEquals(4.0, row.get(i++));
        assertEquals(5.0, row.get(i++));

        assertEquals("(Перейти к товару) " + getCatalogLink(1111L, "100"), row.get(i++));
        assertEquals(2.0, row.get(i++));
        assertEquals(50.0, row.get(i++));
        assertEquals(5.0, row.get(i));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_excel.before.csv")
    @DbUnitDataSet(after = "SupplierRequestControllerTest_excel_second.after.csv")
    public void getExcelGetCorrectExcel_secondDownload() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/supplier-request/RPL-2/excel")
                .header("x-supplier-id", 1))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_excel.before.csv")
    public void getExcelGetCorrectExcelWithWrongHeader() throws Exception {
        mockMvc.perform(get("/api/v1/supplier-request/RPL-1/excel")
                        .header("x-supplier-id", 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Supplier requests have supplier id not equals xSupplierId: 2"));
    }

    private void checkWorkbookStyle(byte[] excelData) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(excelData);
        Workbook workbook = WorkbookFactory.create(bis);

        Sheet shipment = workbook.getSheet("Поставка");
        Sheet instruction = workbook.getSheet("Инструкция");
        Sheet description = workbook.getSheet("Описание полей");

        assertNotNull(shipment);
        assertNotNull(instruction);
        assertNotNull(description);

        checkRowStyle(
            shipment.getRow(0), 665,
            IndexedColors.GREY_25_PERCENT,
            BorderStyle.MEDIUM,
            IndexedColors.BLACK,
            IndexedColors.TAN);

        checkRowStyle(
            shipment.getRow(1), 300,
            IndexedColors.WHITE,
            BorderStyle.THIN,
            IndexedColors.GREY_40_PERCENT,
            IndexedColors.WHITE);

        checkRowStyle(
            shipment.getRow(2), 300,
            IndexedColors.LEMON_CHIFFON,
            BorderStyle.THIN,
            IndexedColors.GREY_40_PERCENT,
            IndexedColors.LEMON_CHIFFON);

        checkRowStyle(
            shipment.getRow(3), 300,
            IndexedColors.LEMON_CHIFFON,
            BorderStyle.THIN,
            IndexedColors.GREY_40_PERCENT,
            IndexedColors.LEMON_CHIFFON);
    }

    private void checkRowStyle(Row row,
                               long height,
                               IndexedColors firstPartForegroundColor,
                               BorderStyle borderBottom,
                               IndexedColors bottomBorderColor,
                               IndexedColors lastPartForegroundColor) {
        final CellStyle firstDataStyle = row.getCell(0).getCellStyle();
        final CellStyle lastDataStyle = row.getCell(row.getLastCellNum() - 1).getCellStyle();

        assertEquals(height, row.getHeight());
        assertEquals(firstPartForegroundColor.getIndex(), firstDataStyle.getFillForegroundColor());
        assertEquals(lastPartForegroundColor.getIndex(), lastDataStyle.getFillForegroundColor());
        assertEquals(FillPatternType.SOLID_FOREGROUND, firstDataStyle.getFillPattern());
        assertEquals(BorderStyle.THIN, firstDataStyle.getBorderRight());
        assertEquals(IndexedColors.BLACK.getIndex(), firstDataStyle.getRightBorderColor());
        assertEquals(borderBottom, firstDataStyle.getBorderBottom());
        assertEquals(bottomBorderColor.getIndex(), firstDataStyle.getBottomBorderColor());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testResponseDateReturns() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests", hasSize(1)))
                .andExpect(jsonPath("$.requests[0].responseDate").value("2021-05-24"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_testGetFfShopRequestsIds.before.csv")
    public void testGetFfShopRequestsIds() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "/ff-shop-requests?supplierRequestIds=RPL-1&supplierRequestIds=RPL-2")
                        .header("x-supplier-id", 1337))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].supplierRequestId").value("RPL-1"))
                .andExpect(jsonPath("$[0].ffShopRequestsIds", hasSize(2)))
                .andExpect(jsonPath("$[1].supplierRequestId").value("RPL-2"))
                .andExpect(jsonPath("$[1].ffShopRequestsIds", hasSize(3)));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.before.csv")
    public void testGetFfShopRequestsIdsNonExistent() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "/ff-shop-requests?supplierRequestIds=RPL-28&supplierRequestIds=RPL-29"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Не существует запрос поставщика с ID 28, 29"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest_coverage.before.csv")
    public void testGetSupplierRequestsCoverage() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "/coverage/123/?supplierRequestIds=RPL-1&supplierRequestIds=RPL-2")
                        .header("x-supplier-id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].supplierRequestId").value("RPL-1"))
                .andExpect(jsonPath("$[0].supplierRequestSskus").value(3L))
                .andExpect(jsonPath("$[0].coverageSskus").value(2L))
                .andExpect(jsonPath("$[1].supplierRequestId").value("RPL-2"))
                .andExpect(jsonPath("$[1].supplierRequestSskus").value(2L))
                .andExpect(jsonPath("$[1].coverageSskus").value(1L));
    }

    @Test
    @DbUnitDataSet(before = "SupplierControllerTest_coverage.withoutSskus.before.csv")
    public void testGetSupplierRequestsCoverage_withoutSskus() throws Exception {
        when(ffwfApiClient.getShadowSupplySskus(123L))
                .thenThrow(new BadRequestException("Error getting items of shadow supply with ID 123"));
        mockMvc.perform(get(URL_PREFIX + "/coverage/123/?supplierRequestIds=RPL-1&supplierRequestIds=RPL-2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Error getting items of shadow supply with ID 123"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.get_by_supplier.before.csv")
    public void testGetBySupplierIdWithWrongHeader() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337").header("x-supplier-id", 777))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Header x-supplier-id: 777 not equals supplierId: 1337"));
    }



    @Test
    @DbUnitDataSet(before = "SupplierControllerTest_coverage.before.csv")
    public void testGetSupplierRequestsCoverageWithWrongSupplierIds() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "/coverage/123/?supplierRequestIds=RPL-1&supplierRequestIds=RPL-2")
                        .header("x-supplier-id", 2))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Supplier requests have supplier id not equals xSupplierId: 2"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest.get_by_supplier.before.csv")
    public void testGetBySupplierIdWithoutHeader() throws Exception {
        mockMvc.perform(get(URL_PREFIX + "?supplierId=1337"))
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_getSL.before.csv")
    public void testGetSupplier3PSL() throws Exception {
        mockMvc.perform(get("/api/v1/supplier/111/sl")
                        .header("x-supplier-id", 111))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sl").value(0.25));
    }

    @Test
    @DbUnitDataSet(before = "SupplierRequestControllerTest_getSL.before.csv")
    public void testGetSupplier3PSL_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/supplier/42/sl")
                .header("x-supplier-id", 42))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Not found SL for supplier 42"));
    }
}
