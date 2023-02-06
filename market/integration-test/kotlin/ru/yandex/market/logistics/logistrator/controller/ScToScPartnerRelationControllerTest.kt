package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status

internal class ScToScPartnerRelationControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateCreationRequestDraft() {
        mockMvc.perform(
            post("/partner-relations/sc-to-sc")
                .content(extractFileContent("request/sc_to_sc_partner_relation_creation/create_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_to_sc_partner_relation_creation/create_request_draft.json"
            )))
    }

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/draft_updated_setup_movement_is_second_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateCreationRequestDraftMovementIsSecondPartner() {
        mockMvc.perform(
            put("/partner-relations/sc-to-sc/101")
                .content(extractFileContent(
                    "request/sc_to_sc_partner_relation_creation/update_request_draft_movement_is_second_partner.json"
                ))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_to_sc_partner_relation_creation/update_request_draft_movement_is_second_partner.json"
            )))
    }

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_creation/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_creation/after/draft_updated_setup_movement_is_third_partner.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateCreationRequestDraftMovementIsThirdPartner() {
        mockMvc.perform(
            put("/partner-relations/sc-to-sc/101")
                .content(extractFileContent(
                    "request/sc_to_sc_partner_relation_creation/update_request_draft_movement_is_third_partner.json"
                ))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_to_sc_partner_relation_creation/update_request_draft_movement_is_third_partner.json"
            )))
    }

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_updating/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_updating/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateUpdatingRequestDraft() {
        mockMvc.perform(
            post("/partner-relations/sc-to-sc/update")
                .content(extractFileContent("request/sc_to_sc_partner_relation_updating/create_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_to_sc_partner_relation_updating/create_request_draft.json"
            )))
    }

    @Test
    @DatabaseSetup("/db/sc_to_sc_partner_relation_updating/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_to_sc_partner_relation_updating/after/draft_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateUpdatingRequestDraft() {
        mockMvc.perform(
            put("/partner-relations/sc-to-sc/update/101")
                .content(extractFileContent("request/sc_to_sc_partner_relation_updating/update_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_to_sc_partner_relation_updating/update_request_draft.json"
            )))
    }
}
