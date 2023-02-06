package ru.yandex.market.logistics.logistrator.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal class McDeliveryPartnerControllerTest : AbstractContextualTest() {

    @BeforeEach
    fun setUp() {
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        whenever(clock.instant()).thenReturn(LocalDateTime.of(2022, 1, 7, 12, 0, 0).toInstant(ZoneOffset.UTC))
    }

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/empty_setup.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/draft_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCreateRequestDraft() {
        mockMvc.perform(
            post("/partners/mc-delivery")
                .content(extractFileContent("request/mc_delivery_creation/create_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/mc_delivery_creation/create_request_draft.json")))
    }

    @Test
    @DatabaseSetup("/db/mc_delivery_creation/before/draft_created.xml")
    @ExpectedDatabase(
        "/db/mc_delivery_creation/after/draft_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateRequestDraft() {
        mockMvc.perform(
            put("/partners/mc-delivery/101")
                .content(extractFileContent("request/mc_delivery_creation/update_request_draft.json"))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/mc_delivery_creation/update_request_draft.json")))
    }
}
