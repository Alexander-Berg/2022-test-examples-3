package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
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
import ru.yandex.market.replenishment.autoorder.exception.AssortmentParametersErrorException;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.AssortmentParameters;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Category;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion;
import ru.yandex.market.replenishment.autoorder.repository.postgres.AssortmentParametersRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.AssortmentParametersService;
import ru.yandex.market.replenishment.autoorder.service.excel.AssortmentParametersExcelReader;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class AssortmentParametersFunctionalTest extends ControllerTest {

    @Autowired
    AssortmentParametersRepository assortmentParametersRepository;

    @Autowired
    AssortmentParametersService assortmentParametersService;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    /*------------------------------- BEGIN VALIDATIONS --------------------------------------------------*/
    @Test
    public void testEmptyMsku() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.empty_msku.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 4 не заполнена ячейка с MSKU и ID Категории\\n" +
                                "В строке 5 не заполнена ячейка с MSKU и ID Категории\\n" +
                                "В строке 3 не заполнена ячейка с MSKU и ID Категории"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv",
            after = "AssortmentParametersFunctionalTest.empty_delete.after.csv")
    public void testEmptyDelete() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.empty_delete.xlsx"
        )
                .andExpect(status().isOk());
    }

    @Test
    public void testInvalidRegion() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.invalid_region.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 4 некорректно заполнен регион\\n" +
                                "В строке 5 некорректно заполнен регион"));
    }

    @Test
    public void testEmptyRegion() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.empty_region.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 3 не указан регион"));
    }

    @Test
    public void testWrongColumns() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.wrong_columns.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В файле должны быть следующие заголовки: [MSKU, ID категории, " +
                                "Наименование категории (информационное поле), Регион, Начало периода, " +
                                "Конец периода, Safety stock level (дни), Минимальный неснижаемый остаток (шт), " +
                                "Максимальный запас (шт), Максимальный запас (дни)]"));
    }

    @Test
    public void testInvalidFormatEndPeriod() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.invalid_format_periods.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 2 некорректно заполнен конец периода\\n" +
                                "В строке 3 не указаны начало или конец периода\\n" +
                                "В строке 2 некорректно заполнено начало периода"));
    }

    @Test
    public void testCrossingPeriods() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.crossing_periods.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Пересекающиеся периоды для MSKU 123123 и региона ростов\\n" +
                                "Пересекающиеся периоды для MSKU 123123 и региона москва"));
    }

    @Test
    public void testMskuAndCategoryOneRow() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.msku_and_category_one_row.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Одновременно указаны MSKU 400 и категория 10\\n" +
                                "Одновременно указаны MSKU 600 и категория 41"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv",
            after = "AssortmentParametersFunctionalTest_MskuBelongToOtherUsers.after.csv")
    public void testMskuBelongToOtherUsers() {
        expectedEx.expect(AssortmentParametersErrorException.class);
        expectedEx.expectMessage("Найдено 2 msku которые принадлежат другим специалистам или не принадлежат никому, " +
                "воспрользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
        ArrayList<AssortmentParameters> list = new ArrayList<>();
        list.add(new AssortmentParameters(null, 100L,
                WarehouseRegion.Companion.getMoscowId(),
                LocalDate.of(2020, 10, 15),
                LocalDate.of(2020, 10, 25),
                null, null, null, null, null, null, null));
        list.add(new AssortmentParameters(null, 300L,
                WarehouseRegion.Companion.getMoscowId(),
                LocalDate.of(2020, 10, 15),
                LocalDate.of(2020, 10, 25),
                null, null, null, null, null, null, null));
        assortmentParametersService.updateAssortmentParameters(list, "alex");
    }
    /*------------------------------- END VALIDATIONS --------------------------------------------------*/

    /*------------------------------- BEGIN CURRENT-USER --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv",
            after = "AssortmentParametersFunctionalTest.positive_simple.after.csv")
    public void testUploadExcel() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.positive_simple.xlsx"
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv",
            after = "AssortmentParametersFunctionalTest.category.after.csv")
    public void testUploadExcelWithCategory() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.category.xlsx"
        )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testUploadExcelWithInvalidCategory() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.invalid_category.xlsx"
        )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Категории с ID не существуют или не являются листовыми: 101"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGet() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/parameters/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        var reader = new AssortmentParametersExcelReader();
        List<AssortmentParameters> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(3, list.size());

        list.sort(Comparator.comparing(AssortmentParameters::getMinSafetyStock));
        int i = 0;
        assertAssortmentParameters(list.get(i++), 400, WarehouseRegion.Companion.getMoscowId(), LocalDate.parse("2020-08-01"),
                LocalDate.parse("2020-08-15"), null, null, 10, 100L, 1000, 30);
        assertAssortmentParameters(list.get(i++), 500, WarehouseRegion.Companion.getRostovId(), LocalDate.parse("2020-08-10"),
                LocalDate.parse("2020-08-20"), null, null, 20, 101L, 1000, 30);
        assertAssortmentParameters(list.get(i), 500, WarehouseRegion.Companion.getRostovId(), LocalDate.parse("2020-08-25"),
                LocalDate.parse("2020-09-05"), null, null, 30, 102L, 1000, 30);
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGetCategory() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/parameters/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        InputStream stream = new ByteArrayInputStream(excelData);

        List<Category> categories = BaseExcelReader.extractFromExcel(stream, (index, row) -> {
                    Double categoryId = BaseExcelReader.extractNumeric(row, 1);
                    String categoryName = BaseExcelReader.extractString(row, 2);

                    Category category = new Category();

                    if (categoryId != null) {
                        category.setId(categoryId.longValue());
                        category.setName(categoryName);
                    }

                    return category;
                },
                "MSKU",
                "ID категории",
                "Наименование категории (информационное поле)",
                "Регион",
                "Начало периода",
                "Конец периода",
                "Safety stock level (дни)",
                "Минимальный неснижаемый остаток (шт)",
                "Максимальный запас (шт)",
                "Максимальный запас (дни)");

        assertEquals(10, categories.get(0).getId());
        assertEquals("footwear", categories.get(0).getName());
        assertEquals(9, categories.get(1).getId());
        assertEquals("something", categories.get(1).getName());
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGetErrors() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/current-user/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.wrong_mskus.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/parameters/excel/get-errors"))
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

        assertEquals(4, list.size());
        assertEquals("boris", list.get(0).first);
        assertEquals("MSKU 100 из категории 12 привязана к пользователю bob", list.get(0).second);
        assertEquals("boris", list.get(1).first);
        assertEquals("Категория 40 (food) привязана к пользователю cat", list.get(1).second);
        assertEquals("boris", list.get(2).first);
        assertEquals("MSKU 300 ни к кому не привязан", list.get(2).second);
        assertEquals("boris", list.get(3).first);
        assertEquals("Категория 13 ни к кому не привязана", list.get(3).second);

    }
    /*------------------------------- END CURRENT-USER --------------------------------------------------*/

    /*------------------------------- BEGIN ADMIN --------------------------------------------------*/
    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testUserDoesntExist() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/user/not-exists/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.positive_simple.xlsx"
        )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Пользователь 'not-exists' не существует"));
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv",
            after = "AssortmentParametersFunctionalTest.positive_simple.after.csv")
    public void testUploadExcelAdmin() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/user/boris/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.positive_simple.xlsx"
        )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGetAdmin() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/assortment/parameters/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        var reader = new AssortmentParametersExcelReader();
        List<AssortmentParameters> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(3, list.size());

        list.sort(Comparator.comparing(AssortmentParameters::getMinSafetyStock));
        int i = 0;
        assertAssortmentParameters(list.get(i++), 400, WarehouseRegion.Companion.getMoscowId(), LocalDate.parse("2020-08-01"),
                LocalDate.parse("2020-08-15"), null, null, 10, 100L, 1000, 30);
        assertAssortmentParameters(list.get(i++), 500, WarehouseRegion.Companion.getRostovId(), LocalDate.parse("2020-08-10"),
                LocalDate.parse("2020-08-20"), null, null, 20, 101L, 1000, 30);
        assertAssortmentParameters(list.get(i), 500, WarehouseRegion.Companion.getRostovId(), LocalDate.parse("2020-08-25"),
                LocalDate.parse("2020-09-05"), null, null, 30, 102L, 1000, 30);
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGetErrorsAdmin() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/user/boris/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.wrong_mskus.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/assortment/parameters/excel/get-errors"))
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

        assertEquals(4, list.size());
        assertEquals("boris", list.get(0).first);
        assertEquals("MSKU 100 из категории 12 привязана к пользователю bob", list.get(0).second);
        assertEquals("boris", list.get(1).first);
        assertEquals("Категория 40 (food) привязана к пользователю cat", list.get(1).second);
        assertEquals("boris", list.get(2).first);
        assertEquals("MSKU 300 ни к кому не привязан", list.get(2).second);
        assertEquals("boris", list.get(3).first);
        assertEquals("Категория 13 ни к кому не привязана", list.get(3).second);
    }
    /*------------------------------- END ADMIN --------------------------------------------------*/

    /*------------------------------- BEGIN 403 --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testUploadExcel403() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/user/fedor/assortment/parameters/excel",
                "AssortmentParametersFunctionalTest.positive_simple.xlsx"
        )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGet403() throws Exception {
        mockMvc.perform(get("/api/v1/user/fedor/assortment/parameters/excel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentParametersFunctionalTest.before.csv")
    public void testGetErrors403() throws Exception {
        mockMvc.perform(get("/api/v1/user/boris/assortment/parameters/excel/get-errors"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    private void assertAssortmentParameters(AssortmentParameters ap,
                                            long msku,
                                            Long regionId,
                                            LocalDate start,
                                            LocalDate end,
                                            Integer turnoverTargetLevel,
                                            Integer stockAvailability,
                                            Integer safetyStockLevel,
                                            Long minSafetyStock,
                                            Integer maxItems,
                                            Integer maxItemsByDays) {
        assertEquals(msku, ap.getMsku());
        assertEquals(regionId, ap.getRegionId());
        assertEquals(start, ap.getPeriodStart());
        assertEquals(end, ap.getPeriodEnd());
        assertEquals(turnoverTargetLevel, ap.getTurnoverTargetLevel());
        assertEquals(stockAvailability, ap.getStockAvailability());
        assertEquals(safetyStockLevel, ap.getSafetyStockLevel());
        assertEquals(minSafetyStock, ap.getMinSafetyStock());
        assertEquals(maxItems, ap.getMaxItems());
        assertEquals(maxItemsByDays, ap.getMaxItemsByDays());
    }
}
