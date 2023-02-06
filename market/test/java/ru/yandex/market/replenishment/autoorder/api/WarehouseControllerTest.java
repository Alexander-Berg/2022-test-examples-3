package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.calendaring.client.dto.DailyQuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.IntervalQuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.QuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.QuotaType;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.CalendaringService;
import ru.yandex.market.replenishment.autoorder.service.RecommendationService;
import ru.yandex.market.replenishment.autoorder.utils.AuditTestingHelper;

import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.DELETE;
import static ru.yandex.market.mbo.pgaudit.PgAuditChangeType.INSERT;

@WithMockLogin
public class WarehouseControllerTest extends ControllerTest {
    @Autowired
    AuditTestingHelper auditTestingHelper;

    @Autowired
    RecommendationService recommendationService;

    @Before
    public void mockCalendaringService() {
        var info1p = new QuotaInfoResponse(200L, 20L, 33L, 3L);
        var info3p = new QuotaInfoResponse(null, null, 1L, 2L);
        var dailyInfo = new DailyQuotaInfoResponse(
            QuotaType.SUPPLY,
            LocalDate.of(2021, 1, 10),
            info1p,
            info3p,
            null,
            null
        );
        IntervalQuotaInfoResponse response = new IntervalQuotaInfoResponse(Collections.singletonList(dailyInfo));

        CalendaringService calendaringService = mock(CalendaringService.class);
        Mockito.when(
                calendaringService.getDailyQuota(
                    Mockito.any(),
                    Mockito.anyLong(),
                    Mockito.any(),
                    Mockito.any()
                )
            )
            .thenReturn(response);
        ReflectionTestUtils.setField(recommendationService, "calendaringService", calendaringService);
    }

    @Test
    public void testGetWarehouses() throws Exception {
        mockMvc.perform(get("/warehouses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].id").value(145L))
            .andExpect(jsonPath("$[0].name").value("Маршрут"))
            .andExpect(jsonPath("$[1].id").value(147L))
            .andExpect(jsonPath("$[1].name").value("Ростов"))
            .andExpect(jsonPath("$[2].id").value(171L))
            .andExpect(jsonPath("$[2].name").value("Томилино"))
            .andExpect(jsonPath("$[3].id").value(172L))
            .andExpect(jsonPath("$[3].name").value("Софьино"));
    }

    @Test
    public void testGetFulfillmentWarehouses() throws Exception {
        mockMvc.perform(get("/warehouses?type=FULFILLMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(10))
            .andExpect(jsonPath("$[0].id").value(145L))
            .andExpect(jsonPath("$[0].name").value("Маршрут"))
            .andExpect(jsonPath("$[1].id").value(147L))
            .andExpect(jsonPath("$[1].name").value("Ростов"))
            .andExpect(jsonPath("$[2].id").value(171L))
            .andExpect(jsonPath("$[2].name").value("Томилино"))
            .andExpect(jsonPath("$[3].id").value(172L))
            .andExpect(jsonPath("$[3].name").value("Софьино"));
    }

    @Test
    public void testGetXdocWarehouses() throws Exception {
        mockMvc.perform(get("/warehouses?type=XDOC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(402L))
            .andExpect(jsonPath("$[0].name").value("РЦ Климовск Кросс-док"))
            .andExpect(jsonPath("$[1].id").value(47723L))
            .andExpect(jsonPath("$[1].name").value("ПЭК (Томилино)"));
    }

    @Test
    public void testGetAllWarehouses() throws Exception {
        mockMvc.perform(get("/warehouses?type=XDOC,FULFILLMENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(12))
            .andExpect(jsonPath("$[0].id").value(145L))
            .andExpect(jsonPath("$[0].name").value("Маршрут"))
            .andExpect(jsonPath("$[1].id").value(147L))
            .andExpect(jsonPath("$[1].name").value("Ростов"))
            .andExpect(jsonPath("$[2].id").value(171L))
            .andExpect(jsonPath("$[2].name").value("Томилино"))
            .andExpect(jsonPath("$[3].id").value(172L))
            .andExpect(jsonPath("$[3].name").value("Софьино"))
            .andExpect(jsonPath("$[4].id").value(300L))
            .andExpect(jsonPath("$[4].name").value("Екатеринбург"))
            .andExpect(jsonPath("$[5].id").value(301L))
            .andExpect(jsonPath("$[5].name").value("Санкт-Петербург"))
            .andExpect(jsonPath("$[6].id").value(302))
            .andExpect(jsonPath("$[6].name").value("Самара"))
        ;
    }

    @Test
    public void testGetWarehouses_Error() throws Exception {
        mockMvc.perform(get("/warehouses?type=RANDOM"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetItemsPlannedToSend_InvalidIdGetsError() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-01&dateTo=2021-01-31&dateType=ORDER"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Склад с указанным id не существует 1"));
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_InvalidDateFromGetsError() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-81&dateTo=2021-91-31&dateType=ORDER"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid dateFrom format"));

    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_InvalidDateToGetsError() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-01&dateTo=2021-91-31&dateType=ORDER"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid dateTo format"));

    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_InvalidDateBoundsGetsError() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-02&dateTo=2021-01-01&dateType=ORDER"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Дата окончания выборки не может предшествовать дате начала выборки"));
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_InvalidDateTypeGetsError() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-01&dateTo=2021-01-31&dateType=WrongDateType"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Invalid dateType"));

    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_GetByOrderDate() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-10&dateTo=2021-01-10&dateType=ORDER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value("2021-01-10"))
            .andExpect(jsonPath("$[0].countOfItemsPlannedToSend").value(1))
            .andExpect(jsonPath("$[0].countOfRecommendedItems").value(289))
            .andExpect(jsonPath("$[0].countOfNotExportedItems").value(288))
            .andExpect(jsonPath("$[0].itemsLimit").isEmpty())
            .andExpect(jsonPath("$[0].itemsTaken").isEmpty())
            .andExpect(jsonPath("$[0].plannedItemsAutodemandTaken").isEmpty())
            .andExpect(jsonPath("$[0].realItemsLimit").isEmpty())
            .andExpect(jsonPath("$[0].realItemsTaken").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSend.before.csv")
    public void testGetItemsPlannedToSend_GetByDeliveryDate() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-10&dateTo=2021-01-10&dateType=DELIVERY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value("2021-01-10"))
            .andExpect(jsonPath("$[0].countOfItemsPlannedToSend").value(2))
            .andExpect(jsonPath("$[0].countOfRecommendedItems").value(2))
            .andExpect(jsonPath("$[0].countOfNotExportedItems").value(0))
            .andExpect(jsonPath("$[0].itemsLimit").isEmpty())
            .andExpect(jsonPath("$[0].itemsTaken").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSendWithLimits.before.csv")
    public void testGetItemsPlannedToSend_GetByDeliveryDateWithLimits() throws Exception {
        mockMvc.perform(get("/warehouses/1/quotes?dateFrom=2021-01-10&dateTo=2021-01-10&dateType=DELIVERY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value("2021-01-10"))
            .andExpect(jsonPath("$[0].countOfItemsPlannedToSend").value(2))
            .andExpect(jsonPath("$[0].countOfRecommendedItems").value(2))
            .andExpect(jsonPath("$[0].countOfNotExportedItems").value(0))
            .andExpect(jsonPath("$[0].itemsLimit").value(10))
            .andExpect(jsonPath("$[0].itemsTaken").value(20))
            .andExpect(jsonPath("$[0].plannedItemsAutodemandTaken").value(84))
            .andExpect(jsonPath("$[0].realItemsLimit").value(200))
            .andExpect(jsonPath("$[0].realItemsTaken").value(33));
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSendWithLimits.before.csv")
    public void testGetItemsPlannedToSend_GetByDeliveryDateWithLimitsAndDemandType() throws Exception {
        mockMvc.perform(get(
                "/warehouses/1/quotes?dateFrom=2021-01-10&dateTo=2021-01-10&dateType=DELIVERY&demandType=TYPE_1P"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value("2021-01-10"))
            .andExpect(jsonPath("$[0].countOfItemsPlannedToSend").value(2))
            .andExpect(jsonPath("$[0].countOfRecommendedItems").value(2))
            .andExpect(jsonPath("$[0].countOfNotExportedItems").value(0))
            .andExpect(jsonPath("$[0].itemsLimit").value(10))
            .andExpect(jsonPath("$[0].itemsTaken").value(20));
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testGetItemsPlannedToSendTender.before.csv")
    public void testGetItemsPlannedToSend_GetByDeliveryDateTender() throws Exception {
        mockMvc.perform(get(
                "/warehouses/1/quotes?dateFrom=2021-01-10&dateTo=2021-01-10&dateType=DELIVERY&demandType=TENDER"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value("2021-01-10"))
            .andExpect(jsonPath("$[0].countOfItemsPlannedToSend").value(2))
            .andExpect(jsonPath("$[0].countOfRecommendedItems").value(2))
            .andExpect(jsonPath("$[0].countOfNotExportedItems").value(0))
            .andExpect(jsonPath("$[0].itemsLimit").value(10))
            .andExpect(jsonPath("$[0].itemsTaken").value(20));
    }

    @Test
    @WithMockLogin
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv")
    public void testGetRelatedChanges_AccessDenied() throws Exception {
        mockMvc.perform(post("/warehouses/147/regions/show-related-changes")
                .content("[1, 2]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value("Access is denied"));
    }

    @Test
    @WithMockLogin("region-warehouse-mapping-admin")
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv")
    public void testGetRelatedChanges() throws Exception {
        mockMvc.perform(post("/warehouses/147/regions/show-related-changes")
                .content("[1, 2]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pairsForDelete.length()").value(16))
            .andExpect(jsonPath("$.pairsForAdd.length()").value(2))
            .andExpect(jsonPath("$.pairsForAdd[0].warehouseId").value(147))
            .andExpect(jsonPath("$.pairsForAdd[0].regionId").value(1))
            .andExpect(jsonPath("$.pairsForAdd[1].warehouseId").value(147))
            .andExpect(jsonPath("$.pairsForAdd[1].regionId").value(2));
    }

    @Test
    @WithMockLogin
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv")
    public void testUpdate_AccessDenied() throws Exception {
        mockMvc.perform(put("/warehouses/147/regions")
                .content("[1, 2]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message")
                .value("Access is denied"));
    }

    @Test
    @WithMockLogin("region-warehouse-mapping-admin")
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv")
    public void testCheckRegionTypes() throws Exception {
        mockMvc.perform(post("/warehouses/147/regions/show-related-changes")
                .content("[1,7777]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Недопустимый регион: REGION_TYPE_DENIED_FOR_MAPPING"));
    }

    @Test
    @WithMockLogin("region-warehouse-mapping-admin")
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv",
        after = "WarehouseControllerTest.testUpdate.after.csv")
    public void testUpdate() {
        auditTestingHelper.assertAuditRecords(
            () -> mockMvc.perform(put("/warehouses/147/regions")
                    .content("[11225,10176,977,959]")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()),
            getExpectedAuditRecords()
        );
    }

    private AuditTestingHelper.ExpectedAuditRecord[] getExpectedAuditRecords() {
        final String name = "region_warehouse_mapping";
        final int warehouseId = 147;
        final List<Integer> oldRegionIds = List.of(11069, 11029, 11024, 11021, 11020, 11015, 11013, 11012, 11010,
            11004, 10995, 10950, 10946, 977, 959);
        final List<Integer> newRegionIds = List.of(10176, 11225, 959, 977);

        List<AuditTestingHelper.ExpectedAuditRecord> records =
            oldRegionIds.stream().map(regionId -> AuditTestingHelper.expected(name, DELETE,
                regionId + "." + warehouseId,
                "region_id", regionId, "warehouse_id", warehouseId)).collect(Collectors.toList());

        records.add(AuditTestingHelper.expected(name, DELETE, "11225.300",
            "region_id", 11225,
            "warehouse_id", 300));
        records.add(AuditTestingHelper.expected(name, DELETE, "10176.301",
            "region_id", 10176,
            "warehouse_id", 301));

        records.addAll(newRegionIds.stream().map(regionId -> AuditTestingHelper.expected(name, INSERT,
            regionId + "." + warehouseId,
            "region_id", regionId, "warehouse_id", warehouseId)).collect(Collectors.toList()));

        return records.toArray(AuditTestingHelper.ExpectedAuditRecord[]::new);
    }

    @Test
    @DbUnitDataSet(before = "WarehouseControllerTest_testMapping.before.csv")
    public void testNamesGet() throws Exception {
        mockMvc.perform(get("/api/v2/warehouse_regions_names"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }
}
