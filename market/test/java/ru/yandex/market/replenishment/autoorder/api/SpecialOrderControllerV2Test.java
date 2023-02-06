package ru.yandex.market.replenishment.autoorder.api;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.config.TestExecutorConfig;
import ru.yandex.market.replenishment.autoorder.dto.GenerateDataStatusResponse;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.WarehouseAvailabilityValidationService;
import ru.yandex.market.replenishment.autoorder.utils.ExcelCellValidator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockLogin
@Import(TestExecutorConfig.class)
@MockBean(value = {WarehouseAvailabilityValidationService.class})
public class SpecialOrderControllerV2Test extends ControllerTest {

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Autowired
    @Qualifier("deepmindHttpClient")
    private CloseableHttpClient httpClient;
    @Autowired
    private WarehouseAvailabilityValidationService warehouseAvailabilityValidationService;

    @Before
    public void init() {
        super.setTestTime(LocalDateTime.of(2020, 9, 6, 0, 0));
        this.mockDeepmindClientForSpecialOrder("{\"ticketName\":\"name\"}");

        Mockito.doReturn(Collections.emptySet())
            .when(warehouseAvailabilityValidationService).validateWarehouseAvailability(anyList());
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongItemQuantityTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_item_quantity.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Для заявки на SSKU 000124.8714100866806 для недели №37 количество отгрузки (5001) не кратно кванту (100)" +
                ", номер строки: 2;Для заявки на SSKU 000124.8714100866806 для недели №38 количество отгрузки (111) " +
                "не кратно кванту (100), номер строки: 2");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongPriceTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_price.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Цена товара с SSKU 000124.8714100866806 не может быть отрицательной, номер строки: 2");
        validator.assertThatTextEquals(2, 19,
            "Цена товара с SSKU 000124.8714100866806 не может быть отрицательной, номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongQuantumTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_quantum.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Для заявки на SSKU 000124.8714100866806 неверно указан квант, номер строки: 2");
        validator.assertThatTextEquals(2, 19,
            "Для заявки на SSKU 000124.8714100866806 неверно указан квант, номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongSeveralWeeksTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_several_weeks.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(2, 19,
            "Заявке на SSKU 000124.8714100866806 со счетом account1 соответствует несколько недель отгрузки, номер " +
                "строки: 3");

    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongSSKUTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_ssku.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "SSKU 000124.8714100868086 не был найден в базе, номер строки: 2;SSKU 000124.8714100868086 не был найден " +
                "в базе, номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongTypeTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_type.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = this.getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Для заявки на SSKU 000124.8714100866806 неверно указан тип, валидные значения: [лот нов сез вал доп.об " +
                "промо перекуп], номер строки: 2");
        validator.assertThatTextEquals(2, 19,
            "Для заявки на SSKU 000124.8714100866806 неверно указан тип, валидные значения: [лот нов сез вал доп.об " +
                "промо перекуп], номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersNonEmptyIdTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.non_empty_id.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Не найдены спецзаказы с идентификаторами: 1, 2. Если вы хотите создать эти заказы, колонка " +
                "идентификатора должна быть пустой.");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersNotMondayTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.not_monday.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(0, 19,
            "дата 25/08/2020 не понедельник, номер колонки: 12, номер строки: 0");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void emptyWeekHeaderTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.empty_week_header.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 17,
            "Для заявки на SSKU 000124.8714100866806 не указано ни одной недели отгрузки, номер строки: 2");
        validator.assertThatTextEquals(2, 17,
            "Для заявки на SSKU 000124.8714100866806 не указано ни одной недели отгрузки, номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersEmptySSKUTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.empty_ssku.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "В файле должны быть указаны SSKU, номер строки: 2");

        validator.assertThatTextEquals(2, 19,
            "В файле должны быть указаны SSKU, номер строки: 3");
    }


    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersWrongWeekNumberTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.wrong_week_number.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(0, 19,
            "номер недели (69) не совпадает с реальным (35), номер колонки: 12, номер строки: 0");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv")
    public void addSpecialOrdersEmptyWarehouseTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.empty_warehouse.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);
        byte[] contentAsByteArray = getSavedData(response);
        ExcelCellValidator validator = new ExcelCellValidator(contentAsByteArray);

        validator.assertThatTextEquals(1, 19,
            "Для заявки на SSKU 000124.8714100866806 не указано название склада, номер строки: 2");
        validator.assertThatTextEquals(2, 19,
            "Для заявки на SSKU 000124.8714100866806 не указано название склада, номер строки: 3");
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "SpecialOrderControllerV2Test.simple.before.csv",
        after = "SpecialOrderControllerV2Test.simple.after_positive.csv")
    public void addSpecialOrdersOkTest() {
        MvcResult saveExcelMvcResult = excelTestingHelper.uploadWithParams(
                "POST",
                "/api/v2/current-user/special-order/excel",
                "SpecialOrderControllerV2Test.simple_positive.xlsx",
                Map.of("useFastTrack", "true"))
            .andExpect(status().isOk())
            .andReturn();

        GenerateDataStatusResponse response = super.readJson(saveExcelMvcResult, GenerateDataStatusResponse.class);

        mockMvc.perform(get("/api/v2/generated_data/" + response.getId()))
            .andExpect(status().isOk())
            .andExpect(content().json("{\"id\":1,\"status\":\"COMPLETED\"} "));

        mockMvc.perform(get("/api/v2/generated_data/" + response.getId() + "/data"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    private void mockDeepmindClientForSpecialOrder(String result) {
        var response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(
            new NStringEntity(result, ContentType.APPLICATION_JSON));
        when(response.getStatusLine()).thenReturn(
            new BasicStatusLine(new ProtocolVersion("https", 0, 0), HttpStatus.SC_OK, "fake reason")
        );
        try {
            when(httpClient.execute(any())).thenReturn(response);
        } catch (IOException ignored) {
        }
    }

    private byte[] getSavedData(GenerateDataStatusResponse response) throws Exception {
        return mockMvc.perform(get("/api/v2/generated_data/" + response.getId() + "/data"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    }

}
