package ru.yandex.market.logistics.mqm.admin.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class AdminIssueLinksControllerTest: AbstractContextualTest() {

    @Test
    @DatabaseSetup("/admin/controller/issue_links_search/before/issue_links_search.xml")
    fun issueLinksSearchByIds() {
        val requestBuilder = MockMvcRequestBuilders.get("/admin/issue-links/search")
            .param("issueLinkId", "101")
        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                IntegrationTestUtils.jsonContent(
                    "admin/controller/issue_links_search/response/issue_links_search_ids.json",
                    false
                )
            )
    }
}
