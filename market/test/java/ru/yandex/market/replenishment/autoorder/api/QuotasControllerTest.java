package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockLogin
@ActiveProfiles("testing")
public class QuotasControllerTest extends ControllerTest {

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetExcelWithQuotas() throws Exception {
        byte[] response = mockMvc.perform(get("/api/v1/quotas/excel?dateFrom=2022-06-27&dateTo=2022-07-02"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
        String actual = buildExcelAsText(response);

        byte[] expectedBytes = this.getClass()
            .getResourceAsStream("quotasControllerTest_qotas[2022-06-27-2022-07-02].xlsx").readAllBytes();
        String expected = this.buildExcelAsText(expectedBytes);
        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetExcelWithQuotasTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/quotas/excel-template"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("excel as text - {}",
                this.buildExcelAsText(result.getResponse().getContentAsByteArray())));
    }

    @SneakyThrows
    private String buildExcelAsText(byte[] response) {
        XSSFWorkbook sheets = new XSSFWorkbook(new ByteArrayInputStream(response));
        XSSFExcelExtractor xssfExcelExtractor = new XSSFExcelExtractor(sheets);
        return xssfExcelExtractor.getText();
    }

    @Test
    public void testGetDepartmentQuotasDatesPatternValidation() throws Exception {
        mockMvc.perform(get("/api/v1/quotas?dateFrom=06-2022-27&dateTo=06-2022-27"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json("{\"message\":\"Parse attempt failed for value [06-2022-27]\"}"));
    }

    @Test
    public void testGetDepartmentQuotasDatesValidation() throws Exception {
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-6-27&dateTo=2022-07-0"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json("{\"message\":\"Parse attempt failed for value [2022-6-27]\"}"));
    }

    @Test
    public void testGetDepartmentQuotasEmptyDatesValidation() throws Exception {
        mockMvc.perform(get("/api/v1/quotas"))
            .andExpect(status().isBadRequest())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json("{\"message\":\"parameter 'dateFrom' is not present\"}"));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetDepartmentQuotas() throws Exception {
        String response = super.readFile("QuotasControllerTest_testGetDepartmentQuotas.json");
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-06-27&dateTo=2022-07-02"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetDepartmentPartOfQuotas() throws Exception {
        String response = super.readFile("QuotasControllerTest_testGetDepartmentPartOfQuotas.json");
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-06-27&dateTo=2022-06-28"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetDepartmentPartOfWithDepAndWarehouseQuotas() throws Exception {
        String response = super.readFile("QuotasControllerTest_testGetDepartmentPartOfWithDepAndWarehouseQuotas.json");
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-06-27&dateTo=2022-06-28&warehouseId=172&departmentId=1"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetDepartmentPartOfWithDepQuotas() throws Exception {
        String response = super.readFile("QuotasControllerTest_testGetDepartmentPartOfWithDepQuotas.json");
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-06-27&dateTo=2022-06-28&departmentId=2"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json(response));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTest.before.csv")
    public void testGetDepartmentPartOfWithWarehouseQuotas() throws Exception {
        mockMvc.perform(get("/api/v1/quotas?dateFrom=2022-06-27&dateTo=2022-06-28&warehouseId=172"))
            .andExpect(status().isOk())
            .andExpect(result -> log.debug("response {}", result.getResponse().getContentAsString()))
            .andExpect(content().json(super.readFile("QuotasControllerTest_testGetDepartmentPartOfWithWarehouseQuotas" +
                ".json")));
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTestSaveNewExcel.before.csv",
        after = "QuotasControllerTestSaveNewExcel.after.csv")
    public void testQuotasSaveNewExcelToDB() throws Exception {
        this.excelTestingHelper.upload(
            "POST",
            "/api/v1/quotas/excel",
            "QuotasControllerTestSaveNewExcel.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "QuotasControllerTestUpdateExcel.before.csv",
        after = "QuotasControllerTestUpdateExcel.after.csv")
    public void testQuotesUpdateExcelToDB() throws Exception {
        this.excelTestingHelper.upload(
            "POST",
            "/api/v1/quotas/excel",
            "QuotasControllerTestUpdateExcel.xlsx"
        ).andExpect(status().isOk());
    }

}
