package ru.yandex.market.ff.controller.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты для {@link RequestExportController}.
 *
 * @author avetokhin 05/04/2018.
 */
class RequestExportControllerTest extends MvcIntegrationTest {

    private static final long SUPPLIER_ID = 1;
    private static final long CROSSDOCK_SUPPLIER_ID = 101;
    private static final long INVENTORYING_SUPPLY_SUPPLIER_ID = 3;
    private static final long OPER_LOST_WITHDRAW_SUPPLIER_ID = 4;
    private static final long MOVEMENT_SUPPLY_SUPPLIER_ID = 6;
    private static final long MOVEMENT_WITHDRAW_SUPPLIER_ID = 7;

    private static final String SUPPLY_SUPPLIERS_PATH = "/export/suppliers/" + SUPPLIER_ID + "/supply-requests";
    private static final String CROSSDOCK_SUPPLY_SUPPLIERS_PATH =
        "/export/suppliers/" + CROSSDOCK_SUPPLIER_ID + "/supply-requests";

    private static final String SUPPLY_VIRTUAL_PATH = "/export/supply-requests";

    private static final String WITHDRAW_SUPPLIERS_PATH = "/export/suppliers/" + SUPPLIER_ID + "/withdraw-requests";
    private static final String WITHDRAW_VIRTUAL_PATH = "/export/withdraw-requests";

    private static final String INVENTORYING_SUPPLY_SUPPLIERS_PATH = "/export/suppliers/" +
            INVENTORYING_SUPPLY_SUPPLIER_ID + "/supply-requests";

    private static final String OPER_LOST_WITHDRAW_PATH = "/export/suppliers/" +
            OPER_LOST_WITHDRAW_SUPPLIER_ID + "/withdraw-requests";

    private static final String MOVEMENT_WITHDRAW_PATH = "/export/suppliers/" +
            MOVEMENT_WITHDRAW_SUPPLIER_ID + "/withdraw-requests";

    private static final String MOVEMENT_SUPPLY_PATH = "/export/suppliers/" +
            MOVEMENT_SUPPLY_SUPPLIER_ID + "/supply-requests";

    private static final String UTILIZATION_TRANSFERS_PATH =
            "/export/suppliers/" + SUPPLIER_ID + "/utilization-transfers";
    private static final String UTILIZATION_TRANSFERS_VIRTUAL_PATH =
            "/export/utilization-transfers";

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyList() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(SUPPLY_SUPPLIERS_PATH)
                        .param("shopIds", String.valueOf(SUPPLIER_ID))
                        .param("requestIds", "200")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("supply-list.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        assertCorrectExcelSheet(sheet);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/request_without_date.xml")
    void supplyWithoutRequestedDate() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(SUPPLY_SUPPLIERS_PATH)
                        .param("requestIds", "1")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        final Row row = sheet.getRow(1);
        assertThat(row.getCell(3).getStringCellValue(), equalTo(""));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedTypes() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(SUPPLY_SUPPLIERS_PATH)
                .param("shopIds", String.valueOf(SUPPLIER_ID))
                .param("requestIds", "200")
                .param("type", "0, 2"))
            .andExpect(status().isOk())
            .andReturn();

        final Sheet sheet = getSheet(mvcResult);

        assertCorrectExcelSheet(sheet);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedIllegalTypes() throws Exception {
        mockMvc.perform(
            get(SUPPLY_SUPPLIERS_PATH)
                .param("shopIds", String.valueOf(SUPPLIER_ID))
                .param("requestIds", "200")
                .param("types", "3"))
            .andDo(print())
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedCrossdockType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(CROSSDOCK_SUPPLY_SUPPLIERS_PATH)
                .param("shopIds", String.valueOf(CROSSDOCK_SUPPLIER_ID))
                .param("types", "4"))
            .andExpect(status().isOk())
            .andReturn();

        assertCorrectCrossdockExcelSheet(getSheet(mvcResult));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedMovementSupplyType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(MOVEMENT_SUPPLY_PATH)
                        .param("shopIds", String.valueOf(MOVEMENT_SUPPLY_SUPPLIER_ID))
                        .param("types", "16"))
                .andExpect(status().isOk())
                .andReturn();

        final Sheet sheet = getSheet(mvcResult);
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(13.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("201"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(0.0));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedMovementWithdrawType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(MOVEMENT_WITHDRAW_PATH)
                        .param("shopIds", String.valueOf(MOVEMENT_WITHDRAW_SUPPLIER_ID)))
                .andExpect(status().isOk())
                .andReturn();

        final Sheet sheet = getSheet(mvcResult);
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(14.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("201"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("Годный"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithProvidedInventoryingSupplyType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(INVENTORYING_SUPPLY_SUPPLIERS_PATH)
                .param("shopIds", String.valueOf(INVENTORYING_SUPPLY_SUPPLIER_ID))
                .param("types", "13"))
            .andExpect(status().isOk())
            .andReturn();

        final Sheet sheet = getSheet(mvcResult);
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("201"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(0.0));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawListWithProvidedOperLostWithdrawType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(OPER_LOST_WITHDRAW_PATH)
                .param("shopIds", String.valueOf(OPER_LOST_WITHDRAW_SUPPLIER_ID)))
            .andExpect(status().isOk())
            .andReturn();

        final Sheet sheet = getSheet(mvcResult);
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(11.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("201"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(row.getCell(3).getStringCellValue(),
                equalTo("Изъятие на товары, не найденные после 2 инвентаризаций"));
        assertThat(toLocal(row.getCell(4).getDateCellValue()),
                equalTo(LocalDateTime.of(2016, 10, 10, 0, 0, 0)));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListWithSurplusAndDefect() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(SUPPLY_SUPPLIERS_PATH)
                .param("shopIds", String.valueOf(SUPPLIER_ID))
                .param("requestIds", "201")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        //new FileOutputStream("supply-list.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(9.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("201"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Брак, Излишек"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyItems() throws Exception {
        final MvcResult mvcResult = supplyItemsRequest(1, 2);
        final Sheet sheet = getSheet(mvcResult);

        //new FileOutputStream("supply-items.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("SHOPSKU2"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Магнитола"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Магнитола"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo(
                "Вы не поставляли такой товар на склад — проверьте SKU или обратитесь в службу поддержки"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("22,33"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("2"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Излишек"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(150.5));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("SHOPSKU3"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Кандибобер"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Кандибобер"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo(
                "На складе осталось меньше штук товара, чем вы хотите вывезти. Для вывоза доступно 5 штук;" +
                        "Вы не поставляли такой товар на склад — проверьте SKU или обратитесь в службу поддержки"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("44,55"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("1"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(4.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(200.4));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5), nullValue());
        assertThat(row.getCell(6), nullValue());
        assertThat(row.getCell(7), nullValue());
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(1754.5));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyRegistryItems() throws Exception {
        final MvcResult mvcResult = supplyItemsRequest(8, 200);

        Workbook workbook = getWorkbook(mvcResult);
        Sheet sheet = workbook.getSheetAt(0);
        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("00100.microwave3"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Микроволновая печь LG MB-3924W"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(6438580.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Микроволновая печь LG MB-3924W"));
        assertThat(row.getCell(4).getNumericCellValue(), equalTo(1300.0));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Брак, Недостача, Излишек"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(30.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(23.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(0.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("00100.microwave5"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Микроволновая печь Sencor SMW 6022"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(9336445.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Микроволновая печь Sencor SMW 6022"));
        assertThat(row.getCell(4).getNumericCellValue(), equalTo(1500.0));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Брак, Излишек"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(20.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(35.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(3.0));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4).getNumericCellValue(), equalTo(69000.0));
        assertThat(row.getCell(5), nullValue());
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(50.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(58.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(3.0));
        assertThat(row.getCell(17).getNumericCellValue(), equalTo(65.0));

        // Принятые КИЗы
        Sheet sheetWithCis = workbook.getSheetAt(1);
        row = sheetWithCis.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("00100.microwave3"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("microwave3CIS1"));

        row = sheetWithCis.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("00100.microwave5"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("microwave5CIS1"));

        row = sheetWithCis.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("00100.microwave5"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("microwave5CIS2"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyRegistryItemsVirtual() throws Exception {
        MvcResult mvcResult = supplyItemsVirtualRequest(202);

        Workbook workbook = getWorkbook(mvcResult);
        Sheet sheet = workbook.getSheetAt(0);
        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Тест 8"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("00100.microwave3.8"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Микроволновая печь LG MB-3924W"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(6438580.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Микроволновая печь LG MB-3924W"));
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(1300.0));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Брак, Недостача, Излишек"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(30.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(23.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(17).getNumericCellValue(), equalTo(0.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Тест 9"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("00100.microwave5.9"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Микроволновая печь Sencor SMW 6022"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(9336445.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Микроволновая печь Sencor SMW 6022"));
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(1500.0));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Брак, Излишек"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(20.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(35.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(3.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(17).getNumericCellValue(), equalTo(5.0));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(69000.0));
        assertThat(row.getCell(6), nullValue());
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(50.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(58.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(3.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(15).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(16).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(17).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(18).getNumericCellValue(), equalTo(32.0));

        // Принятые КИЗы
        Sheet sheetWithCis = workbook.getSheetAt(1);
        row = sheetWithCis.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Тест 8"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("00100.microwave3.8"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("microwave3CIS1"));

        row = sheetWithCis.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Тест 9"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("00100.microwave5.9"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("microwave5CIS1"));

        row = sheetWithCis.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Тест 9"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("00100.microwave5.9"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("microwave5CIS2"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyRegistryItemsNoCisSheet() throws Exception {
        final MvcResult mvcResult = supplyItemsRequest(8, 201);
        Workbook workbook = getWorkbook(mvcResult);
        // проверка удаления страницы с КИЗами
        assertThat(workbook.getNumberOfSheets(), equalTo(1));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyRegistryItemsVirtualNoCisSheet() throws Exception {
        final MvcResult mvcResult = supplyItemsVirtualRequest(201);
        Workbook workbook = getWorkbook(mvcResult);
        // проверка удаления страницы с КИЗами
        assertThat(workbook.getNumberOfSheets(), equalTo(1));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListVirtual() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(SUPPLY_VIRTUAL_PATH)
                        .param("requestIds", "500")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("supply-list-virtual.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        assertCorrectExcelSheetForVirtualEndpoint(sheet);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListVirtualWithProvidedTypes() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(SUPPLY_VIRTUAL_PATH)
                .param("requestIds", "500")
                .param("types", "0, 2")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("supply-list-virtual.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        assertCorrectExcelSheetForVirtualEndpoint(sheet);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListVirtualWithProvidedIllegalTypes() throws Exception {
        mockMvc.perform(
            get(SUPPLY_VIRTUAL_PATH)
                .param("requestIds", "500")
                .param("types", "3"))
            .andDo(print())
            .andExpect(status().isInternalServerError());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyListVirtualWithProvidedCrossdockType() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
            get(SUPPLY_VIRTUAL_PATH)
                .param("types", "4")
        ).andReturn();

        assertCorrectVirtualCrossdockExcelSheet(getSheet(mvcResult));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void supplyItemsVirtual() throws Exception {
        final MvcResult mvcResult = supplyItemsVirtualRequest(4);

        final Sheet sheet = getSheet(mvcResult);

//         new FileOutputStream("supply-items-virtual.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ООО Квазиморда"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("SHOPSKU5.1"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Книга"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Книга"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("11,22"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("1"));
        assertThat(row.getCell(8).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(100.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Затупко"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("SHOPSKU6.2"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Очки"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Очки"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("11,22"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("1"));
        assertThat(row.getCell(8).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(100.0));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5), nullValue());
        assertThat(row.getCell(6), nullValue());
        assertThat(row.getCell(7), nullValue());
        assertThat(row.getCell(8), nullValue());
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(13).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(14).getNumericCellValue(), equalTo(200.0));
    }


    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawList() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(WITHDRAW_SUPPLIERS_PATH)
                        .param("requestIds", "6")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("withdraw-list.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("300"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("sparta warehouse"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("Просроченный"));
        assertThat(toLocal(row.getCell(4).getDateCellValue()), equalTo(LocalDateTime.of(2016, 12, 6, 5, 5, 5)));
        assertThat(toLocal(row.getCell(5).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 12, 0)));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Товары переданы"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(16.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(1.0));
        assertThat(toLocal(row.getCell(11).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 1, 0, 0, 0)));
        assertThat(toLocal(row.getCell(12).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 0, 0, 0)));
        assertThat(toLocal(row.getCell(13).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 5, 0)));
        assertThat(toLocal(row.getCell(14).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 10, 0)));
        assertThat(toLocal(row.getCell(15).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 11, 0)));
        assertThat(toLocal(row.getCell(16).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 12, 0)));

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawItems() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(WITHDRAW_SUPPLIERS_PATH + "/6/items")
        ).andReturn();
        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("withdraw-items.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("SHOPSKU1"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Наушники"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Наушники"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(50.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("SHOPSKU2"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Магнитола"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("М Магнитола"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(150.5));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(11.0));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(1052.5));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawItemsNotPrepared() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(WITHDRAW_SUPPLIERS_PATH + "/8/items")
        ).andReturn();
        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("withdraw-items.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("SHOPSKU1"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Книга"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(3).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(6).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(7).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(8).getStringCellValue(), equalTo(""));

        // Итого
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(6).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(7).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(8).getStringCellValue(), equalTo(""));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawListVirtual() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(WITHDRAW_VIRTUAL_PATH)
                        .param("requestIds", "7")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("withdraw-list-virtual.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИП Затупко"));
        assertThat(row.getCell(1).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(2).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("2x2 warehouse"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Брак"));
        assertThat(toLocal(row.getCell(5).getDateCellValue()), equalTo(LocalDateTime.of(2017, 2, 1, 0, 0, 0)));
        assertThat(row.getCell(6).getDateCellValue(), nullValue());
        assertThat(row.getCell(7).getStringCellValue(), equalTo("В обработке"));
        assertThat(row.getCell(8).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(7.0));
        assertThat(row.getCell(10).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(11).getStringCellValue(), equalTo(""));
        assertThat(toLocal(row.getCell(12).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 1, 0, 0, 0)));
        assertThat(toLocal(row.getCell(13).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 0, 0, 0)));
        assertThat(toLocal(row.getCell(14).getDateCellValue()), nullValue());
        assertThat(toLocal(row.getCell(15).getDateCellValue()), nullValue());
        assertThat(toLocal(row.getCell(16).getDateCellValue()), nullValue());
        assertThat(toLocal(row.getCell(17).getDateCellValue()), nullValue());
        assertThat(toLocal(row.getCell(18).getDateCellValue()), nullValue());

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void withdrawItemsVirtual() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get(WITHDRAW_VIRTUAL_PATH + "/6/items")
        ).andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("withdraw-items-virtual.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        // Первая строка
        Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ООО Квазиморда"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("SHOPSKU1.1"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Наушники"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Наушники"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(50.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ООО Квазиморда"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("SHOPSKU2.1"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Магнитола"));
        assertThat(row.getCell(3).getNumericCellValue(), equalTo(123.0));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("М Магнитола"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(150.5));

        // Итого
        row = sheet.getRow(3);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ИТОГО:"));
        assertThat(row.getCell(1), nullValue());
        assertThat(row.getCell(2), nullValue());
        assertThat(row.getCell(3), nullValue());
        assertThat(row.getCell(4), nullValue());
        assertThat(row.getCell(5), nullValue());
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(11.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1052.5));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransfers() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_PATH)
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("utilization-transfers.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        assertCorrectRowForUtilizationTransferList(sheet.getRow(1), 103, "", "warehouse",
                "Просроченный", "Создана", 1, null);
        assertCorrectRowForUtilizationTransferList(sheet.getRow(2), 104, "SERV_104", "2x2 warehouse",
                "Брак", "Товары оприходованы", 1, 1);

        assertThat(sheet.getRow(3), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransfersWithFilter() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_PATH).param("serviceIds", "1")
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        // new FileOutputStream("utilization-transfers.xlsx").write(mvcResult.getResponse().getContentAsByteArray());

        assertCorrectRowForUtilizationTransferList(sheet.getRow(1), 103, "", "warehouse",
                "Просроченный", "Создана", 1, null);

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransfersVirtualWithFilter() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_VIRTUAL_PATH).param("serviceIds", "1")
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        Row row = sheet.getRow(1);
        assertCorrectRowForUtilizationTransferList(row, 103, "", "warehouse",
                "Просроченный", "Создана", 1, null);
        assertThat(row.getCell(7).getStringCellValue(), equalTo("ООО Квазиморда"));

        assertThat(sheet.getRow(2), nullValue());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertCorrectRowForUtilizationTransferList(Row row, long requestId, String serviceRequestId,
                                                            String serviceName, String stock, String status,
                                                            int planItemsCount, Integer factItemsCount) {
        assertThat((long) row.getCell(0).getNumericCellValue(), equalTo(requestId));
        assertThat(row.getCell(1).getStringCellValue(), equalTo(serviceRequestId));
        assertThat(row.getCell(2).getStringCellValue(), equalTo(serviceName));
        assertThat(row.getCell(3).getStringCellValue(), equalTo(stock));
        assertThat(row.getCell(4).getStringCellValue(), equalTo(status));
        assertThat((int) row.getCell(5).getNumericCellValue(), equalTo(planItemsCount));
        if (factItemsCount == null) {
            assertThat(row.getCell(6).getStringCellValue(), equalTo(""));
        } else {
            assertThat((int) row.getCell(6).getNumericCellValue(), equalTo(factItemsCount));
        }
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransferItems() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_PATH + "/104/items")
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        assertCorrectRowForUtilizationTransferItems(sheet.getRow(1), "SHOPSKU1", "М Наушники", "2x2 warehouse",
                "Брак", 1, 1, BigDecimal.valueOf(50.0));

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransferItemsVirtual() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_VIRTUAL_PATH + "/104/items")
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        Row row = sheet.getRow(1);
        assertCorrectRowForUtilizationTransferItems(row, "SHOPSKU1", "М Наушники", "2x2 warehouse",
                "Брак", 1, 1, BigDecimal.valueOf(50.0));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("ООО Квазиморда"));

        assertThat(sheet.getRow(2), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-export/requests.xml")
    void utilizationTransferItemsNotEnriched() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get(UTILIZATION_TRANSFERS_PATH + "/103/items")
        ).andReturn();

        Sheet sheet = getSheet(mvcResult);

        assertCorrectRowForUtilizationTransferItems(sheet.getRow(1), "SHOPSKU1", "М Наушники", "warehouse",
                "Просроченный", 1, null, null);

        assertThat(sheet.getRow(2), nullValue());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void assertCorrectRowForUtilizationTransferItems(Row row, String article, String name,
                                                             String serviceName, String stock, int planCount,
                                                             Integer factCount, BigDecimal price) {
        assertThat(row.getCell(0).getStringCellValue(), equalTo(article));
        assertThat(row.getCell(1).getStringCellValue(), equalTo(name));
        assertThat(row.getCell(2).getStringCellValue(), equalTo(serviceName));
        assertThat(row.getCell(3).getStringCellValue(), equalTo(stock));
        assertThat((int) row.getCell(4).getNumericCellValue(), equalTo(planCount));
        if (factCount == null) {
            assertThat(row.getCell(5).getStringCellValue(), equalTo(""));
        } else {
            assertThat((int) row.getCell(5).getNumericCellValue(), equalTo(factCount));
        }
        if (price == null) {
            assertThat(row.getCell(6).getStringCellValue(), equalTo(""));
        } else {
            assertThat(BigDecimal.valueOf(row.getCell(6).getNumericCellValue()), equalTo(price));
        }
    }

    private void assertCorrectExcelSheetForVirtualEndpoint(Sheet sheet) {
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("Перепоставка"));
        assertThat(row.getCell(1).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("500"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("warehouse"));
        assertThat(toLocal(row.getCell(4).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 1, 0, 0, 0)));
        assertThat(toLocal(row.getCell(5).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 1, 0)));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(1.0));
        assertThat(toLocal(row.getCell(13).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 1, 0, 0, 0)));
        assertThat(toLocal(row.getCell(14).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 0, 0, 0)));
        assertThat(toLocal(row.getCell(15).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 0, 0)));
        assertThat(toLocal(row.getCell(16).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 1, 0)));
        assertThat(toLocal(row.getCell(17).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 10, 0)));
        assertThat(toLocal(row.getCell(18).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 11, 0)));

        assertThat(sheet.getRow(2), nullValue());
    }

    private void assertCorrectVirtualCrossdockExcelSheet(Sheet sheet) {
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("ООО Кроссдочный Квазиморда"));
        assertThat(row.getCell(1).getNumericCellValue(), equalTo(101.0));
        assertThat(row.getCell(2).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("warehouse"));
        assertThat(toLocal(row.getCell(4).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 9, 9, 9)));
        assertThat(row.getCell(5).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(7).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(12).getNumericCellValue(), equalTo(0.0));

        assertThat(toLocal(row.getCell(13).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 9, 9, 9)));
        assertThat(row.getCell(14).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(15).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(16).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(17).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(18).getStringCellValue(), equalTo(""));

        assertThat(sheet.getRow(2), nullValue());
    }


    private void assertCorrectCrossdockExcelSheet(Sheet sheet) {
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(101.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(toLocal(row.getCell(3).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 9, 9, 9)));
        assertThat(row.getCell(4).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Создана"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Нет"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(0.0));

        assertThat(toLocal(row.getCell(12).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 9, 9, 9)));
        assertThat(row.getCell(13).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(14).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(15).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(16).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(17).getStringCellValue(), equalTo(""));

        assertThat(sheet.getRow(2), nullValue());
    }

    private void assertCorrectExcelSheet(Sheet sheet) {
        final Row row = sheet.getRow(1);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("200"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("warehouse"));
        assertThat(toLocal(row.getCell(3).getDateCellValue()), equalTo(LocalDateTime.of(2016, 10, 10, 0, 0, 0)));
        assertThat(toLocal(row.getCell(4).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 1, 0)));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(6).getStringCellValue(), equalTo("Недостача"));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(10.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(9.0));
        assertThat(row.getCell(9).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(10).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(11).getNumericCellValue(), equalTo(1.0));
        assertThat(toLocal(row.getCell(12).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 1, 0, 0, 0)));
        assertThat(toLocal(row.getCell(13).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 2, 0, 0, 0)));
        assertThat(toLocal(row.getCell(14).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 0, 0)));
        assertThat(toLocal(row.getCell(15).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 1, 0)));
        assertThat(toLocal(row.getCell(16).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 10, 0)));
        assertThat(toLocal(row.getCell(17).getDateCellValue()), equalTo(LocalDateTime.of(2016, 1, 4, 0, 11, 0)));

        assertThat(sheet.getRow(2), nullValue());
    }

    private LocalDateTime toLocal(final Date date) {
        if (date == null) {
            return null;
        }

        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private MvcResult supplyItemsRequest(long supplierId, long requestId) throws Exception {
        return mockMvc.perform(
                get("/export/suppliers/" + supplierId + "/supply-requests/" + requestId + "/items")
        ).andReturn();
    }

    private MvcResult supplyItemsVirtualRequest(long requestId) throws Exception {
        return mockMvc.perform(
                get(SUPPLY_VIRTUAL_PATH + "/" + requestId + "/items")
        ).andReturn();
    }
}
