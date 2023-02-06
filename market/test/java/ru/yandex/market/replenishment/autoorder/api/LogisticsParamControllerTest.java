package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.MinAmountUnit;
import ru.yandex.market.replenishment.autoorder.model.dto.LogisticsParamDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;

import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.DELETE;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.INSERT;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.UPDATE;
import static ru.yandex.market.replenishment.autoorder.model.SupplyRouteType.DIRECT;
import static ru.yandex.market.replenishment.autoorder.model.SupplyRouteType.XDOC;

@Slf4j
@DbUnitDataBaseConfig({
    @DbUnitDataBaseConfig.Entry(
        name = "tableType",
        value = "TABLE,VIEW"
    )
})
@WithMockLogin
public class LogisticsParamControllerTest extends ControllerTest {

    @Autowired
    AuditTestingHelper auditTestingHelper;

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2020, 3, 19, 18, 58, 58);

    @Before
    public void mockDate() {
        setTestTime(MOCK_DATE);
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testGet() throws Exception {
        String response = super.readFile("LogisticsParamControllerTest_testGet.json");

        mockMvc.perform(get("/suppliers/123/logistic-params")
                .header("x-supplier-id", 123))
            .andExpect(status().isOk())
            .andExpect(mvcResult -> log.debug("response - {}", mvcResult.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testGetWithWrongSupplierId() throws Exception {
        mockMvc.perform(get("/suppliers/123/logistic-params")
                .header("x-supplier-id", 777)
            )
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 777 not equals supplierId: 123"));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_saveV2AddNew.after.csv")
    public void testSaveV2_AddNew() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[]{1, 2})
                .deliveryTime(7)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .deliveryFrequency(3L)
                .autoProcessing(true)
                .calendarWorkId(12512L)
                .build()
        );

        auditTestingHelper.assertAuditRecordAdded(
            () -> mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                    .header("x-supplier-id", 123)
                    .content(dtoToString(logisticsParamDtos))
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(response -> log.debug("response - {}", response.getResponse().getContentAsString())),
            r -> AuditTestingHelper.assertAuditRecord(r, "logistics_param", INSERT,
                "id", 1,
                "supplier_id", 123,
                "warehouse_id", 171,
                "group_name", null,
                "schedule", "пн,вт",
                "lead_time", 7,
                "min_amount", 5,
                "calendar_work_id", 12512
            )
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveV2_AddNewWithWrongSupplierId() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[]{1, 2})
                .deliveryTime(7)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build()
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .header("x-supplier-id", 777)
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(response -> log.debug("response - {}", response.getResponse().getContentAsString()))
            .andExpect(jsonPath("$.message")
                .value("Header x-supplier-id: 777 not equals supplierId: 123"));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_saveV2UpdateExisted.after.csv")
    public void testSaveV2_UpdateExisted() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(11L)
                .warehouseId(145)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(9)
                .build(),
            LogisticsParamDTO.builder()
                .id(12L)
                .warehouseId(145)
                .groupName("Алиди-1")
                .schedule(new int[]{3, 5})
                .deliveryTime(4)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(7)
                .build(),
            LogisticsParamDTO.builder()
                .id(13L)
                .warehouseId(147)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(11)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(12)
                .build()
        );

        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                    .content(dtoToString(logisticsParamDtos))
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            AuditTestingHelper.expected("logistics_param", UPDATE, "11",
                "schedule", "вт,ср",
                "lead_time", 10,
                "min_amount", 9),
            AuditTestingHelper.expected("logistics_param", UPDATE, "12",
                "schedule", "ср,пт"),
            AuditTestingHelper.expected("logistics_param", UPDATE, "13",
                "schedule", "вт,ср",
                "lead_time", 11,
                "min_amount", 12)
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam_testSyncMonoXDoc.before.csv",
        after = "LogisticsParam_testSyncMonoXDoc.after.csv")
    public void testSyncMonoXDoc() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(10L)
                .warehouseId(172)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(9)
                .build(),
            LogisticsParamDTO.builder()
                .id(11L)
                .warehouseId(147)
                .groupName("user1")
                .schedule(new int[]{3, 5})
                .deliveryTime(4)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(7)
                .active(false)
                .globallyActive(false)
                .build()
        );
        List<LogisticsParamDTO> logisticsParamDtosSupplier2 = List.of(
            LogisticsParamDTO.builder()
                .id(12L)
                .warehouseId(172)
                .groupName("user1")
                .schedule(new int[]{3, 5})
                .deliveryTime(4)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(7)
                .active(true)
                .globallyActive(false)
                .build()
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/1/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/2/logistic-params")
                .content(dtoToString(logisticsParamDtosSupplier2))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(response -> log.debug("response - {}", response.getResponse().getContentAsString()));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_saveV2UpdateGroupName.after.csv")
    public void testSaveV2_UpdateGroupName() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(11L)
                .warehouseId(145)
                .groupName("New group name")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(9)
                .build()
        );
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(response -> log.debug("response - {}", response.getResponse().getContentAsString()));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_deleteByIds.after.csv")
    public void testDeleteByIds() {
        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(delete("/api/v2/suppliers/logistic-params?logisticsParamId=11,12")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            AuditTestingHelper.expected("logistics_param", DELETE, "11",
                "supplier_id", 123,
                "warehouse_id", 145,
                "group_name", null),
            AuditTestingHelper.expected("logistics_param", DELETE, "12",
                "supplier_id", 123,
                "warehouse_id", 145,
                "group_name", "Алиди-1")

        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveWithIds_emptySchedule() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[0])
                .deliveryTime(7)
                .minimumOrderQuantity(5)
                .build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveWithIds_nullSchedule() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(null)
                .deliveryTime(7)
                .minimumOrderQuantity(5)
                .build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveWithIds_wrongDeliveryTime() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(100)
                .minimumOrderQuantity(5)
                .build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveWithIds_doubleGroupName() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(145)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Logistics parameters of warehouse id 171 have not unique group names: Test"));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveWithIds_noDoubleGroupName() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(XDOC)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build(),
            LogisticsParamDTO.builder()
                .warehouseId(145)
                .groupName("Test")
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(5)
                .build());
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testSaveV2_doubleGroupNameInDB() throws Exception {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(145)
                .groupName(null)
                .schedule(new int[]{2, 3})
                .deliveryTime(10)
                .supplyRoute(DIRECT)
                .minimumOrderQuantity(9)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/123/logistic-params")
                .content(dtoToString(logisticsParamDtos))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("There is a logistics parameter with the same supplier_id, warehouse_id and " +
                    "group_name"));
    }

    private <T> String dtoToString(T logisticsParamDTO) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(logisticsParamDTO);
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv")
    public void testGet3p() throws Exception {
        mockMvc.perform(get("/suppliers/901/logistic-params"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))

            .andExpect(jsonPath("$[0].id").value(15L))
            .andExpect(jsonPath("$[0].warehouseId").value(147L))
            .andExpect(jsonPath("$[0].groupName").value(IsNull.nullValue()))
            .andExpect(jsonPath("$[0].schedule[0]").value(1))
            .andExpect(jsonPath("$[0].schedule[1]").value(5))
            .andExpect(jsonPath("$[0].deliveryTime").value(7))
            .andExpect(jsonPath("$[0].deliveryTimeXdoc").value(0))
            .andExpect(jsonPath("$[0].supplyRoute").value("DIRECT"))
            .andExpect(jsonPath("$[0].minimumOrderQuantity").value(3))
            .andExpect(jsonPath("$[0].minimumOrderItemsQuantity").value(0))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].globallyActive").value(true))
            .andExpect(jsonPath("$[0].minAmountUnit").value("RUB"))

            .andExpect(jsonPath("$[1].id").value(16L))
            .andExpect(jsonPath("$[1].warehouseId").value(172L))
            .andExpect(jsonPath("$[1].groupName").value(IsNull.nullValue()))
            .andExpect(jsonPath("$[1].schedule[0]").value(1))
            .andExpect(jsonPath("$[1].schedule[1]").value(5))
            .andExpect(jsonPath("$[1].deliveryTime").value(8))
            .andExpect(jsonPath("$[1].deliveryTimeXdoc").value(0))
            .andExpect(jsonPath("$[1].supplyRoute").value("XDOC"))
            .andExpect(jsonPath("$[1].minimumOrderQuantity").value(0))
            .andExpect(jsonPath("$[1].minimumOrderItemsQuantity").value(4))
            .andExpect(jsonPath("$[1].active").value(false))
            .andExpect(jsonPath("$[1].globallyActive").value(true))
            .andExpect(jsonPath("$[1].minAmountUnit").value("ITEMS"));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_save3PAddNew.after.csv")
    public void testSave3P_AddNew() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .schedule(new int[]{1, 2})
                .deliveryTime(7)
                .supplyRoute(DIRECT)
                .minimumOrderItemsQuantity(5)
                .active(true)
                .globallyActive(true)
                .minAmountUnit(MinAmountUnit.ITEMS)
                .build()
        );

        auditTestingHelper.assertAuditRecordAdded(
            () -> mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/950/logistic-params")
                    .content(dtoToString(logisticsParamDtos))
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            r -> AuditTestingHelper.assertAuditRecord(r, "logistics_param", INSERT,
                "id", 1,
                "supplier_id", 950,
                "warehouse_id", 171,
                "group_name", null,
                "schedule", "пн,вт",
                "lead_time", 7,
                "min_amount", 0
            )
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_update3P.after.csv")
    public void testUpdate3P() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(15L)
                .warehouseId(147)
                .schedule(new int[]{5, 6})
                .deliveryTime(11)
                .supplyRoute(DIRECT)
                .minimumOrderItemsQuantity(12)
                .active(false)
                .globallyActive(true)
                .minAmountUnit(MinAmountUnit.ITEMS)
                .build()
        );

        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/v2/suppliers/901/logistic-params")
                        .content(dtoToString(logisticsParamDtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            AuditTestingHelper.expected("logistics_param", UPDATE, "15",
                "schedule", "пт,сб",
                "lead_time", 11,
                "min_amount", 0)
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam_for_views.before.csv",
        after = "LogisticsParam_views.after.csv")
    public void testViews() {
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_save_null3pmoscow.after.csv")
    public void testSave_null3pMoscow() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .warehouseId(171)
                .schedule(null)
                .deliveryTime(null)
                .supplyRoute(DIRECT)
                .minimumOrderItemsQuantity(5)
                .active(true)
                .globallyActive(true)
                .minAmountUnit(MinAmountUnit.ITEMS)
                .build()
        );

        auditTestingHelper.assertAuditRecordAdded(
            () -> mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/suppliers/950/logistic-params")
                    .content(dtoToString(logisticsParamDtos))
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            r -> AuditTestingHelper.assertAuditRecord(r, "logistics_param", INSERT,
                "id", 1,
                "supplier_id", 950,
                "warehouse_id", 171,
                "group_name", null,
                "schedule", null,
                "lead_time", null,
                "min_amount", 0
            )
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam.before.csv",
        after = "LogisticsParam_update_null3pmoscow.after.csv")
    public void testUpdate_null3pMoscow() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(16L)
                .warehouseId(172)
                .schedule(null)
                .deliveryTime(null)
                .supplyRoute(XDOC)
                .minimumOrderItemsQuantity(12)
                .active(true)
                .globallyActive(true)
                .minAmountUnit(MinAmountUnit.ITEMS)
                .build()
        );

        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/v2/suppliers/901/logistic-params")
                        .content(dtoToString(logisticsParamDtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            AuditTestingHelper.expected("logistics_param", UPDATE, "16",
                "schedule", null,
                "lead_time", null)
        );
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam_moscow3p.before.csv")
    public void testGetMoscow3p() throws Exception {
        mockMvc.perform(get("/suppliers/950/logistic-params"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].id").value(10L))
            .andExpect(jsonPath("$[0].warehouseId").value(171L))
            .andExpect(jsonPath("$[0].groupName").value(IsNull.nullValue()))
            .andExpect(jsonPath("$[0].schedule").isEmpty())
            .andExpect(jsonPath("$[0].deliveryTime").isEmpty())
            .andExpect(jsonPath("$[0].supplyRoute").value("DIRECT"))
            .andExpect(jsonPath("$[0].minimumOrderQuantity").value(0))
            .andExpect(jsonPath("$[0].minimumOrderItemsQuantity").value(5))
            .andExpect(jsonPath("$[0].active").value(true))
            .andExpect(jsonPath("$[0].globallyActive").value(true))
            .andExpect(jsonPath("$[0].minAmountUnit").value("ITEMS"));
    }

    @Test
    @DbUnitDataSet(before = "LogisticsParam_moscow3p.before.csv",
        after = "LogisticsParam_update_existedMoscow3p.after.csv")
    public void testUpdate_existedNull3pMoscow() {
        List<LogisticsParamDTO> logisticsParamDtos = List.of(
            LogisticsParamDTO.builder()
                .id(10L)
                .warehouseId(171)
                .schedule(null)
                .deliveryTime(null)
                .supplyRoute(XDOC)
                .minimumOrderItemsQuantity(5)
                .active(false)
                .globallyActive(false)
                .minAmountUnit(MinAmountUnit.ITEMS)
                .build()
        );

        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(
                    MockMvcRequestBuilders.post("/api/v2/suppliers/950/logistic-params")
                        .content(dtoToString(logisticsParamDtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            AuditTestingHelper.expected("logistics_param", UPDATE, "10",
                "schedule", null,
                "lead_time", null)
        );
    }
}
