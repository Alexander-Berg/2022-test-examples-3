package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminPartnerRelationParamsControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/partner_relation_params_search/before/partner_relation_params_search.xml")
    fun partnerRelationParamsSearchByIds() {
        val requestBuilder = get("/admin/partner-relation-params/search")
            .param("partnerRelationParamsId", "1")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/partner_relation_params_search/response/partner_relation_params_search_ids.json",
                    false
                )
            )
    }
}
