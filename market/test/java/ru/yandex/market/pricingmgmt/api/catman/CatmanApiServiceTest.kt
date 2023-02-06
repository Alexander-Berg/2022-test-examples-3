package ru.yandex.market.pricingmgmt.api.catman

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest

class CatmanApiServiceTest : ControllerTest() {

    @DbUnitDataSet(before = ["CatmanApiServiceTest.getCatmans.before.csv"])
    @Test
    fun testGetCatmans() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/catmans")
                .contentType("application/json")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].login").value("ivan"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$[1].login").value("andrey"))
    }
}
