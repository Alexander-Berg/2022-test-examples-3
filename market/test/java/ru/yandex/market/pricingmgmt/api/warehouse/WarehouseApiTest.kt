package ru.yandex.market.pricingmgmt.api.warehouse

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest

class WarehouseApiTest : ControllerTest() {

    @DbUnitDataSet(before = ["WarehouseApiTest.getWarehouses.before.csv"])
    @Test
    fun testGetWarehouses() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/warehouses")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(147))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Ростов-на-Дону"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(172))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Софьино"))
    }

}
