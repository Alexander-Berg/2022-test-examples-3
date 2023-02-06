package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbo.excel.StreamExcelParser;
import ru.yandex.market.replenishment.autoorder.api.dto.user_fillters.recommendation.RecommendationFilters;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.Recommendation;
import ru.yandex.market.replenishment.autoorder.model.RecommendationFilter;
import ru.yandex.market.replenishment.autoorder.model.TenderStatus;
import ru.yandex.market.replenishment.autoorder.model.dto.DemandIdentityDTO;
import ru.yandex.market.replenishment.autoorder.repository.postgres.ReplenishmentResultToPdbRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SalesRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.utils.Constants;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class DemandControllerTest extends ControllerTest {

    public static final int EXCEL_COLUMN_NUM_MSKU = 2;
    public static final int EXCEL_COLUMN_NUM_STOCK = 27;
    public static final int EXCEL_COLUMN_NUM_STOCK_OVERALL = 28;

    @Autowired
    ReplenishmentResultToPdbRepository replenishmentResultToPdbRepository;

    @Autowired
    SalesRepository salesRepository;

    @Autowired
    RecommendationController recommendationController;

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetBadRequest() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").
                value("demandType and list of id or dateType should be specified in query params"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetBadRequestWithoutDemandType() throws Exception {
        mockMvc.perform(get("/demands")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("demandType and list of id or dateType should be specified in query params"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDates() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))

            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].supplier.name").value("123TEST_SUPPLIER"))
            .andExpect(jsonPath("$[0].supplier.rsId").value("000199"))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings.length()").value(1))
            .andExpect(jsonPath("$[0].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[0].minPurchase").value(1500))
            .andExpect(jsonPath("$[0].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[0].isGrouped").value(false))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(2L))
            .andExpect(jsonPath("$[0].warehouseFrom.name").value("Софьино"))

            .andExpect(jsonPath("$[1].id").value(3))
            .andExpect(jsonPath("$[1].supplier.name").value("123TEST_SUPPLIER"))
            .andExpect(jsonPath("$[1].warehouse.name").value("Софьино"))
            .andExpect(jsonPath("$[1].errors").value("2"))
            .andExpect(jsonPath("$[1].warnings.length()").value(1))
            .andExpect(jsonPath("$[1].warnings[0]").value("AXAPTA_EXPORT_ERRORS"))
            .andExpect(jsonPath("$[1].minPurchase").value(1000))
            .andExpect(jsonPath("$[1].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[1].isGrouped").value(false))
            .andExpect(jsonPath("$[1].autoProcessing").value(true))
            .andExpect(jsonPath("$[1].axaptaStatusId").value(1L));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testGetMinPurchase.before.csv")
    public void testGetMinPurchase() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=2").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].minPurchase").value(42));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndWarehouse() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23" +
                "&warehouseIds=145,147")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].supplier.name").value("123TEST_SUPPLIER"))
            .andExpect(jsonPath("$[0].supplier.rsId").value("000199"))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings.length()").value(1))
            .andExpect(jsonPath("$[0].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[0].minPurchase").value(1500))
            .andExpect(jsonPath("$[0].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(2L));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndNonExistsWarehouse() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23" +
                "&warehouseIds=777")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Склад с указанным id не существует 777"));
    }


    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndSupplier() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-16" +
                "&supplierIds=11,13")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].supplier.name").value("123TEST_SUPPLIER"))
            .andExpect(jsonPath("$[0].supplier.rsId").value("000199"))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings.length()").value(1))
            .andExpect(jsonPath("$[0].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[0].minPurchase").value(1500))
            .andExpect(jsonPath("$[0].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(2L))
            .andExpect(jsonPath("$[0].axaptaStatusDescr").value("Test axapta descr 2"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndNonExistsSupplier() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-16" +
                "&supplierIds=777")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Поставщик с указанным id не существует 777"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.IsExportedBeforePlan.before.csv")
    public void testGetByDatesAndIsExportedBeforePlan() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-16" +
                "&isExportedBeforePlan=true")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndResponsible() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-16" +
                "&responsibleIds=10,20")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].supplier.name").value("123TEST_SUPPLIER"))
            .andExpect(jsonPath("$[0].supplier.rsId").value("000199"))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings.length()").value(1))
            .andExpect(jsonPath("$[0].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[0].minPurchase").value(1500))
            .andExpect(jsonPath("$[0].demandType").value("TYPE_1P"))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(2L));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDatesAndNonExistsResponsible() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-16" +
                "&responsibleIds=777")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Пользователь с указанным id не существует 777"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByOrderDateNow() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=ORDER")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[1].id").value(4));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByOrderDateFrom() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=ORDER&dateFrom=2019-03-18")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[1].id").value(4));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByOrderDateTo() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 11, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=ORDER&dateTo=2019-03-18")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDeliveryDateNow() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 15, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDeliveryDateFrom() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 15, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateFrom=2019-03-15")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByDeliveryDateTo() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 16, 0, 0));
        mockMvc.perform(get("/demands?demandType=TYPE_1P&dateType=DELIVERY&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[1].id").value(3));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetTenderByDates() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&dateType=DELIVERY&dateFrom=2019-03-15&dateTo=2019-03-23")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))

            .andExpect(jsonPath("$[0].id").value(9))
            .andExpect(jsonPath("$[0].supplier").value(IsNull.nullValue()))
            .andExpect(jsonPath("$[0].warehouse.name").value("Маршрут"))
            .andExpect(jsonPath("$[0].errors").isEmpty())
            .andExpect(jsonPath("$[0].warnings").isEmpty())
            .andExpect(jsonPath("$[0].minPurchase").value(IsNull.nullValue()))
            .andExpect(jsonPath("$[0].demandType").value("TENDER"))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].axaptaStatusId").isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demands-tender.before.csv")
    public void testGetTender() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&id=9")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(9))
            .andExpect(jsonPath("$[0].tenderStatus").value(TenderStatus.STARTED.toString()))
            .andExpect(jsonPath("$[0].openTender").value(true));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getTender.before.csv")
    public void testGetTenderByOrderDateAndStatuses() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&orderDateFrom=2019-03-20&orderDateTo=2019-07-20&status" +
                "=REVIEWED&status=PROCESSED")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(4))
            .andExpect(jsonPath("$[0].status").value("REVIEWED"))
            .andExpect(jsonPath("$[0].orderDate").value("2019-03-20"))
            .andExpect(jsonPath("$[1].id").value(6))
            .andExpect(jsonPath("$[1].status").value("PROCESSED"))
            .andExpect(jsonPath("$[1].orderDate").value("2019-07-20"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getTender.before.csv")
    public void testGetTenderByDeliveryDateAndAxaptaStatusIds() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&deliveryDateFrom=2019-03-16&deliveryDateTo=2019-07-25" +
                "&axaptaStatusId=1")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(1L))
            .andExpect(jsonPath("$[0].deliveryDate").value("2019-03-23"))
            .andExpect(jsonPath("$[1].id").value(4))
            .andExpect(jsonPath("$[1].axaptaStatusId").value(1L))
            .andExpect(jsonPath("$[1].deliveryDate").value("2019-03-25"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getTender.before.csv")
    public void testGetTenderByStatusesAndAxaptaStatusIds() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&orderDateFrom=2019-01-01&orderDateTo=2019-12-31&status" +
                "=REVIEWED&status=PROCESSED&axaptaStatusId=2")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(6))
            .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testGetGroupedTenderByIds.before.csv")
    public void testGetGroupedTenderByIds() throws Exception {
        mockMvc.perform(get("/demands?demandType=TENDER&id=1,2")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].id", containsInAnyOrder(1, 2)));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGet1pByOrderDateAndStatuses() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&orderDateFrom=2019-03-20&orderDateTo=2019-07-20&status" +
                "=REVIEWED&status=PROCESSED")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(4))
            .andExpect(jsonPath("$[0].status").value("REVIEWED"))
            .andExpect(jsonPath("$[0].orderDate").value("2019-03-20"))
            .andExpect(jsonPath("$[1].id").value(6))
            .andExpect(jsonPath("$[1].status").value("PROCESSED"))
            .andExpect(jsonPath("$[1].orderDate").value("2019-07-20"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGet1pByDeliveryDateAndAxaptaStatusIds() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&deliveryDateFrom=2019-03-16&deliveryDateTo=2019-07-25" +
                "&axaptaStatusId=1")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[0].axaptaStatusId").value(1L))
            .andExpect(jsonPath("$[0].deliveryDate").value("2019-03-23"))
            .andExpect(jsonPath("$[1].id").value(4))
            .andExpect(jsonPath("$[1].axaptaStatusId").value(1L))
            .andExpect(jsonPath("$[1].deliveryDate").value("2019-03-23"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGet1pByDeliveryDateAndAutoProcessing() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&deliveryDateFrom=2019-03-16&deliveryDateTo=2019-07-25" +
                "&autoProcessing=true")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(3))
            .andExpect(jsonPath("$[0].autoProcessing").value(true))
            .andExpect(jsonPath("$[0].deliveryDate").value("2019-03-23"))
            .andExpect(jsonPath("$[1].id").value(5))
            .andExpect(jsonPath("$[1].autoProcessing").value(true))
            .andExpect(jsonPath("$[1].deliveryDate").value("2019-07-25"))
            .andExpect(jsonPath("$[2].id").value(7))
            .andExpect(jsonPath("$[2].autoProcessing").value(true))
            .andExpect(jsonPath("$[2].deliveryDate").value("2019-07-25"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGet1pByDeliveryDateAndNotAutoProcessing() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&deliveryDateFrom=2019-03-16&deliveryDateTo=2019-07-25" +
                "&autoProcessing=false")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(2))
            .andExpect(jsonPath("$[0].autoProcessing").value(false))
            .andExpect(jsonPath("$[0].deliveryDate").value("2019-03-16"))
            .andExpect(jsonPath("$[1].id").value(4))
            .andExpect(jsonPath("$[1].autoProcessing").value(false))
            .andExpect(jsonPath("$[1].deliveryDate").value("2019-03-23"))
            .andExpect(jsonPath("$[2].id").value(6))
            .andExpect(jsonPath("$[2].autoProcessing").value(false))
            .andExpect(jsonPath("$[2].deliveryDate").value("2019-07-24"))
            .andExpect(jsonPath("$[3].id").value(8))
            .andExpect(jsonPath("$[3].autoProcessing").value(false))
            .andExpect(jsonPath("$[3].deliveryDate").value("2019-07-25"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGet1pByStatusesAndAxaptaStatusIds() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&orderDateFrom=2019-01-01&orderDateTo=2019-12-31&status" +
                "=REVIEWED&status=PROCESSED&axaptaStatusId=2")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(6))
            .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getType1p.before.csv")
    public void testGetDeleted1pById() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=9,10")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))

            .andExpect(jsonPath("$[0].id").value(9))
            .andExpect(jsonPath("$[0].status").value("DELETED"))
            .andExpect(jsonPath("$[0].deletionCause").value("DEMAND_UNION"))
            .andExpect(jsonPath("$[0].linkedDemand").value(576))

            .andExpect(jsonPath("$[1].id").value(10))
            .andExpect(jsonPath("$[1].status").value("DELETED"))
            .andExpect(jsonPath("$[1].deletionCause").value("DEMAND_UNION"))
            .andExpect(jsonPath("$[1].linkedDemand").value(576));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.new.before.csv")
    public void testExportDemandWithRecommendationsToExcel() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));
        byte[] excelData = mockMvc.perform(get("/api/v1/demands/1/excel?demandType"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetResult(excelData, getExpectedMskuRecommendationsMap());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.mono.new.before.csv")
    public void testExportMonoXdocDemandWithRecommendationsToExcel() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));
        byte[] excelData = mockMvc.perform(get("/api/v1/demands/mono/2/excel?demandType"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertNotNull(excelData);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.new.before.csv")
    public void testExportDemandWithRecommendationsToExcelWithRecIdsParam() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));
        byte[] excelData = mockMvc.perform(get("/api/v1/demands/1/excel?demandType&recommendationIds=1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetResultWithRecIdsParam(excelData);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.new.before.csv")
    public void testExportDemandWithRecommendationsToExcelWithRecIdsParamNotExisting() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));
        byte[] excelData = mockMvc.perform(get("/api/v1/demands/1/excel?demandType&recommendationIds=88"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetResultWithRecIdsParamNotExisting(excelData);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.tender.before.csv")
    public void testExportTenderWithRecommendationsToExcel() throws Exception {
        prepareTimeService(LocalDateTime.now().plusDays(3));
        byte[] excelData = mockMvc.perform(get("/api/v1/demands/1/excel?demandType=TENDER&" +
                "headers=index,groupId,categoryName,msku,ssku,title,abc,transit,purchaseQuantity,manufacturer"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetTenderResult(excelData);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.exportNonCumulativeSales.before.csv")
    public void testExportDemandWithNonCumulativeSalesToExcel() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));

        byte[] excelData = mockMvc.perform(get("/api/v1/demands/1/excel?" +
                "getCumulativeSales=false&" +
                "headers=index,groupId,categoryName,msku,ssku,title," +
                "orders7days,orders14days,orders28days,orders56days"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertGetDemandWithNonCumulativeSales(excelData);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.exportNonCumulativeSales.before.csv")
    public void testExportDemandWithNonCumulativeSalesToExcelScbScfCheck() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 2, 1, 0, 0, 0));

        byte[] excelDataCumulativeSalesTrue = mockMvc.perform(get("/api/v1/demands/1/excel?" +
                "getCumulativeSales=true"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        Map<Integer, String> row;
        List<StreamExcelParser.Sheet> cumulativeSalesTrueSheets =
            StreamExcelParser.parse(new ByteArrayInputStream(excelDataCumulativeSalesTrue));
        Map<Integer, Map<Integer, String>> cumulativeSalesTrueRows = cumulativeSalesTrueSheets.get(0).getRows();

        row = cumulativeSalesTrueRows.get(9);
        int cumulativeSalesTrueScb = Integer.parseInt(row.get(42));
        int cumulativeSalesTrueScb1p = Integer.parseInt(row.get(43));
        int cumulativeSalesTrueScf = Integer.parseInt(row.get(45));

        byte[] excelDataCumulativeSalesFalse = mockMvc.perform(get("/api/v1/demands/1/excel?" +
                "getCumulativeSales=false"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        List<StreamExcelParser.Sheet> cumulativeSalesFalseSheets =
            StreamExcelParser.parse(new ByteArrayInputStream(excelDataCumulativeSalesFalse));
        Map<Integer, Map<Integer, String>> cumulativeSalesFalseRows = cumulativeSalesFalseSheets.get(0).getRows();

        row = cumulativeSalesFalseRows.get(9);
        int cumulativeSalesFalseScb = Integer.parseInt(row.get(42));
        int cumulativeSalesFalseScb1p = Integer.parseInt(row.get(43));
        int cumulativeSalesFalseScf = Integer.parseInt(row.get(45));

        // scb и scf не должны зависеть от getCumulativeSales
        assertEquals(cumulativeSalesTrueScb, cumulativeSalesFalseScb);
        assertEquals(cumulativeSalesTrueScb1p, cumulativeSalesFalseScb1p);
        assertEquals(cumulativeSalesTrueScf, cumulativeSalesFalseScf);
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByIds() throws Exception {
        mockMvc.perform(get("/demands?id=1,4&demandType=TYPE_1P")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[1].id").value(4));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.SpecialOrder.before.csv")
    public void testGetWithSpecialWarning() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=1,2").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].warnings.length()").value(0))

            .andExpect(jsonPath("$[1].warnings.length()").value(2))
            .andExpect(jsonPath("$[1].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[1].warnings[1]").value("SPECIAL_ORDER"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.CopyOfDemand.before.csv")
    public void testGetCopyOfDemand() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=1,2").contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].warnings.length()").value(0))
            .andExpect(jsonPath("$[0].copiedDemandId").isEmpty())

            .andExpect(jsonPath("$[1].warnings.length()").value(2))
            .andExpect(jsonPath("$[1].warnings[0]").value("MIN_PURCHASE_REQUIREMENT"))
            .andExpect(jsonPath("$[1].warnings[1]").value("COPY_OF_DEMAND"))
            .andExpect(jsonPath("$[1].copiedDemandId").value(123));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_export2.before.csv",
        after = "DemandControllerTest_export2.after.csv")
    @WithMockLogin
    public void testReplenishmentResultExport2() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 8, 7));
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(Collections.singletonList(new DemandIdentityDTO(2L, 1))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_export_badQuotas.before.csv",
        after = "DemandControllerTest_export_badQuotas.after.csv")
    @WithMockLogin
    public void testReplenishmentResultExport_badQuotas() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 8, 7));
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_1P")
                .content(TestUtils.dtoToString(Collections.singletonList(new DemandIdentityDTO(2L, 1))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Квота в 1 шт на склад Ростов на дату 2019-02-03, на департамент НЕ_ЭиБТ превышена на 7, доступно 1 "));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testExport3p.before.csv",
        after = "DemandControllerTest.testExport3p.after.csv")
    public void testExport3p() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 8, 7));
        mockMvc.perform(post("/api/v2/demands/export?demandType=TYPE_3P")
                .content(TestUtils.dtoToString(Collections.singletonList(new DemandIdentityDTO(2L, 1))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
        assertEquals(0, replenishmentResultToPdbRepository.getReplenishmentResultsAsPdbReplenishments().size());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_testExportWithDistrDemand.before.csv",
        after = "DemandControllerTest_testExportWithDistrDemand.after.csv")
    public void testReplenishmentResultExport_withDistrDemand() throws Exception {
        prepareTimeService(LocalDateTime.of(2019, 3, 18, 8, 7));
        mockMvc.perform(post("/api/v2/demands/export")
                .content(TestUtils.dtoToString(Collections.singletonList(new DemandIdentityDTO(1L, 1))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_simple.before.csv",
        after = "DemandControllerTest_simple.after.csv")
    @WithMockLogin
    public void testSplitByGroupsDemand_Simple() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_mono_xdoc_simple.before.csv",
        after = "DemandControllerTest_mono_xdoc_simple.after.csv")
    @WithMockLogin
    public void testSplitByGroupsMonoXdocDemand_Simple() throws Exception {
        mockMvc.perform(post("/api/v1/demands/mono-xdoc/1000/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_simpleWithoutCatman.before.csv",
        after = "DemandControllerTest_simpleWithoutCatman.after.csv")
    @WithMockLogin
    public void testSplitByGroupsDemand_SimpleWithoutCatman() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest_undo_grouped.before.csv",
        after = "DemandControllerTest_undo_grouped.after.csv")
    @WithMockLogin
    public void testUndoSplitByGroupsDemand_Simple() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/undo-split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest_undo_ungrouped.before.csv",
        after = "DemandControllerTest_undo_ungrouped.after.csv")
    @WithMockLogin
    public void testUndoSplitByGroupsDemand_Ungrouped() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/undo-split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_grouped.before.csv",
        after = "DemandControllerTest_grouped.after.csv")
    @WithMockLogin
    public void testSplitByGroupsDemand_AlreadyGrouped() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByVolume.before.csv",
        after = "DemandControllerTest.testSplitByVolume.after.csv")
    @WithMockLogin
    public void testSplitByVolume() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByMsku.before.csv",
        after = "DemandControllerTest.testSplitByMsku.after.csv")
    @WithMockLogin
    public void testSplitByMsku() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByCategory.before.csv",
        after = "DemandControllerTest.testSplitByCategory.after.csv")
    @WithMockLogin
    public void testSplitByCategory() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByChz.before.csv",
        after = "DemandControllerTest.testSplitByChz.after.csv")
    @WithMockLogin
    public void testSplitByHonestSign() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByWeightWithQuantum.before.csv",
        after = "DemandControllerTest.testSplitByWeightWithQuantum.after.csv")
    @WithMockLogin
    public void testSplitByWeightWithQuantum() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testSplitByPromoPurchase.before.csv",
        after = "DemandControllerTest.testSplitByPromoPurchase.after.csv")
    @WithMockLogin
    public void testSplitByPromoPurchase() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-groups")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGet_BadRequestForDemandId_1() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=123,4")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGet_WrongDemand() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&id=123,4,345")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребностей с типом 1P и c id 123, 345 нет в базе, вероятно произошел реимпорт рекомендаций" +
                    " " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testEditDeliveryDate_WrongDemand() throws Exception {
        String url = "/demands/123/delivery-date";
        String content = "{\"deliveryDate\":\"2020-09-12\"}";
        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.changeDeliveryType.diffGroups.before.csv")
    public void testChangeDeliveryType_diffGroups() throws Exception {
        String url = "/demands/2/change-delivery-type?supplyRouteType=DIRECT";
        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value(
                "Ассортименту потребности 2 соответствуют разные логистические группы параметров"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.new.before.csv")
    public void testExportDemandWithRecommendationsToExcel_WrongDemand() throws Exception {
        mockMvc.perform(get("/api/v1/demands/123/excel"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testExport_WrongDemand() throws Exception {
        String url = "/api/v2/demands/export";

        List<DemandIdentityDTO> request = Collections.singletonList(
            new DemandIdentityDTO(123L, 1));

        mockMvc.perform(post(url).contentType(APPLICATION_JSON_UTF8)
                .content(new ObjectMapper().writeValueAsString(request))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testGetByParent3pDemandIdNotExistsThrowError() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_3P&parentDemandId=11")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Entity of class ParentDemand3pRepository with id 11 is not " +
                "exists"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testExport3pWithParent.before.csv")
    public void testGetByParent3pDemandIdReturnDemands() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_3P&parentDemandId=1")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.id==1)].deliveryDate").value("2019-02-03"))
            .andExpect(jsonPath("$[?(@.id==2)].deliveryDate").value("2019-02-04"))
            .andExpect(jsonPath("$[?(@.id==1)].minPurchase").value(1500))
            .andExpect(jsonPath("$[?(@.id==1)].minPurchaseItems").value(0))
            .andExpect(jsonPath("$[?(@.id==2)].minPurchase").value(0))
            .andExpect(jsonPath("$[?(@.id==2)].minPurchaseItems").value(123));
    }

    @Test
    public void testGetAllAxaptaStatuses() throws Exception {
        mockMvc.perform(get("/demands/axapta-status")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(39))
            .andExpect(jsonPath("$[?(@.id==1)].name").value("Ошибка создания заголовка " +
                "заказа - не найден договор\\нет условий оплаты"))
            .andExpect(jsonPath("$[?(@.id==39)].name").value("Заказ отменен"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.updateXdocDate.before.csv",
        after = "DemandControllerTest.updateXdocDate.after.csv")
    public void updateXdocDate() throws Exception {
        String url = "/demands/1/xdoc-date";
        String content = "{\"xdocDate\":\"2020-09-12\"}";

        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(content))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.withRepositoryData.before.csv")
    public void testEditXdocDate_WrongDemand() throws Exception {
        String url = "/demands/123/xdoc-date";
        String content = "{\"xdocDate\":\"2020-09-12\"}";
        mockMvc.perform(put(url).contentType(APPLICATION_JSON_UTF8)
                .content(content)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Потребности с типом 1P и c id 123 нет в базе, вероятно произошел реимпорт рекомендаций " +
                    "и у них поменялись id, попробуйте снова найти их на экране с календарем и " +
                    "открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    private void prepareTimeService(final LocalDateTime now) {
        TestUtils.mockTimeService(timeService, now);
    }

    private void assertGetResult(byte[] excelData, Map<Long, List<Recommendation>> expectedMskuRecommendations) {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));
        assertEquals(sheets.size(), 1);

        Map<Integer, Map<Integer, String>> rows = sheets.get(0).getRows();
        assertEquals(rows.size(), 10);

        Map<Integer, String> row;
        int currentRow = 0;

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Потребность:");
        assertEquals(row.get(1), "1");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 3);
        assertEquals(row.get(0), "Склад:");
        assertEquals(row.get(1), "Томилино");
        assertEquals(row.get(2), "171");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 3);
        assertEquals(row.get(0), "Поставщик:");
        assertEquals(row.get(1), "'Поставщик №020'");
        assertEquals(row.get(2), "020");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата отправки:");
        assertEquals("2019-01-02", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата поставки:");
        assertEquals("2019-02-03", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Тип поставки:");
        assertEquals(row.get(1), "Прямая");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(2, row.size());
        assertEquals("Статус:", row.get(0));
        assertNotNull(row.get(1));

        // Разделитель пустым рядом
        assertNull(rows.get(currentRow++));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), Constants.REPLENISHMENT_COLUMNS.size());
        Set<String> headers = new HashSet<>(row.values());
        headers.removeAll(Constants.REPLENISHMENT_COLUMNS.values());
        assertEquals(headers.size(), 0);

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.get(2), "100");
        assertEquals(row.get(3), "020.111");
        assertEquals(row.get(75), "Да");
        assertRowEqualsRecommendation(row, expectedMskuRecommendations);

        row = rows.get(currentRow);
        assertNotNull(row);

        assertEquals(row.get(2), "200");
        assertEquals(row.get(3), "020.111");
        assertEquals(row.get(75), "Нет");
        assertRowEqualsRecommendation(row, expectedMskuRecommendations);


    }

    private void assertRowEqualsRecommendation(Map<Integer, String> row,
                                               Map<Long, List<Recommendation>> expectedMskuRecommendations) {
        Long msku = Long.parseLong(row.get(EXCEL_COLUMN_NUM_MSKU));
        List<Recommendation> mskuRecommendations = expectedMskuRecommendations.get(msku);
        assertNotNull(mskuRecommendations, "Recommendation with msku " + msku
            + " doesn't present in the expected recommendations");
        assertEquals(1, mskuRecommendations.size(), "Recommendation with msku " + msku
            + " doesn't present in the expected recommendations");
        Recommendation recommendation = mskuRecommendations.get(0);
        assertEquals(Long.toString(recommendation.getStock()), row.get(EXCEL_COLUMN_NUM_STOCK));
        assertEquals(Long.toString(recommendation.getStockOverall()), row.get(EXCEL_COLUMN_NUM_STOCK_OVERALL));
    }

    private void assertGetTenderResult(byte[] excelData) {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));
        assertEquals(sheets.size(), 1);

        Map<Integer, Map<Integer, String>> rows = sheets.get(0).getRows();
        assertEquals(rows.size(), 9);

        Map<Integer, String> row;
        int currentRow = 0;

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Потребность:");
        assertEquals(row.get(1), "1");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 3);
        assertEquals(row.get(0), "Склад:");
        assertEquals(row.get(1), "Маршрут");
        assertEquals(row.get(2), "145");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 1);
        assertEquals(row.get(0), "Поставщик:");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата отправки:");
        assertEquals(row.get(1), "2019-01-02");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата поставки:");
        assertEquals(row.get(1), "2019-01-09");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Тип поставки:");
        assertEquals(row.get(1), "Тендер");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2); //why expected 1?
        assertEquals(row.get(0), "Статус:");
        // Assert status?

        // Разделитель пустым рядом
        assertNull(rows.get(currentRow++));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 13);

        row = rows.get(currentRow);
        assertNotNull(row);

        assertEquals(row.get(6), "vendorСode1");
    }

    private void assertGetResultWithRecIdsParam(byte[] excelData) {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));
        assertEquals(sheets.size(), 1);

        Map<Integer, Map<Integer, String>> rows = sheets.get(0).getRows();
        assertEquals(rows.size(), 9);

        Map<Integer, String> row;
        int currentRow = 0;

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Потребность:");
        assertEquals(row.get(1), "1");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 3);
        assertEquals(row.get(0), "Склад:");
        assertEquals(row.get(1), "Томилино");
        assertEquals(row.get(2), "171");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 3);
        assertEquals(row.get(0), "Поставщик:");
        assertEquals(row.get(1), "'Поставщик №020'");
        assertEquals(row.get(2), "020");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата отправки:");
        assertEquals("2019-01-02", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата поставки:");
        assertEquals("2019-02-03", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Тип поставки:");
        assertEquals(row.get(1), "Прямая");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(2, row.size());
        assertEquals("Статус:", row.get(0));
        assertNotNull(row.get(1));

        // Разделитель пустым рядом
        assertNull(rows.get(currentRow++));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), Constants.REPLENISHMENT_COLUMNS.size());
        Set<String> headers = new HashSet<>(row.values());
        headers.removeAll(Constants.REPLENISHMENT_COLUMNS.values());
        assertEquals(headers.size(), 0);

        row = rows.get(currentRow);
        assertNotNull(row);

        assertEquals(row.get(2), "100");
        assertEquals(row.get(3), "020.111");
    }

    private void assertGetResultWithRecIdsParamNotExisting(byte[] excelData) {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));
        assertEquals(sheets.size(), 1);

        Map<Integer, Map<Integer, String>> rows = sheets.get(0).getRows();
        assertEquals(rows.size(), 8);
    }

    private void assertGetDemandWithNonCumulativeSales(byte[] excelData) {
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));
        assertEquals(sheets.size(), 1);

        Map<Integer, Map<Integer, String>> rows = sheets.get(0).getRows();
        assertEquals(9, rows.size());

        Map<Integer, String> row;
        int currentRow = 0;

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Потребность:");
        assertEquals(row.get(1), "1");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(3, row.size());
        assertEquals(row.get(0), "Склад:");
        assertEquals(row.get(1), "Томилино");
        assertEquals(row.get(2), "171");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(3, row.size());
        assertEquals(row.get(0), "Поставщик:");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата отправки:");
        assertEquals(row.get(1), "2019-01-02");

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Дата поставки:");
        assertEquals("2019-02-03", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Тип поставки:");
        assertEquals("Прямая", row.get(1));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(row.size(), 2);
        assertEquals(row.get(0), "Статус:");
        assertNotNull(row.get(1));

        // Разделитель пустым рядом
        assertNull(rows.get(currentRow++));

        row = rows.get(currentRow++);
        assertNotNull(row);

        assertEquals(16, row.size());

        row = rows.get(currentRow);
        assertNotNull(row);

        assertEquals("30", row.get(9));
        assertEquals("2", row.get(10));
        assertEquals("2", row.get(11));
        assertEquals("2", row.get(12));
    }

    private Map<Long, List<Recommendation>> getExpectedMskuRecommendationsMap() {
        RecommendationFilters recommendationFilters = new RecommendationFilters(new RecommendationFilter(), null);
        recommendationFilters.getFilter().setDemandIds(List.of(1L));
        List<Recommendation> expectedRecommendations =
            recommendationController.replenishmentWithCountPost(DemandType.TYPE_1P, recommendationFilters)
                .getRecommendations();
        return expectedRecommendations.stream()
            .collect(Collectors.groupingBy(Recommendation::getMsku));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_NonZeroDemand.before.csv")
    public void testGetNonZeroDemand() throws Exception {
        mockMvc.perform(get("/api/v1/demands/non-zero-demand?recommendationId=1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2L))
            .andExpect(jsonPath("$.warehouse").value("Томилино"))
            .andExpect(jsonPath("$.supplier").value("Ватсон"))
            .andExpect(jsonPath("$.date").value("2020-10-24"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_NonZeroDemand_empty.before.csv")
    public void testGetNonZeroDemand_empty() throws Exception {
        mockMvc.perform(get("/api/v1/demands/non-zero-demand?recommendationId=1001"))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_NonZeroDemand_orderByDate.before.csv")
    public void testGetNonZeroDemand_orderByDate() throws Exception {
        mockMvc.perform(get("/api/v1/demands/non-zero-demand?recommendationId=1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3L))
            .andExpect(jsonPath("$.warehouse").value("Томилино"))
            .andExpect(jsonPath("$.supplier").value("Ватсон"))
            .andExpect(jsonPath("$.date").value("2020-10-24"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_NonZeroDemand.before.csv")
    public void testGetNonZeroDemand_badRecId() throws Exception {
        mockMvc.perform(get("/api/v1/demands/non-zero-demand?recommendationId=42"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Couldn't find recommendation '42'")
            );
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest_NonZeroDemand-hasntCheaper.before.csv")
    public void testGetNonZeroDemand_hasntCheaper() throws Exception {
        mockMvc.perform(get("/api/v1/demands/non-zero-demand?recommendationId=1001"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Recommendation '1001' hasn't cheaper one")
            );
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getWarningCounts.before.csv")
    public void testGetWarningCounts_isOk() throws Exception {
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2019, 3, 20));
        mockMvc.perform(get("/api/v1/demands/warnings?demandType=TYPE_1P&demandIds=1&demandIds=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.SCB_OVER_MAX_LIFETIME").value(1))
            .andExpect(jsonPath("$.SCF_OVER_MAX_LIFETIME").value(1));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.getWarningCounts.before.csv")
    public void testGetWarningCounts_isWrong() throws Exception {
        mockMvc.perform(get("/api/v1/demands/warnings?demandType=TYPE_1P&demandIds=34"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом 1P и c id 34 нет в базе, вероятно произошел реимпорт рекомендаций" +
                    " и у них поменялись id, попробуйте снова найти их на экране с календарем и открыть " +
                    "заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.truckSplit.before.csv")
    public void testTruckSplit() throws Exception {
        mockMvc.perform(get("/api/v1/demands/1/trucks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trucks.length()").value(2))
            .andExpect(jsonPath("$.trucks[0].pallets").value(2))
            .andExpect(jsonPath("$.trucks[0].weight").value(178))
            .andExpect(jsonPath("$.trucks[0].fullness").value(0.5))
            .andExpect(jsonPath("$.trucks[0].items").value(17))
            .andExpect(jsonPath("$.trucks[1].pallets").value(1))
            .andExpect(jsonPath("$.trucks[1].weight").value(78))
            .andExpect(jsonPath("$.trucks[1].fullness").value(0.25))
            .andExpect(jsonPath("$.trucks[1].items").value(7))
            .andExpect(jsonPath("$.minPallets").value(1))
            .andExpect(jsonPath("$.maxPallets").value(4))
            .andExpect(jsonPath("$.palletMaxHeight").value(30))
            .andExpect(jsonPath("$.palletMaxWeight").value(110))
            .andExpect(jsonPath("$.truckMaxWeight").value(250));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.truckSplit.before.csv")
    public void testTruckSplitWithFail() throws Exception {
        mockMvc.perform(get("/api/v1/demands/2/trucks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.error").value("Количество товара не кратно кванту товара в рекомендации"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.truckSplit.before.csv")
    public void testTruckSplitWithUnknownDemand() throws Exception {
        mockMvc.perform(get("/api/v1/demands/777/trucks"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом 1P и c id 777 нет в базе, вероятно произошел " +
                    "реимпорт рекомендаций и у них поменялись id, попробуйте снова найти их на экране " +
                    "с календарем и открыть заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testGetByCatteamIdFilterDemands.before.csv")
    public void testGetByCatteamIdFilterDemands() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&deliveryDateFrom=2019-01-16" +
                "&deliveryDateTo=2019-07-25&catteamId=1")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].catteamId").value(1))

            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].catteamId").value(1));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testGetByCatteamIdFilterDemands.before.csv")
    public void testGetByNotFoundCatteamIdFilterDemands() throws Exception {
        mockMvc.perform(get("/demands?demandType=TYPE_1P&deliveryDateFrom=2019-01-16" +
                "&deliveryDateTo=2019-07-25&catteamId=777")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.truckSplitDemandIntoGroupsByTrucks.before.csv")
    public void testTruckSplitDemandIntoGroupsByTrucksWithFail() throws Exception {
        mockMvc.perform(post("/api/v1/demands/2/split-by-trucks"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message").value("Ошибка при разбиении на группы по грузовикам: " +
                "Количество товара не кратно кванту товара в рекомендации"));
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.truckSplitDemandIntoGroupsByTrucks.before.csv",
        after = "DemandControllerTest.truckSplitDemandIntoGroupsByTrucks.after.csv"
    )
    public void testTruckSplitDemandIntoGroupsByTrucks() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-trucks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trucks.length()").value(3))
            .andExpect(jsonPath("$.trucks[0].pallets").value(2))
            .andExpect(jsonPath("$.trucks[0].weight").value(178))
            .andExpect(jsonPath("$.trucks[0].fullness").value(0.5))
            .andExpect(jsonPath("$.trucks[0].items").value(17))
            .andExpect(jsonPath("$.trucks[1].pallets").value(3))
            .andExpect(jsonPath("$.trucks[1].weight").value(234))
            .andExpect(jsonPath("$.trucks[1].fullness").value(0.75))
            .andExpect(jsonPath("$.trucks[1].items").value(21))
            .andExpect(jsonPath("$.trucks[2].pallets").value(3))
            .andExpect(jsonPath("$.trucks[2].weight").value(234))
            .andExpect(jsonPath("$.trucks[2].fullness").value(0.75))
            .andExpect(jsonPath("$.trucks[2].items").value(21))
            .andExpect(jsonPath("$.minPallets").value(1))
            .andExpect(jsonPath("$.maxPallets").value(4))
            .andExpect(jsonPath("$.palletMaxHeight").value(30))
            .andExpect(jsonPath("$.palletMaxWeight").value(110))
            .andExpect(jsonPath("$.truckMaxWeight").value(250));
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.truckSplitDemandIntoGroupsByTrucksWithSameMsku.before.csv",
        after = "DemandControllerTest.truckSplitDemandIntoGroupsByTrucksWithSameMsku.after.csv"
    )
    public void testTruckSplitDemandIntoGroupsByTrucksWithSameMskuAndZeroAdjPurchQty() throws Exception {
        mockMvc.perform(post("/api/v1/demands/1/split-by-trucks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trucks.length()").value(2))

            .andExpect(jsonPath("$.trucks[0].pallets").value(4))
            .andExpect(jsonPath("$.trucks[0].weight").value(240))
            .andExpect(jsonPath("$.trucks[0].fullness").value(1))
            .andExpect(jsonPath("$.trucks[0].items").value(8))

            .andExpect(jsonPath("$.trucks[1].pallets").value(2))
            .andExpect(jsonPath("$.trucks[1].weight").value(116))
            .andExpect(jsonPath("$.trucks[1].fullness").value(0.5))
            .andExpect(jsonPath("$.trucks[1].items").value(4))

            .andExpect(jsonPath("$.minPallets").value(1))
            .andExpect(jsonPath("$.maxPallets").value(4))
            .andExpect(jsonPath("$.palletMaxHeight").value(30))
            .andExpect(jsonPath("$.palletMaxWeight").value(110))
            .andExpect(jsonPath("$.truckMaxWeight").value(250));
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.demandAdjustResult.before.csv",
        after = "DemandControllerTest.demandAdjustResult.after.csv"
    )
    public void testDemandAdjustResult() throws Exception {
        mockMvc.perform(post("/api/v1/demands/101/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));
        mockMvc.perform(post("/api/v1/demands/101/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность №101 уже отредактирована копией №1"));
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.demandAdjustResultMonoXdoc.before.csv",
        after = "DemandControllerTest.demandAdjustResultMonoXdoc.after.csv"
    )
    public void testDemandAdjustResultMonoXdoc() throws Exception {
        mockMvc.perform(post("/api/v1/demands/101/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(1));
        mockMvc.perform(post("/api/v1/demands/101/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность №101 уже отредактирована копией №1"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandAdjustResult.before.csv")
    public void testDemandAdjustResultFailDemandId() throws Exception {
        mockMvc.perform(post("/api/v1/demands/777/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Потребности с типом 1P и c id 777 нет в базе, вероятно произошел реимпорт рекомендаций" +
                    " и у них поменялись id, попробуйте снова найти их на экране с календарем и открыть " +
                    "заново (не переживайте, ваши корректировки не были перезаписаны)"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandAdjustResult.before.csv")
    public void testDemandAdjustResultFailStatus() throws Exception {
        mockMvc.perform(post("/api/v1/demands/104/adjust-result?resultCorrectionReasonId=1"))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность №104 не отправлена в AX"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledEmpty() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_1P)
                .content(TestUtils.dtoToString(List.of()))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.demandMarkAsHandled.before.csv",
        after = "DemandControllerTest.demandMarkAsHandled1P.after.csv"
    )
    public void testMarkAsHandledSimple1P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_1P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(1L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.demandMarkAsHandled.before.csv",
        after = "DemandControllerTest.demandMarkAsHandled3P.after.csv"
    )
    public void testMarkAsHandledSimple3P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_3P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(3L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(
        before = "DemandControllerTest.demandMarkAsHandled.before.csv",
        after = "DemandControllerTest.demandMarkAsHandledTender.after.csv"
    )
    public void testMarkAsHandledSimpleTender() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TENDER)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(5L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledVersionFail1P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_1P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(1L, 2))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #1 была изменена. " +
                    "Пожалуйста, перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledVersionFail3P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_3P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(3L, 2))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #3 была изменена. " +
                    "Пожалуйста, перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledVersionFailTender() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TENDER)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(5L, 2))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #5 была изменена. " +
                    "Пожалуйста, перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledStatusFail3P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_3P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(4L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #4 была изменена. " +
                    "Пожалуйста, перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledStatusFailTender() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TENDER)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(6L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #6 была изменена. " +
                    "Пожалуйста, перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.demandMarkAsHandled.before.csv")
    public void testMarkAsHandledStatusFail1P() throws Exception {
        mockMvc
            .perform(post("/api/v1/demands/handled?demandType=" + DemandType.TYPE_1P)
                .content(TestUtils.dtoToString(List.of(new DemandIdentityDTO(2L, 3))))
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isIAmATeapot())
            .andExpect(jsonPath("$.message")
                .value("Потребность #2 была изменена. Пожалуйста, " +
                    "перезагрузите страницу, чтобы увидеть актуальные данные"));
    }

    @Test
    @DbUnitDataSet(before = "DemandControllerTest.testGetGroupedTenderDemands.before.csv")
    public void testGetGroupedTenderDemands() throws Exception {
        mockMvc.perform(get("/api/v1/demands/1/grouped?demandType=" + DemandType.TENDER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$", containsInAnyOrder(10, 20)));
    }
}
