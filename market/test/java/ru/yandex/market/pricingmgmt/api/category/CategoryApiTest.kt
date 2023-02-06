package ru.yandex.market.pricingmgmt.api.category

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
class CategoryApiTest : ControllerTest() {

    @Test
    @DbUnitDataSet(
        before = ["CategoryApiTest.searchCategoriesTest.before.csv"]
    )
    fun testSearchCategories() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/categories/search")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(5))
            .andExpect(jsonPath("$[0].id").value(5L))
            .andExpect(jsonPath("$[0].name").value("ABCAAAAA"))
            .andExpect(jsonPath("$[0].parent.id").value(4L))
            .andExpect(jsonPath("$[0].parent.name").value("ADVB"))
            .andExpect(jsonPath("$[0].grandparent.id").value(3))
            .andExpect(jsonPath("$[0].grandparent.name").value("ADVF"))
            .andExpect(jsonPath("$[1].id").value(1L))
            .andExpect(jsonPath("$[1].name").value("ABCC"))
            .andExpect(jsonPath("$[1].parent.id").value(1L))
            .andExpect(jsonPath("$[1].parent.name").value("ABCC"))
            .andExpect(jsonPath("$[1].grandparent.id").value(1L))
            .andExpect(jsonPath("$[1].grandparent.name").value("ABCC"))
            .andExpect(jsonPath("$[2].id").value(2L))
            .andExpect(jsonPath("$[2].name").value("ABCD"))
            .andExpect(jsonPath("$[1].parent.id").value(1L))
            .andExpect(jsonPath("$[1].parent.name").value("ABCC"))
            .andExpect(jsonPath("$[1].grandparent.id").value(1L))
            .andExpect(jsonPath("$[1].grandparent.name").value("ABCC"))
            .andExpect(jsonPath("$[3].id").value(4L))
            .andExpect(jsonPath("$[3].name").value("ADVB"))
            .andExpect(jsonPath("$[3].parent.id").value(3))
            .andExpect(jsonPath("$[3].parent.name").value("ADVF"))
            .andExpect(jsonPath("$[3].grandparent.id").value(2))
            .andExpect(jsonPath("$[3].grandparent.name").value("ABCD"))
            .andExpect(jsonPath("$[4].id").value(3L))
            .andExpect(jsonPath("$[4].name").value("ADVF"))
            .andExpect(jsonPath("$[4].parent.id").value(2))
            .andExpect(jsonPath("$[4].parent.name").value("ABCD"))
            .andExpect(jsonPath("$[4].grandparent.id").value(1))
            .andExpect(jsonPath("$[4].grandparent.name").value("ABCC"))
    }

    @Test
    @DbUnitDataSet(
        before = ["CategoryApiTest.searchCategoriesTest.before.csv"]
    )
    fun testSearchCategoriesWithName() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/categories/search?name=Ad")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(4L))
            .andExpect(jsonPath("$[0].name").value("ADVB"))
            .andExpect(jsonPath("$[0].parent.id").value(3))
            .andExpect(jsonPath("$[0].parent.name").value("ADVF"))
            .andExpect(jsonPath("$[0].grandparent.id").value(2))
            .andExpect(jsonPath("$[0].grandparent.name").value("ABCD"))
            .andExpect(jsonPath("$[1].id").value(3L))
            .andExpect(jsonPath("$[1].name").value("ADVF"))
            .andExpect(jsonPath("$[1].parent.id").value(2))
            .andExpect(jsonPath("$[1].parent.name").value("ABCD"))
            .andExpect(jsonPath("$[1].grandparent.id").value(1))
            .andExpect(jsonPath("$[1].grandparent.name").value("ABCC"))
    }

    @Test
    @DbUnitDataSet(
        before = ["CategoryApiTest.searchCategoriesTest.before.csv"]
    )
    fun testGetExistingCategories() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/categories?ids=1,3,6,4,8")
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("ABCC"))
            .andExpect(jsonPath("$[1].id").value(3L))
            .andExpect(jsonPath("$[1].name").value("ADVF"))
            .andExpect(jsonPath("$[2].id").value(4L))
            .andExpect(jsonPath("$[2].name").value("ADVB"))
    }
}
