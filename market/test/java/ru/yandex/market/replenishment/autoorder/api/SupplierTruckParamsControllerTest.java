package ru.yandex.market.replenishment.autoorder.api;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbo.excel.StreamExcelParser;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.excel.SupplierTruckItemsParamsExcelReader;
import ru.yandex.market.replenishment.autoorder.service.excel.SupplierTruckParamsExcelReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class SupplierTruckParamsControllerTest extends ControllerTest {

    private final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv")
    public void testWrongPathSupplierId() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/supplier/99/truck_params/excel",
                "SupplierTruckParamsForUpload.xlsx"
        ).andExpect(jsonPath("$.message")
                .value("Поставщик с указанным id не существует 99"));

    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv",
            after = "SupplierTruckParamsControllerTest_uploadExcelWithEmptyItemsParams.after.csv")
    public void testUploadEmptyItemsParamsExcel() throws Exception {
        excelTestingHelper.upload(
                "POST",
                "/api/v1/supplier/1/truck_params/excel",
                "SupplierTruckParamsForUploadEmptyItemsParams.xlsx"
        ).andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv")
    public void testUploadExcelWrongMsku() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/supplier/1/truck_params/excel",
                        "SupplierTruckParamsForUploadWrongMSKU.xlsx"
                )
                .andExpect(jsonPath("$.message")
                        .value("Во втором листе найден несуществующий MSKU"));

    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv")
    public void testUploadExcelEmptyMsku() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/supplier/1/truck_params/excel",
                        "SupplierTruckParamsForUploadEmptyMSKU.xlsx"
                )
                .andExpect(jsonPath("$.message")
                        .value("В строке 2 не заполнено одно из обязательных полей"));

    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv")
    public void testUploadExcelMskuWithEmptyParams() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/supplier/1/truck_params/excel",
                        "SupplierTruckParamsForUploadMSKUwithEmptyParams.xlsx"
                )
                .andExpect(jsonPath("$.message")
                        .value("Для msku: 987 должно быть заполнено хотя бы одно поле"));

    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv",
            after = "SupplierTruckParamsControllerTest_uploadExcel.after.csv")
    public void testUploadExcel() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/supplier/1/truck_params/excel",
                        "SupplierTruckParamsForUpload.xlsx"
                )
                .andExpect(status().isOk());

    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_uploadExcel.before.csv",
            after = "SupplierTruckParamsControllerTest_uploadExcelEmptyOptionalParams.after.csv")
    public void testUploadExcelEmptyOptionalParams() throws Exception {
        excelTestingHelper.upload(
                        "POST",
                        "/api/v1/supplier/1/truck_params/excel",
                        "SupplierTruckParamsForUploadEmptyOptionalParams.xlsx"
                )
                .andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_getExcel.before.csv")
    public void testGet() throws Exception {
        byte[] excelData = mockMvc.perform(get("/api/v1/supplier/1/truck_params/excel"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        var truckParamsReader = new SupplierTruckParamsExcelReader();
        var truckItemsReader = new SupplierTruckItemsParamsExcelReader();
        var sheets = StreamExcelParser.parse(new ByteArrayInputStream(excelData));

        assertEquals(sheets.size(), 2);
        var supplierTruckParams = truckParamsReader.read(sheets.get(0));
        var supplierTruckItemsParams = truckItemsReader.read(sheets.get(1));

        assertEquals(1, supplierTruckParams.size());
        var sTruckParams = supplierTruckParams.get(0);
        assertEquals(1, sTruckParams.getMinPallets());
        assertEquals(10, sTruckParams.getMaxPallets());
        assertEquals(100, sTruckParams.getPalletMaxHeight());
        assertEquals(100, sTruckParams.getPalletMaxWeight());
        assertEquals(10000, sTruckParams.getTruckMaxWeight());

        assertEquals(2, supplierTruckItemsParams.size());
        var firstItemsParams = supplierTruckItemsParams.get(0);
        assertEquals(987L, firstItemsParams.getMsku());
        assertNull(firstItemsParams.getWeight());
        assertEquals(20L, firstItemsParams.getHeight());

        var secondItemsParams = supplierTruckItemsParams.get(1);
        assertEquals(654L, secondItemsParams.getMsku());
        assertEquals(15L, secondItemsParams.getWeight());
        assertEquals(25L, secondItemsParams.getHeight());
    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_hasSupplierTruckParams.before.csv")
    public void testHasSupplierTruckParams() throws Exception {
        mockMvc.perform(get("/api/v1/supplier/1/truck_params/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DbUnitDataSet(before = "SupplierTruckParamsControllerTest_hasSupplierTruckParams.before.csv")
    public void testHasSupplierTruckParamsIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/supplier/3/truck_params/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
