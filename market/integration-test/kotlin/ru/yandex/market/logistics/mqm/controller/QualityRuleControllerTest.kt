package ru.yandex.market.logistics.mqm.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class QualityRuleControllerTest : AbstractContextualTest() {

    @Test
    @DatabaseSetup("/controller/qualityrule/before/get_quality_rule.xml")
    fun findQualityRule() {
        mockMvc.perform(MockMvcRequestBuilders.get("/quality-rule/2"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/qualityrule/response/find_quality_rule.json"))
    }

    @Test
    fun findQualityRuleNotFoundError() {
        mockMvc.perform(MockMvcRequestBuilders.get("/quality-rule/2"))
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @ExpectedDatabase(
        value = "/controller/qualityrule/after/create_quality_rule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createQualityRule() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/quality-rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/qualityrule/request/create_quality_rule.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/qualityrule/response/create_quality_rule.json"))
    }

    @Test
    fun createQualityRuleWithNonSerializablePayload() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/quality-rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/create_quality_rule_with_non_serializable_payload.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/qualityrule/before/update_quality_rule.xml")
    @ExpectedDatabase(
        value = "/controller/qualityrule/after/update_quality_rule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateQualityRule() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/quality-rule/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/update_quality_rule.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/qualityrule/response/update_quality_rule.json"))
    }

    @Test
    @DatabaseSetup("/controller/qualityrule/before/update_quality_rule_with_payload.xml")
    @ExpectedDatabase(
        value = "/controller/qualityrule/after/update_quality_rule_with_payload.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateQualityRuleWithPayload() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/quality-rule/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/update_quality_rule_with_payload.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent(
                "controller/qualityrule/response/update_quality_rule_with_payload.json"
            ))
    }

    @Test
    fun updateNonExistingQualityRule() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/quality-rule/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/update_quality_rule.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/qualityrule/before/search_quality_rule.xml")
    fun searchQualityRules() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/quality-rule/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/search_quality_rule.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(IntegrationTestUtils.jsonContent("controller/qualityrule/response/search_quality_rule.json"))
    }

    @Test
    fun searchQualityRulesFindZero() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("/quality-rule/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(
                    "controller/qualityrule/request/search_quality_rule.json"
                ))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(content().json("[]"))
    }
}
