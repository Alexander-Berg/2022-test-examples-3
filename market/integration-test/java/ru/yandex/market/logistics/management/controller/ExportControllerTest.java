package ru.yandex.market.logistics.management.controller;

import java.time.LocalDate;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;

class ExportControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Экспорт существующих реестров для связки фф-сд")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetSchedulesRegisterCreation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/schedules?from=1&to=2&type=REGISTER_CREATION"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/schedule_response_register.json"));
    }

    @Test
    @DisplayName("Экспорт существующих заявок для связки фф-сд")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetSchedulesIntakeCreation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/schedules?from=1&to=2&type=INTAKE_CREATION"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/schedule_response_intake.json"));
    }

    @Test
    @DisplayName("При запросе экспорта реестров несуществующей связки фф-сд, возвращается ошибка")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetSchedulesEmpty() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/schedules?from=2&to=1&type=REGISTER_CREATION"))
            .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    @DisplayName("При запросе связки фф-сд, возвращается одна связка")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetPartnerRelation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelation?from=1&to=2"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_relation.json"));
    }

    @Test
    @DisplayName("При запросе связки, возвращается одна и фф-сд и сц-сд")
    @DatabaseSetup("/data/controller/export/partner_relations_with_sc.xml")
    void testGetBothTypesRelation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelation?from=3&to=4"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_relation_with_sc.json"));
    }

    @Test
    @DisplayName("При отсутствии необходимого параметра запроса, возвращается 400")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetPartnerRelationWrongRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelation?to=777"))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    @DisplayName("При запросе несуществующей связки возвращается статус 200 и body null")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetEmptyPartnerRelation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelation?from=666&to=777"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("null"));
    }

    @Test
    @DisplayName("При запросе всех связок фф-сд возвращается список")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetPartnerRelations() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelations"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_relations.json"));
    }

    @Test
    @DisplayName("При запросе всех связок фф-сд + сц-сд возвращается список")
    @DatabaseSetup("/data/controller/export/partner_relations_with_sc.xml")
    void testGetDifferentRelations() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/partnerRelations"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_relations_with_sc.json"));
    }

    @Test
    @DisplayName("При запросе экспорта катофф возвращается список катофф")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetCutoffs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/cutoffs?from=1&to=2"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/cutoffs.json"));
    }

    @Test
    @DisplayName("При запросе экспорта складов для несуществующей связки возвращается пустой список")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void testGetEmptyCutoffs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/cutoffs?from=100&to=200"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/empty_entities.json"));
    }

    @Test
    @DisplayName("При запросе несуществующей XDOC связки для склада назначения FULFILLMENT возвращается пустой список")
    @DatabaseSetup("/data/controller/export/prepare_data.xml")
    void shouldNotReturnPartnerRelationsIfXDocPartnerRelationsDoNotExist() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/xDocPartnerRelations?to=1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/empty_entities.json"));
    }

    @Test
    @DisplayName("При запросе XDOC связки для склада назначения FULFILLMENT, возвращается корректная связка")
    @DatabaseSetup("/data/controller/export/partner_relations_with_xdoc.xml")
    void shouldReturnXDocPartnerRelations() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/export/xDocPartnerRelations?to=1"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_relations_xdoc.json"));
    }

    @Test
    @DatabaseSetup("/data/controller/export/partner_xdoc_warehouses.xml")
    void getXDocWarehouses() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/export/partners/5/xDocWarehouses"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_xdoc_warehouses.json"));
    }

    @Test
    @DisplayName("При запросе экспорта расписаний складов партнера возвращается корректный список")
    @DatabaseSetup(value = "/data/controller/export/partner_warehouses_gates_schedule.xml")
    void getWarehousesGatesSchedule() throws Exception {
        MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        paramsMap.add("from", LocalDate.of(2019, 11, 4).toString());
        paramsMap.add("to", LocalDate.of(2019, 11, 11).toString());

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/export/partners/1/warehousesGatesSchedule")
                .params(paramsMap)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_warehouses_gates_schedule.json"));
    }

    @Test
    @DisplayName("При запросе экспорта расписаний складов партнера (v2) возвращается корректный список")
    @DatabaseSetup("/data/controller/export/partner_warehouses_gates_schedule_v2.xml")
    void getWarehousesGatesScheduleV2() throws Exception {
        MultiValueMap<String, String> paramsMap = new LinkedMultiValueMap<>();
        paramsMap.add("from", LocalDate.of(2021, 6, 21).toString());
        paramsMap.add("to", LocalDate.of(2021, 6, 26).toString());

        mockMvc.perform(
            MockMvcRequestBuilders
                .get("/export/partners/1/warehousesGatesCustomSchedule")
                .params(paramsMap)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(TestUtil.testJson("data/controller/partner_warehouses_gates_schedule_v2.json"));
    }
}
