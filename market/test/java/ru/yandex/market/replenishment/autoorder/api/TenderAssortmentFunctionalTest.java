package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.exception.TenderAssortmentErrorException;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.AssortmentDto;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.excel.TenderAssortmentExcelReader;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;
import ru.yandex.market.replenishment.autoorder.service.tender.TenderAssortmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class TenderAssortmentFunctionalTest extends ControllerTest {

    @Autowired
    TenderAssortmentService tenderAssortmentService;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    /*------------------------------- BEGIN VALIDATIONS --------------------------------------------------*/
    @Test
    public void testEmptyMsku() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.empty_msku.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 2 не заполнены ячейки с MSKU и ID категории\\n" +
                                "В строке 4 не заполнены ячейки с MSKU и ID категории"));
    }

    @Test
    public void testWrongColumns() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.wrong_columns.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Колонка №1 имеет заголовок \"MSKU_bad\", а ожидается \"MSKU\". "
                                + "В файле должны быть следующие заголовки: "
                                + "[MSKU, Наименование (информационное поле), "
                                + "ID Категории, Категория (информационное поле)]"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testDuplicateMskus() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.duplicate.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("MSKU представлены в файле несколько раз: 100"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testDuplicateCategories() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.duplicateCategories.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("ID категории представлены в файле несколько раз: 100"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testUploadCategoryWithMsku() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.categoryAndMsku.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 1 заполнена и MSKU и категория, выберите что-то одно"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testDuplicatedMskuInCategory() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.mskuInCategory.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Найдены MSKU из уже указанных категорий: 321 (кат 41)"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testMskuBelongToOtherUsers() {
        expectedEx.expect(TenderAssortmentErrorException.class);
        expectedEx.expectMessage("Найдены msku и категории, которые принадлежат другим специалистам или не " +
                "принадлежат никому, " +
                "воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
        ArrayList<AssortmentDto> list = new ArrayList<>();
        list.add(new AssortmentDto(100L, null, null, null));
        list.add(new AssortmentDto(300L, null, null, null));
        tenderAssortmentService.addTenderAssortment(list, "boris");
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testMskuBelongToOtherUsers1() {
        expectedEx.expect(TenderAssortmentErrorException.class);
        expectedEx.expectMessage("Найдены msku и категории, которые принадлежат другим специалистам или не " +
            "принадлежат никому, " +
            "воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
        ArrayList<AssortmentDto> list = new ArrayList<>();
        list.add(new AssortmentDto(101L, null, null, null));
        tenderAssortmentService.addTenderAssortment(list, "boris");
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testCategoriesBelongToOtherUsers() {
        expectedEx.expect(TenderAssortmentErrorException.class);
        expectedEx.expectMessage("Найдены msku и категории, которые принадлежат другим специалистам или не " +
                "принадлежат никому, " +
                "воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
        ArrayList<AssortmentDto> list = new ArrayList<>();
        list.add(new AssortmentDto(null, null, 40L, null));
        tenderAssortmentService.addTenderAssortment(list, "boris");
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testMskuExistence() {
        expectedEx.expect(UserWarningException.class);
        expectedEx.expectMessage("MSKU не существуют: 100500");
        ArrayList<AssortmentDto> list = new ArrayList<>();
        list.add(new AssortmentDto(100500L, null, null, null));
        tenderAssortmentService.addTenderAssortment(list, "boris");
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testCategoryExistence() {
        expectedEx.expect(UserWarningException.class);
        expectedEx.expectMessage("Категории с ID не существуют или не являются листовыми: 100500");
        ArrayList<AssortmentDto> list = new ArrayList<>();
        list.add(new AssortmentDto(null, null, 100500L, null));
        tenderAssortmentService.addTenderAssortment(list, "boris");
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testNullMskuInCategory() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/tender/assortment/excel",
                "TenderAssortmentFunctionalTest.nullMskuInCategory.xlsx"
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Найдены msku и категории, которые принадлежат другим специалистам или не принадлежат никому, " +
                    "воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\""));

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/tender/assortment/excel/get-errors"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        List<Pair<String, String>> list = BaseExcelReader.extractFromExcel(
            new ByteArrayInputStream(excelData),
            (index, row) -> {
                String userLogin = BaseExcelReader.extractString(row, 0);
                String error = BaseExcelReader.extractString(row, 1);
                return new Pair<>(userLogin, error);
            }, "Текущий ответственный", "Ошибка");

        assertEquals(1, list.size());
        assertEquals("Категория 48 или MSKU из категории привязана к пользователю bob", list.get(0).second);
    }

    /*------------------------------- END VALIDATIONS --------------------------------------------------*/

    /*------------------------------- BEGIN CURRENT-USER --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.positive_simple.after.csv")
    public void testUploadExcel() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTestGet.before.csv")
    public void testGet() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/tender/assortment/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        var reader = new TenderAssortmentExcelReader();
        List<AssortmentDto> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(4, list.size());

        list.sort(Comparator.comparing(ta -> ta.getMsku() == null ? ta.getCategoryId() : ta.getMsku()));
        assertEquals(42, list.get(0).getCategoryId());
        assertEquals(100, list.get(1).getMsku());
        assertEquals(200, list.get(2).getMsku());
        assertEquals(300, list.get(3).getMsku());
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.empty-add.after.csv")
    public void testEmptyAdd() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.empty.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.before.csv")
    public void testEmptyDelete() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/delete/excel",
                        "TenderAssortmentFunctionalTest.empty.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.delete.after.csv")
    public void testSimpleDelete() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/delete/excel",
                        "TenderAssortmentFunctionalTest.delete.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testGetErrors() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.wrong_mskus.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/tender/assortment/excel/get-errors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<Pair<String, String>> list = BaseExcelReader.extractFromExcel(
                new ByteArrayInputStream(excelData),
                (index, row) -> {
                    String userLogin = BaseExcelReader.extractString(row, 0);
                    String error = BaseExcelReader.extractString(row, 1);
                    return new Pair<>(userLogin, error);
                }, "Текущий ответственный", "Ошибка");

        assertEquals(2, list.size());
        assertEquals("boris", list.get(0).first);
        assertEquals("MSKU 100 из категории 12 привязана к пользователю bob", list.get(0).second);
        assertEquals("boris", list.get(1).first);
        assertEquals("MSKU 300 ни к кому не привязан", list.get(1).second);
    }
    /*------------------------------- END CURRENT-USER --------------------------------------------------*/

    /*------------------------------- BEGIN ADMIN --------------------------------------------------*/
    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.positive_simple.after.csv")
    public void testUploadExcel_Admin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTestGet.before.csv")
    public void testGet_Admin() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/tender/assortment/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        var reader = new TenderAssortmentExcelReader();
        List<AssortmentDto> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(4, list.size());

        list.sort(Comparator.comparing(ta -> ta.getMsku() == null ? ta.getCategoryId() : ta.getMsku()));
        assertEquals(42, list.get(0).getCategoryId());
        assertEquals(100, list.get(1).getMsku());
        assertEquals(200, list.get(2).getMsku());
        assertEquals(300, list.get(3).getMsku());
    }

    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.empty-add.after.csv")
    public void testEmptyAdd_Admin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.empty.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.before.csv")
    public void testEmptyDelete_Admin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/tender/assortment/delete/excel",
                        "TenderAssortmentFunctionalTest.empty.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv",
            after = "TenderAssortmentFunctionalTest.delete.after.csv")
    public void testSimpleDelete_Admin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/tender/assortment/delete/excel",
                        "TenderAssortmentFunctionalTest.delete.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("tender-admin")
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testGetErrors_Admin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.wrong_mskus.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/tender/assortment/excel/get-errors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<Pair<String, String>> list = BaseExcelReader.extractFromExcel(
                new ByteArrayInputStream(excelData),
                (index, row) -> {
                    String userLogin = BaseExcelReader.extractString(row, 0);
                    String error = BaseExcelReader.extractString(row, 1);
                    return new Pair<>(userLogin, error);
                }, "Текущий ответственный", "Ошибка");

        assertEquals(2, list.size());
        assertEquals("boris", list.get(0).first);
        assertEquals("MSKU 100 из категории 12 привязана к пользователю bob", list.get(0).second);
        assertEquals("boris", list.get(1).first);
        assertEquals("MSKU 300 ни к кому не привязан", list.get(1).second);
    }

    /*------------------------------- END ADMIN --------------------------------------------------*/

    /*------------------------------- BEGIN 403 --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testUploadExcel403() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/fedor/tender/assortment/excel",
                        "TenderAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testGet403() throws Exception {
        mockMvc.perform(get("/api/v1/user/fedor/tender/assortment/excel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "TenderAssortmentFunctionalTest.before.csv")
    public void testGetErrors403() throws Exception {
        mockMvc.perform(get("/api/v1/user/boris/tender/assortment/excel/get-errors"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }
}
