package ru.yandex.market.replenishment.autoorder.api

import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbo.excel.StreamExcelParser
import ru.yandex.market.replenishment.autoorder.config.ControllerTest
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import java.io.ByteArrayInputStream
import java.util.List

@DbUnitDataBaseConfig(DbUnitDataBaseConfig.Entry(name = "tableType", value = "TABLE"))
@WithMockLogin
class TenderTruckControllerTest : ControllerTest() {

    private val excelTestingHelper = ExcelTestingHelper(this)

    @Test
    @DbUnitDataSet(before = ["TenderTruckControllerTest.before.csv"])
    fun testGetTrucksByDemandIdAndSupplierId_isOk() {
        mockMvc.perform(get("/api/v1/tender/10/100/trucks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.truckId==91)].cargo.size()").value(3))
            .andExpect(
                jsonPath("$[?(@.truckId==91)].cargo[?(@.ssku==\"111\")].items").value(5)
            )
            .andExpect(
                jsonPath("$[?(@.truckId==91)].cargo[?(@.ssku==\"222\")].items").value(7)
            )
            .andExpect(
                jsonPath("$[?(@.truckId==91)].cargo[?(@.ssku==\"333\")].items").value(8)
            )
            .andExpect(jsonPath("$[?(@.truckId==91)].warehouseId").value(171))
            .andExpect(jsonPath("$[?(@.truckId==91)].warehouseName").value("Томилино"))
            .andExpect(jsonPath("$[?(@.truckId==91)].orderId").value("91-4242"))
            .andExpect(jsonPath("$[?(@.truckId==91)].onPallet").value(false))
            .andExpect(jsonPath("$[?(@.truckId==91)].comment").value("опа опа"))

            .andExpect(jsonPath("$[?(@.truckId==92)].cargo.size()").value(3))
            .andExpect(
                jsonPath("$[?(@.truckId==92)].cargo[?(@.ssku==\"111\")].items").value(10)
            )
            .andExpect(
                jsonPath("$[?(@.truckId==92)].cargo[?(@.ssku==\"222\")].items").value(14)
            )
            .andExpect(
                jsonPath("$[?(@.truckId==92)].cargo[?(@.ssku==\"333\")].items").value(16)
            )
            .andExpect(jsonPath("$[?(@.truckId==92)].warehouseId").value(304))
            .andExpect(jsonPath("$[?(@.truckId==92)].warehouseName").value("Яндекс.Маркет (Софьино КГТ)"))
            .andExpect(jsonPath("$[?(@.truckId==92)].orderId").value("92-4242"))
            .andExpect(jsonPath("$[?(@.truckId==92)].comment").value("америка европа"))
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.before.csv"],
        after = ["TenderTruckControllerTest.add.after.csv"]
    )
    fun testAddTrucksByDemandIdAndSupplierId_isOk() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.addTruck.xlsx"
        ).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.before.csv"],
        after = ["TenderTruckControllerTest.add-main.after.csv"]
    )
    fun testAddTrucksByDemandIdAndSupplierIdExactMainTruck() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.addExactMainTruck.xlsx"
        ).andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(before = ["TenderTruckControllerTest.before.csv"])
    fun testAddTrucksByDemandIdAndSupplierId_badSsku_isBadRequest() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.badSskuFormat.xlsx"
        ).andExpect(status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Sskus not found in tender results for tender id 20 and supplier id 300: 0300.424242"
                    )
            )
    }

    @Test
    @DbUnitDataSet(before = ["TenderTruckControllerTest.before.csv"])
    fun testAddTrucksByDemandIdAndSupplierId_itemsAbsence_isBadRequest() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.itemsAbsence.xlsx"
        ).andExpect(status().isIAmATeapot)
            .andExpect(jsonPath("$.message").value("Excel file doesn't contain any ssku."))
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.before.csv"]
    )
    fun testAddTrucksByDemandIdAndSupplierIdExcessive_isBadRequest() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.addExcessive.xlsx"
        ).andExpect(status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message")
                    .value("Превышено кол-во по тендеру для сскю: 0300.111 на 96.")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest_differentWarehouses.before.csv"]
    )
    fun testAddTrucksByDemandIdAndSupplierIdExcessive_differentWarehouses() {
        excelTestingHelper.upload(
            "POST",
            "/api/v1/tender/20/300/truck",
            "TenderTruckControllerTest.differentWarehouses.xlsx"
        ).andExpect(status().isIAmATeapot)
            .andExpect(
                jsonPath("$.message")
                    .value("Все ssku должны иметь одинаковый склад.")
            )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.before.csv"],
        after = ["TenderTruckControllerTest.delete.after.csv"]
    )
    fun testDeleteTruckByDemandIdAndSupplierId_isOk() {
        mockMvc.perform(delete("/api/v1/tender/truck/92"))
            .andExpect(status().isOk)
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.before.csv"]
    )
    fun testGetTenderTruckTemplate_empty() {
        val excelData = mockMvc.perform(get("/api/v1/tender/20/200/truck-template"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray
        val sheets = StreamExcelParser.parse(ByteArrayInputStream(excelData))


        val sheet = sheets.get(0)

        Assertions.assertEquals(4, sheet.allRows.size)

        Assertions.assertEquals("ID Заказа", sheet.rows[0]?.get(0))
        Assertions.assertEquals("Привезем", sheet.rows[1]?.get(0))
        Assertions.assertEquals("На паллетах", sheet.rows[1]?.get(1))
        Assertions.assertEquals("Комментарий", sheet.rows[2]?.get(0))
        Assertions.assertEquals("SSKU", sheet.rows[4]?.get(0))
        Assertions.assertEquals("Количество", sheet.rows[4]?.get(1))
        Assertions.assertEquals("Склад", sheet.rows[4]?.get(2))
        Assertions.assertEquals("Цена,руб", sheet.rows[4]?.get(3))
        Assertions.assertEquals("Сумма,руб", sheet.rows[4]?.get(4))
        Assertions.assertNull(sheet.rows[5])
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderTruckControllerTest.notEmptyTemplate.csv"]
    )
    fun testGetTenderTruckTemplate_notEmpty() {
        val excelData = mockMvc.perform(get("/api/v1/tender/20/200/truck-template"))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsByteArray
        val sheets = StreamExcelParser.parse(ByteArrayInputStream(excelData))
        val sheet = sheets.get(0)

        Assertions.assertEquals(6, sheet.allRows.size)

        Assertions.assertEquals("На паллетах", sheet.rows[1]?.get(1))

        val updatedRows = List.of<Map<Int, String>>(sheet.rows[5], sheet.rows[6])

        org.assertj.core.api.Assertions
            .assertThat(updatedRows)
            .containsExactlyInAnyOrder(
                java.util.Map.of(0, "222", 1, "3", 2, "Софьино", 3, "0.3", 4, "0"),
                java.util.Map.of(0, "111", 1, "4", 2, "Ростов", 3, "0.4", 4, "0"),
            )

        Assertions.assertNull(sheet.rows[8])
    }
}
