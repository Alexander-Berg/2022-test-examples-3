package ru.yandex.market.pricingmgmt.api.security

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.pricingmgmt.api.ControllerTest

open class SecurityApiServiceTest : ControllerTest() {

    @Test
    fun getAuthContextTest() {
        mockMvc.perform(get("/api/v1/security/get-context"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.login").value("localDeveloper"))
            .andExpect(jsonPath("$.roles[0]").value("ROLE_PRICING_MGMT_ACCESS"))
    }
}
