package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.Every.everyItem;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class SupplierScheduleControllerTest extends ControllerTest {

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2020, 3, 19, 18, 58, 58);
    private static final String MOCK_DATE_STRING = "2020-03-19T18:58:58";

    @Before
    public void mockDate() {
        setTestTime(MOCK_DATE);
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv")
    public void testGetSupplierSchedules() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/1/schedule")
                .content("[]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())

            .andExpect(jsonPath("$.length()").value(2))

            .andExpect(jsonPath("$[0].id").value(11L))
            .andExpect(jsonPath("$[0].dayOfWeek").value("MO"))
            .andExpect(jsonPath("$[0].timeStart").value("00:00:00"))
            .andExpect(jsonPath("$[0].timeEnd").value("14:00:00"))
            .andExpect(jsonPath("$[0].warehouseId").value(145L))
            .andExpect(jsonPath("$[0].updatedTs").value(MOCK_DATE_STRING))

            .andExpect(jsonPath("$[1].id").value(12L))
            .andExpect(jsonPath("$[1].dayOfWeek").value("MO"))
            .andExpect(jsonPath("$[1].timeStart").value("15:00:00"))
            .andExpect(jsonPath("$[1].timeEnd").value("19:00:00"))
            .andExpect(jsonPath("$[1].warehouseId").value(145L))
            .andExpect(jsonPath("$[0].updatedTs").value(MOCK_DATE_STRING));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_save_or_delete.before.csv",
        after = "SupplierScheduleControllerTest_save.after.csv")
    public void testSaveSupplierSchedules() throws Exception {
        String content = "{\n" +
            "  \"timeStart\" : \"12:15:00\",\n" +
            "  \"timeEnd\" : \"17:30:00\",\n" +
            "  \"warehouseId\" : 147,\n" +
            "  \"dayOfWeek\" : \"FR\"\n" +
            "}";
        mockMvc.perform(post("/api/v1/suppliers/3/schedule")
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testSaveSupplierSchedules_Crossing() throws Exception {
        String content = "{\n" +
            "  \"timeStart\" : \"11:00:00\",\n" +
            "  \"timeEnd\" : \"20:00:00\",\n" +
            "  \"warehouseId\" : 145,\n" +
            "  \"dayOfWeek\" : \"MO\"\n" +
            "}";
        mockMvc.perform(post("/api/v1/suppliers/1/schedule")
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Пересечение с уже заданным периодом 00:00 - 14:00\\n" +
                    "Пересечение с уже заданным периодом 15:00 - 19:00"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testUpdateSupplierSchedules_BadTimePeriod() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/11?timeStart=12:00:00&timeEnd=11:00:00")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Начало временного слота задано позже окончания"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testUpdateSupplierSchedules_Crossing() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/11?timeStart=11:00:00&timeEnd=20:00:00")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Пересечение с уже заданным периодом 15:00 - 19:00"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testUpdateSupplierSchedules_WithoutDate() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/11?timeStart=11:00:00")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("parameter 'timeEnd' is not present"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testUpdateSupplierSchedules_NotParseDate() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/11?timeStart=11:00:00&timeEnd=end_of_day")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Illegal time type"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_simple.before.csv")
    public void testUpdateSupplierSchedules_NotSchedule() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/42?timeStart=11:00:00&timeEnd=20:00:00")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Не найден временной слот с идентификатором 42"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_simple.before.csv",
        after = "SupplierScheduleControllerTest_update.after.csv")
    public void testUpdateSupplierSchedules() throws Exception {
        mockMvc.perform(put("/api/v1/suppliers/schedule/11?timeStart=10:00:00&timeEnd=11:00:00")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_save_or_delete.before.csv",
        after = "SupplierScheduleControllerTest_delete.after.csv")
    public void testDeleteSupplierSchedules() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/schedule/11")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_delete_last_day.before.csv",
        after = "SupplierScheduleControllerTest_delete_last_day.before.csv")
    public void testDeleteSupplierSchedules_lastDay() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/schedule/11")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Нельзя удалить последний временной слот в расписании"));
    }

    @Test
    public void testGetSupplierSchedulesByRsId_RsIdIsAbsent() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/schedule?rsId=1")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Запрошено расписание для несуществующего поставщика rs_id = 1"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_ByRsId_WarehouesIsAbsent.before.csv")
    public void testGetSupplierSchedulesByRsId_WarehouesIsAbsent() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/schedule?rsId=1&warehouseIds=1&warehouseIds=2&warehouseIds=3")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Запрошено расписание на несуществующие склады: 1, 3"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_ByRsId.before.csv")
    public void testGetSupplierSchedulesByRsId() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/schedule?rsId=1&warehouseIds=1&warehouseIds=2")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].rsId").value("1"))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[1].active").value(false))
            .andExpect(jsonPath("$[2].active").value(false))
            .andExpect(jsonPath("$[?(@.dayOfWeek=='FR')].dayOfWeek").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "SupplierScheduleControllerTest_ByRsId.before.csv")
    public void testGetSupplierSchedulesByUpdatedTs() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/schedule?updatedSince=2020-03-20T18:58:58")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].rsId").value("1"))
            .andExpect(jsonPath("$[1].rsId").value("2"))
            .andExpect(jsonPath("$[2].rsId").value("2"))
            .andExpect(jsonPath("$[?(@.dayOfWeek=='FR')].dayOfWeek", everyItem(is("FR"))));
    }
}
