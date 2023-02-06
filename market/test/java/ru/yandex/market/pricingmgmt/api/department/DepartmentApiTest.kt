package ru.yandex.market.pricingmgmt.api.department

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest

class DepartmentApiTest : ControllerTest() {

    @DbUnitDataSet(before = ["DepartmentApiTest.getDepartments.before.csv"])
    @Test
    fun testGetDepartments() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/departments")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("команда1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].idmTranslation").value("KOMANDA1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].priceThreshold").value("MARGINALITY"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].priceThresholdValue").value(20.0))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("команда2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].idmTranslation").value("KOMANDA2"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].priceThreshold").value("EXTRA_CHARGE"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].priceThresholdValue").value(30.0))
    }

    @DbUnitDataSet(before = ["DepartmentApiTest.getDepartments.before.csv"])
    @Test
    fun testGetDepartment() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/departments/1/")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("команда1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.idmTranslation").value("KOMANDA1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.priceThreshold").value("MARGINALITY"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.priceThresholdValue").value(20.0))
    }
}
