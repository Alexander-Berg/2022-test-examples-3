package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjInterWarehouseRecommendationQtyDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class InterWarehouseRecommendationsControllerTest extends ControllerTest {

    private static final String PREFIX = "/recommendations/inter-warehouse";
    private static final String PREFIX_EXPORTED = PREFIX + "/exported";

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Before
    public void mockWorkbookConfigAndTimeService() {
        setTestTime(LocalDateTime.of(2020, 9, 1, 0, 0));
    }

    @Test
    @DbUnitDataSet(
        before = "InterWarehouseRecommendationsControllerTest.get.before.csv"
    )
    public void testGet() throws Exception {
        mockMvc.perform(
                get(PREFIX + "?dateFrom=2020-12-01&dateTo=2020-12-09")
            ).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id==3)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@.id==2)]").isEmpty())
            .andExpect(jsonPath("$[?(@.id==3)].orderDate").value("2020-12-04"))
            .andExpect(jsonPath("$[?(@.id==3)].warehouseFrom").value(147))
            .andExpect(jsonPath("$[?(@.id==3)].warehouseTo").value(145))
            .andExpect(jsonPath("$[?(@.id==3)].purchaseQuantity").value(10))
            .andExpect(jsonPath("$[?(@.id==3)].loaded").value(false))
            .andExpect(jsonPath("$[?(@.id==3)].msku").value(100))
            .andExpect(jsonPath("$[?(@.id==3)].ssku").value("000001.100"));
    }

    /**
     * IW-Recommendations adjustment tests
     */

    @Test
    @DbUnitDataSet(
        before = "InterWarehouseRecommendationsControllerTest.adjQty.before.csv",
        after = "InterWarehouseRecommendationsControllerTest.adjQty.after.csv"
    )
    public void testAdjustQuantity() throws Exception {
        AdjInterWarehouseRecommendationQtyDTO recommendationDTO = new AdjInterWarehouseRecommendationQtyDTO();
        recommendationDTO.setAdjustedPurchQty(10L);
        recommendationDTO.setCorrectionReason(1L);

        mockMvc.perform(
                put(PREFIX + "/2/qty")
                    .contentType(MediaType.APPLICATION_JSON_UTF8)
                    .content(dtoToString(recommendationDTO)))
            .andExpect(status().isOk());
    }

    /**
     * Statistics aggregation tests for regular (non-exported) recommendations
     */

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.getStats.before.csv")
    public void testGetStatisticsWithOrderDates() throws Exception {
        validateSingleGroupStatistics(PREFIX + "/stats?dateFrom=2020-12-09&dateTo=2020-12-10");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.getStats.before.csv")
    public void testGetStatisticsWithWarehouseTo() throws Exception {
        validateSingleGroupStatistics(PREFIX + "/stats?warehouseTo=147");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.getStats.before.csv")
    public void testGetStatisticsWithCatteam() throws Exception {
        mockMvc.perform(get(PREFIX + "/stats?catteam=team2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations").value(2))
            .andExpect(jsonPath("$.items").value(20))
            .andExpect(jsonPath("$.sskus").value(1))
            .andExpect(jsonPath("$.volume").value(160))
            .andExpect(jsonPath("$.weight").value(40))
            .andExpect(jsonPath("$.sum").value(400));
    }

    private void validateSingleGroupStatistics(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations").value(2))
            .andExpect(jsonPath("$.items").value(20))
            .andExpect(jsonPath("$.sskus").value(2))
            .andExpect(jsonPath("$.volume").value(90))
            .andExpect(jsonPath("$.weight").value(30))
            .andExpect(jsonPath("$.sum").value(300));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.getFubarStats.before.csv")
    public void testGetFubarStatisticsWithOrderDates() throws Exception {
        mockMvc.perform(get(PREFIX + "/stats?dateFrom=2020-12-09&dateTo=2020-12-10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").value(20))
            .andExpect(jsonPath("$.sskus").value(0))
            .andExpect(jsonPath("$.volume").value(0))
            .andExpect(jsonPath("$.weight").value(0))
            .andExpect(jsonPath("$.sum").value(0));
    }

    /**
     * Statistics aggregation tests for exported recommendations
     */

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithEmptyFilter() throws Exception {
        validateSumOfExportedStatistics(PREFIX_EXPORTED + "/stats");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithDateFrom() throws Exception {
        validateSecondMovementRecommendations(PREFIX_EXPORTED + "/stats?dateFrom=2021-03-26");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithDateTo() throws Exception {
        validateFirstMovementRecommendations(PREFIX_EXPORTED + "/stats?dateTo=2021-03-25");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithWarehouseFrom() throws Exception {
        validateFirstMovementRecommendations(PREFIX_EXPORTED + "/stats?warehouseFrom=147");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithWarehouseTo() throws Exception {
        validateSumOfExportedStatistics(PREFIX_EXPORTED + "/stats?warehouseTo=145");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsWithCatteam() throws Exception {
        validateSecondMovementRecommendations(PREFIX_EXPORTED + "/stats?catteam=TEST_DEPARTMENT_2");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsOnlyManual() throws Exception {
        validateFirstMovementRecommendations(PREFIX_EXPORTED + "/stats?loaded=true");
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedStatisticsOnlyAutomatic() throws Exception {
        validateSecondMovementRecommendations(PREFIX_EXPORTED + "/stats?loaded=false");
    }

    private void validateFirstMovementRecommendations(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations").value(1))
            .andExpect(jsonPath("$.items").value(8))
            .andExpect(jsonPath("$.sskus").value(1))
            .andExpect(jsonPath("$.volume").value(48))
            .andExpect(jsonPath("$.weight").value(32))
            .andExpect(jsonPath("$.sum").value(80));
    }

    private void validateSecondMovementRecommendations(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations").value(1))
            .andExpect(jsonPath("$.items").value(30))
            .andExpect(jsonPath("$.sskus").value(1))
            .andExpect(jsonPath("$.volume").value(180))
            .andExpect(jsonPath("$.weight").value(120))
            .andExpect(jsonPath("$.sum").value(600));
    }

    private void validateSumOfExportedStatistics(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations").value(2))
            .andExpect(jsonPath("$.items").value(38))
            .andExpect(jsonPath("$.sskus").value(2))
            .andExpect(jsonPath("$.volume").value(228))
            .andExpect(jsonPath("$.weight").value(152))
            .andExpect(jsonPath("$.sum").value(680));
    }

    // EXCEL

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.uploadExcel.before.csv",
        after = "InterWarehouseRecommendationsControllerTest.uploadExcel.after.csv")
    public void testUploadExcel() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            PREFIX + "/excel",
            "InterWarehouseRecommendationsControllerTest.uploadExcel.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.uploadExcel.before.csv",
        after = "InterWarehouseRecommendationsControllerTest.uploadExcel.modif.order-date.after.csv")
    public void testUploadExcelWithModifOrderDate() throws Exception {
        excelTestingHelper.upload(
            "PUT",
            PREFIX + "/excel",
            "InterWarehouseRecommendationsControllerTest.uploadExcel.modif.order-date.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv")
    public void testGetExportedRecommendation_Correct() throws Exception {
        mockMvc.perform(get(PREFIX_EXPORTED + "?dateFrom=2021-03-24&dateTo=2021-03-27&interWarehouseMovementIds="))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.msku==100)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@.msku==100)].ssku").value("000001.100"))
            .andExpect(jsonPath("$[?(@.msku==100)].purchaseQuantity").value(10))
            .andExpect(jsonPath("$[?(@.msku==100)].adjustedPurchaseQuantity").value(8))
            .andExpect(jsonPath("$[?(@.msku==100)].manuallyCreated").value(true))
            .andExpect(jsonPath("$[?(@.msku==100)].movementId").value(1))
            .andExpect(jsonPath("$[?(@.msku==100)].correctionReason.id").value(1))
            .andExpect(jsonPath("$[?(@.msku==100)].correctionReason.name")
                .value("1. Не согласен с прогнозом"))
            .andExpect(jsonPath("$[?(@.msku==200)]").isNotEmpty())
            .andExpect(jsonPath("$[?(@.msku==200)].ssku").value("000001.200"))
            .andExpect(jsonPath("$[?(@.msku==200)].purchaseQuantity").value(20))
            .andExpect(jsonPath("$[?(@.msku==200)].adjustedPurchaseQuantity").value(30))
            .andExpect(jsonPath("$[?(@.msku==200)].manuallyCreated").value(false))
            .andExpect(jsonPath("$[?(@.msku==200)].movementId").value(2))
            .andExpect(jsonPath("$[?(@.msku==200)].correctionReason.id").value(2))
            .andExpect(jsonPath("$[?(@.msku==200)].correctionReason.name")
                .value("2. Незапланированное промо"));
    }

    @Test
    @DbUnitDataSet(
        before = "InterWarehouseRecommendationsControllerTest.exported.get.before.csv"
    )
    public void testGetExportedRecommendation_DateFromNotCorrect() throws Exception {
        mockMvc.perform(get(PREFIX_EXPORTED + "?dateFrom=NotCorrectDate&dateTo=2021-03-27"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Incorrect date string: Text 'NotCorrectDate' could not be parsed at index 0"));
    }

    @Test
    @DbUnitDataSet(before = "InterWarehouseRecommendationsControllerTest.getStats.before.csv")
    public void testGetStatisticsWithDeepMindErrorCode() throws Exception {
        mockMvc.perform(get(PREFIX
                + "/stats?dateFrom=2020-12-01&dateTo=2020-12-30&deepmindErrorCode=Error 4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").value(10))
            .andExpect(jsonPath("$.sskus").value(1))
            .andExpect(jsonPath("$.volume").value(80))
            .andExpect(jsonPath("$.weight").value(20))
            .andExpect(jsonPath("$.sum").value(200));
    }

    @Test
    @DbUnitDataSet(
        before = "InterWarehouseRecommendationsControllerTest.getDeepmindErrors.before.csv"
    )
    public void testGetDeepmindErrors() throws Exception {
        mockMvc.perform(get(PREFIX + "/deepmind-errors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.code==1)].text").value("Error1"))
            .andExpect(jsonPath("$[?(@.code==2)].text").value("Error2"));
    }

    private <Type> String dtoToString(Type adjustmentDTO) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(adjustmentDTO);
    }
}
