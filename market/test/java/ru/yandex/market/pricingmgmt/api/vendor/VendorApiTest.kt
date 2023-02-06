package ru.yandex.market.pricingmgmt.api.vendor

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
class VendorApiTest : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["VendorApiTest.searchVendorsTest.before.csv"]
    )
    fun testSearchVendors() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/vendors/search")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].id").value(5L))
            .andExpect(jsonPath("$[0].name").value("ABCAAAAA"))
            .andExpect(jsonPath("$[1].id").value(1L))
            .andExpect(jsonPath("$[1].name").value("ABCC"))
            .andExpect(jsonPath("$[2].id").value(2L))
            .andExpect(jsonPath("$[2].name").value("ABCD"))
            .andExpect(jsonPath("$[3].id").value(4L))
            .andExpect(jsonPath("$[3].name").value("ADVB"))
            .andExpect(jsonPath("$[4].id").value(3L))
            .andExpect(jsonPath("$[4].name").value("ADVF"))
    }

    @Test
    @DbUnitDataSet(
        before = ["VendorApiTest.searchVendorsTest.before.csv"]
    )
    fun testSearchVendorsWithName() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/vendors/search?name=aBcc")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("ABCC"))
    }

    @Test
    @DbUnitDataSet(
        before = ["VendorApiTest.searchVendorsTest.before.csv"]
    )
    fun testGetExistingVendors() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/vendors?ids=5,11,33,44,55,66,1,5")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("ABCC"))
            .andExpect(jsonPath("$[1].id").value(5L))
            .andExpect(jsonPath("$[1].name").value("ABCAAAAA"))
    }
}
