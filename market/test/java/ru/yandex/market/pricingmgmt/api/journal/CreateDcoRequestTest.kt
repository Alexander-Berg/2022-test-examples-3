package ru.yandex.market.pricingmgmt.api.journal

import org.junit.jupiter.api.Test
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationFilter

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
@WithMockUser(
    username = PassportAuthenticationFilter.LOCAL_DEV,
    roles = ["PRICING_MGMT_ACCESS", "VIEW_PRICES_JOURNALS_KOMANDA1"]
)
class CreateDcoRequestTest: ControllerTest() {

    @DbUnitDataSet(
        before = ["JournalApiTest.createDcoRequest.before.csv"],
        after = ["JournalApiTest.createDcoRequest.after.csv"],
    )
    @Test
    fun testCreateRequest() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/dco-request")
                .contentType("application/json")
                .content("{ \"ids\": [1, 5] }")
        ).andExpect(status().isOk)
    }

    @DbUnitDataSet(before = ["JournalApiTest.createDcoRequest.no_edit.before.csv"])
    @Test
    fun testCreateRequestNoEdit() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/journals/dco-request")
                .contentType("application/json")
                .content("{ \"ids\": [1, 5] }")
        ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Не хватает роли для редактирования"))
    }
}
