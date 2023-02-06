package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.config.security.SecurityUtils;
import ru.yandex.market.replenishment.autoorder.dto.OwnedAssortmentDto;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.AssortmentResponsibleError;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.UserJpa;
import ru.yandex.market.replenishment.autoorder.repository.postgres.UserRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.UserAssortmentService;
import ru.yandex.market.replenishment.autoorder.service.excel.AssortmentResponsibleExcelReader;
import ru.yandex.market.replenishment.autoorder.service.excel.core.reader.BaseExcelReader;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.INSERT;
import static ru.yandex.market.replenishment.autoorder.model.DemandType.TYPE_1P;
import static ru.yandex.market.replenishment.autoorder.model.DemandType.TYPE_3P;
@WithMockLogin
public class UserAssortmentFunctionalTest extends ControllerTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserAssortmentService userAssortmentService;

    @Autowired
    AuditTestingHelper auditTestingHelper;

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);
    private final String[] errorHeaders = {"Текущий ответственный", "Ошибка"};

    /*------------------------------- BEGIN VALIDATIONS --------------------------------------------------*/
    @Test
    public void testEmptyMskuAndCategory() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/excel",
                        "UserAssortmentFunctionalTest.empty_msku.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("В строке 1 не заполнены ячейки с MSKU и ID категории\\n" +
                                "В строке 2 не заполнены ячейки с MSKU и ID категории"));
    }

    @Test
    public void testWrongColumns() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/excel",
                        "UserAssortmentFunctionalTest.wrong_columns.xlsx"
                )
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Количество колонок не соответствует ожидаемому. "
                                + "В файле должны быть следующие заголовки: [MSKU, Наименование (информационное поле), "
                                + "ID Категории, Категория (информационное поле), Группа лог. параметров (опционально)"
                                + "]"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv", after = "UserAssortmentFunctionalTest" +
            ".no_exist_msku_1p.after.csv")
    public void testMskuNotExists_1p() {
        testMskuNotExists(TYPE_1P);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv", after = "UserAssortmentFunctionalTest" +
            ".no_exist_msku_3p.after.csv")
    public void testMskuNotExists_3p() {
        testMskuNotExists(TYPE_3P);
    }

    private void testMskuNotExists(DemandType demandType) {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(1000L, null, null, null, null));
        list.add(new OwnedAssortmentDto(7777L, null, null, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, demandType,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "MSKU не существуют: 7777, 1000");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testMskuDuplicates_1p() {
        testMskuDuplicates(TYPE_1P);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testMskuDuplicates_3p() {
        testMskuDuplicates(TYPE_3P);
    }

    private void testMskuDuplicates(DemandType demandType) {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(1000L, null, null, null, null));
        list.add(new OwnedAssortmentDto(1000L, null, null, null, null));
        list.add(new OwnedAssortmentDto(2000L, null, null, null, null));
        list.add(new OwnedAssortmentDto(2000L, null, null, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, demandType,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "MSKU представлены в файле несколько раз: 1000,2000");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testMskuDuplicates2() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        for (long i = 1000; i < 2000; i++) {
            list.add(new OwnedAssortmentDto(i, null, null, null, null));
            list.add(new OwnedAssortmentDto(i, null, null, null, null));
        }
        for (long i = 1000; i < 2000; i++) {
            list.add(new OwnedAssortmentDto(null, null, i, null, null));
            list.add(new OwnedAssortmentDto(null, null, i, null, null));
        }
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals("ID категории представлены в файле несколько раз: 1000,1001,1002,1003,1004,1005,1006,1007,1008," +
                        "1009...\\n" +
                        "MSKU представлены в файле несколько раз: 1000,1001,1002,1003,1004,1005,1006,1007,1008,1009...",
                exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGroupDoesntExist() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(100409204048L, null, null, null, "НетТакойГруппы"));
        list.add(new OwnedAssortmentDto(100409204049L, null, null, null, "ТакойТожеНет"));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals("Не найдены логистические параметры c группой 'НетТакойГруппы'\\n" +
                        "Не найдены логистические параметры c группой 'ТакойТожеНет'",
                exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testMskuBelongToAnotherUserCheck() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(127L, null, null, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "Найдено 1 msku и категорий которые принадлежат другим специалистам," +
                " воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testMskuBelongToALinkedCategoryCheck() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(229L, null, null, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "Найдено 2 msku и категорий которые принадлежат другим специалистам," +
                " воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testCategoryContainsLinkedMskuCheck() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(null, null, 15L, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "Найдено 1 msku и категорий которые принадлежат другим специалистам," +
                " воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testNotLeafCategory() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(null, null, 11L, null, null));
        list.add(new OwnedAssortmentDto(null, null, 122L, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals("Категории с ID не существуют или не являются листовыми: 122, 11", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testNotLeafCategoryAndMskuDoesNotExist() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(null, null, 11L, null, null));
        list.add(new OwnedAssortmentDto(1337L, null, null, null, null));

        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals("MSKU не существуют: 1337\\n" +
                "Категории с ID не существуют или не являются листовыми: 11", exception.getMessage());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.testConflictMskuAndCategoryByMsku.after.csv")
    public void testConflictMskuAndCategoryByMsku() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(229L, null, null, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "Найдено 2 msku и категорий которые принадлежат другим специалистам," +
                " воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.testConflictMskuAndCategoryByCategory.after.csv")
    public void testConflictMskuAndCategoryByCategory() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(null, null, 13L, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals(exception.getMessage(), "Найдено 2 msku и категорий которые принадлежат другим специалистам," +
                " воспользуйтесь кнопкой \"Выгрузить ошибки в Excel\"");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testConflictMskuAndCategoryFromSingleFile() {
        ArrayList<OwnedAssortmentDto> list = new ArrayList<>();
        list.add(new OwnedAssortmentDto(228L, null, null, null, null));
        list.add(new OwnedAssortmentDto(null, null, 14L, null, null));
        Exception exception = assertThrows(UserWarningException.class,
                () -> userAssortmentService.appendOrEditUserAssortment(list, TYPE_1P,
                        SecurityUtils.getCurrentUserLogin()));
        assertEquals("Найдены MSKU из уже указанных категорий: 228 (кат 14)", exception.getMessage());
    }
    /*------------------------------- END VALIDATIONS --------------------------------------------------*/

    /*------------------------------- BEGIN CURRENT-USER --------------------------------------------------*/
    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_simple_1p.after.csv")
    public void testUploadExcel_1p() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                        excelTestingHelper.upload(
                                        "POST",
                                        "/api/v1/current-user/assortment/excel",
                                        "UserAssortmentFunctionalTest.positive_simple.xlsx"
                                )
                                .andExpect(status().isOk()),
                3,
                r -> AuditTestingHelper.assertAuditRecord(r.get(0),
                        "assortment_responsible", INSERT, "category_id", 14)
        );
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_simple_3p.after.csv")
    public void testUploadExcel_3p() {
        auditTestingHelper.assertAuditRecordAdded(() ->
                        excelTestingHelper.upload(
                                        "POST",
                                        "/api/v1/current-user/assortment/excel?demandType=TYPE_3P",
                                        "UserAssortmentFunctionalTest.positive_simple.xlsx"
                                )
                                .andExpect(status().isOk()),
                3,
                r -> AuditTestingHelper.assertAuditRecord(r.get(0),
                        "assortment_responsible", INSERT, "category_id", 14)
        );
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_no_groups.after.csv")
    public void testUploadNoGroupsExcel() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/excel",
                        "UserAssortmentFunctionalTest.positive_no_groups.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_delete_1p.after.csv")
    public void testPostDeletePositive_1p() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/delete/excel?demandType=TYPE_1P",
                        "UserAssortmentFunctionalTest.positive_delete.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_delete_3p.after.csv")
    public void testPostDeletePositive_3p() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/delete/excel?demandType=TYPE_3P",
                        "UserAssortmentFunctionalTest.positive_delete.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGet_1p() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisBindings(excelData);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGet_3p() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/excel?demandType=TYPE_3P"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisBindings(excelData);
    }

    private void assertBorisBindings(byte[] excelData) {
        AssortmentResponsibleExcelReader reader = new AssortmentResponsibleExcelReader();
        List<OwnedAssortmentDto> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(4, list.size());
        assertBorisCommonBindings(list, 0);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before_categories.csv")
    public void testGetWithCategories_1p() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisCategoryBindings(excelData);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before_categories.csv")
    public void testGetWithCategories_3p() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/excel?demandType=TYPE_3P"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisCategoryBindings(excelData);
    }

    private void assertBorisCategoryBindings(byte[] excelData) {
        AssortmentResponsibleExcelReader reader = new AssortmentResponsibleExcelReader();
        List<OwnedAssortmentDto> list = reader.read(new ByteArrayInputStream(excelData));
        assertEquals(7, list.size());

        int i = 0;
        assertDto(list.get(i++), null, null, null, 12L, "Холодильники");
        assertDto(list.get(i++), null, null, null, 13L, "Телевизоры");
        assertDto(list.get(i++), null, null, null, 14L, "Микроволновки");
        assertBorisCommonBindings(list, i);
    }

    private void assertBorisCommonBindings(List<OwnedAssortmentDto> list, int i) {
        assertDto(list.get(i++), 123L, "колпак", null, null, null);
        assertDto(list.get(i++), 124L, "котелок", null, null, null);
        assertDto(list.get(i++), 125L, "бандана", null, null, null);
        assertDto(list.get(i), 100409204048L, "шапка", "Группа11", null, null);
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetErrorsWithoutUploadingExcel() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/excel",
                        "UserAssortmentFunctionalTest.negative_duplicate.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/current-user/assortment/excel/get-errors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<AssortmentResponsibleError> list = BaseExcelReader.extractFromExcel(
                new ByteArrayInputStream(excelData),
                readErrorsFromExcel,
                errorHeaders);

        assertEquals(1, list.size());
        assertEquals("boris", list.get(0).getUser().getLogin());
        assertEquals("MSKU 127 из категории 13 привязана к пользователю fedor", list.get(0).getMessage());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetErrorsMayRewriteLogParams() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/current-user/assortment/excel/check-log-params",
                        "UserAssortmentFunctionalTest.exist_log_params.xlsx")
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("У указанных MSKU уже существуют заполненные лог параметры"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetNoErrorsMayRewriteLogParams_1p() throws Exception {
        testGetNoErrorsMayRewriteLogParams("/api/v1/current-user/assortment/excel/check-log-params");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetNoErrorsMayRewriteLogParams_3p() throws Exception {
        testGetNoErrorsMayRewriteLogParams("/api/v1/current-user/assortment/excel/check-log-params?demandType=TYPE_3P");
    }

    public void testGetNoErrorsMayRewriteLogParams(String url) throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        url,
                        "UserAssortmentFunctionalTest.no_exist_log_params.xlsx")
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetResponsibleFromMsku_1p() throws Exception {
        testGetResponsibleFromMsku("/api/v1/assortment/125/responsible");
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetResponsibleFromMsku_3p() throws Exception {
        testGetResponsibleFromMsku("/api/v1/assortment/125/responsible?demandType=TYPE_3P");
    }

    public void testGetResponsibleFromMsku(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1000))
                .andExpect(jsonPath("$.login").value("boris"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetResponsibleFromMskuWrongMsku() throws Exception {
        mockMvc.perform(get("/api/v1/assortment/001/responsible"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("MSKU 1 не найден"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetResponsibleFromMskuNoResponsible() throws Exception {
        mockMvc.perform(get("/api/v1/assortment/228/responsible"))
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("У MSKU 228 не назначен ответственный"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetResponsibleFromMskuByCategory() throws Exception {
        mockMvc.perform(get("/api/v1/assortment/229/responsible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1002))
                .andExpect(jsonPath("$.login").value("assortment-admin"));
    }
    /*------------------------------- END CURRENT-USER --------------------------------------------------*/

    /*------------------------------- BEGIN ADMIN --------------------------------------------------*/

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testUserDoesntExist() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/not-exists/assortment/excel",
                        "UserAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Пользователь 'not-exists' не существует"));
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_simple_1p.after.csv")
    public void testUploadExcelAdmin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/assortment/excel",
                        "UserAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv",
            after = "UserAssortmentFunctionalTest.positive_delete_1p.after.csv")
    public void testPostDeletePositiveAdmin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/assortment/delete/excel",
                        "UserAssortmentFunctionalTest.positive_delete.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetAdmin() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/assortment/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisBindings(excelData);
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before_categories.csv")
    public void testGetAdminWithCategories() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/assortment/excel?demandType=TYPE_1P"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertBorisCategoryBindings(excelData);
    }

    @Test
    @WithMockLogin("assortment-admin")
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetErrorsWithoutUploadingExcelAdmin() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/boris/assortment/excel",
                        "UserAssortmentFunctionalTest.negative_duplicate.xlsx")
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        byte[] excelData = mockMvc.perform(get("/api/v1/user/boris/assortment/excel/get-errors"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        List<AssortmentResponsibleError> list = BaseExcelReader.extractFromExcel(
                new ByteArrayInputStream(excelData),
                readErrorsFromExcel,
                errorHeaders);

        assertEquals(1, list.size());
        assertEquals("boris", list.get(0).getUser().getLogin());
        assertEquals("MSKU 127 из категории 13 привязана к пользователю fedor", list.get(0).getMessage());
    }
    /*------------------------------- END ADMIN --------------------------------------------------*/

    /*------------------------------- BEGIN 403 --------------------------------------------------*/

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testUploadExcel403() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/fedor/assortment/excel",
                        "UserAssortmentFunctionalTest.positive_simple.xlsx"
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testPostDelete403() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/user/fedor/assortment/delete/excel",
                        "UserAssortmentFunctionalTest.positive_delete.xlsx"
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGet403() throws Exception {
        mockMvc.perform(get("/api/v1/user/fedor/assortment/excel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    @Test
    @DbUnitDataSet(before = "UserAssortmentFunctionalTest.before.csv")
    public void testGetErrors403() throws Exception {
        mockMvc.perform(get("/api/v1/user/boris/assortment/excel/get-errors"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("Access is denied"));
    }

    private void assertDto(OwnedAssortmentDto dto, Long msku, String title, String group, Long categoryId,
                           String categoryName) {
        assertEquals(msku, dto.getMsku());
        assertEquals(group, dto.getLogParamGroup());
        assertEquals(categoryId, dto.getCategoryId());
        assertEquals(categoryName, dto.getCategoryName());
        assertEquals(title, dto.getTitle());
    }

    private final BaseExcelReader.Mapper<AssortmentResponsibleError> readErrorsFromExcel = (index, row) -> {
        String responsible = BaseExcelReader.extractString(row, 0);
        String message = BaseExcelReader.extractString(row, 1);
        var jpaUser = userRepository.findByLogin(responsible).map(UserJpa::new);
        return new AssortmentResponsibleError(null, jpaUser.orElse(null), TYPE_1P, message, null);
    };
}
