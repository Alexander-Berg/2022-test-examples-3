package ru.yandex.market.pricingmgmt.api.ssku

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
class SskuApiTest : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["SskuApiTest.searchSskusTest.before.csv"]
    )
    fun testSearchSskus() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/sskus/search")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
            .andExpect(jsonPath("$[0]").value("6435.332"))
            .andExpect(jsonPath("$[1]").value("6437.339"))
            .andExpect(jsonPath("$[2]").value("6438.331"))
            .andExpect(jsonPath("$[3]").value("6438.334"))
            .andExpect(jsonPath("$[4]").value("6438.335"))
            .andExpect(jsonPath("$[5]").value("6438.336"))
    }

    @Test
    @DbUnitDataSet(
        before = ["SskuApiTest.searchSskusTest.before.csv"]
    )
    fun testSearchSskusWithSsku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/sskus/search?ssku=6438.33")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0]").value("6438.331"))
            .andExpect(jsonPath("$[1]").value("6438.334"))
            .andExpect(jsonPath("$[2]").value("6438.335"))
            .andExpect(jsonPath("$[3]").value("6438.336"))
    }
}
