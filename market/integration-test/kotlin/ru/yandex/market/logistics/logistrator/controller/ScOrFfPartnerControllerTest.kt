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

internal class ScOrFfPartnerControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/db/sc_or_ff_creation/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateRequestDraft() {
        mockMvc.perform(
            post("/partners/sorting-centers")
                .content(extractFileContent("request/sc_or_ff_creation/create_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/sc_or_ff_creation/create_request_draft.json")))
    }

    @Test
    @DatabaseSetup("/db/sc_or_ff_creation/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/sc_or_ff_creation/after/draft_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateRequestDraft() {
        mockMvc.perform(
            put("/partners/sorting-centers/101")
                .content(extractFileContent("request/sc_or_ff_creation/update_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/sc_or_ff_creation/update_request_draft.json")))
    }
}
