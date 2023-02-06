package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

internal class ScToMcPartnerRelationControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateRequestDraft() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/partner-relations/sc-to-mc")
                .content(IntegrationTestUtils.extractFileContent(
                    "request/sc_to_mc_partner_relation_creation/create_request_draft.json"
                ))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(IntegrationTestUtils.status().isOk)
            .andExpect(json().isEqualTo(IntegrationTestUtils.extractFileContent(
                "response/sc_to_mc_partner_relation_creation/create_request_draft.json"
            )))
    }

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_creation/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_creation/after/draft_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateRequestDraft() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/partner-relations/sc-to-mc/101")
                .content(IntegrationTestUtils.extractFileContent("request/sc_to_mc_partner_relation_creation/update_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(IntegrationTestUtils.status().isOk)
            .andExpect(json().isEqualTo(IntegrationTestUtils.extractFileContent("response/sc_to_mc_partner_relation_creation/update_request_draft.json")))
    }

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_updating/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_updating/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateUpdatingRequestDraft() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/partner-relations/sc-to-mc/update")
                .content(IntegrationTestUtils.extractFileContent("request/sc_to_mc_partner_relation_updating/create_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(IntegrationTestUtils.status().isOk)
            .andExpect(json().isEqualTo(
                IntegrationTestUtils.extractFileContent(
                    "response/sc_to_mc_partner_relation_updating/create_request_draft.json"
                )
            ))
    }

    @Test
    @DatabaseSetup("/db/sc_to_mc_partner_relation_updating/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_to_mc_partner_relation_updating/after/draft_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateUpdatingRequestDraft() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/partner-relations/sc-to-mc/update/51")
                .content(IntegrationTestUtils.extractFileContent("request/sc_to_mc_partner_relation_updating/update_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(IntegrationTestUtils.status().isOk)
            .andExpect(json().isEqualTo(
                IntegrationTestUtils.extractFileContent(
                    "response/sc_to_mc_partner_relation_updating/update_request_draft.json"
                )
            ))
    }
}
