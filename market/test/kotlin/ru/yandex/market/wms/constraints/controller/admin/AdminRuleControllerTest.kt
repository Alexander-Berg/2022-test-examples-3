package ru.yandex.market.wms.constraints.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class AdminRuleControllerTest : ConstraintsIntegrationTest() {
    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/without-restrictions/before.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/without-restrictions/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule without restrictions`() {
        testCreateRule(testCase = "without-restrictions", expectedStatus = status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/with-valid-restrictions/before.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/with-valid-restrictions/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with valid restrictions`() {
        testCreateRule(testCase = "with-valid-restrictions", expectedStatus = status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with incompatible cargo type collation`() {
        testCreateRule(testCase = "invalid-cargotype-collation", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with multiple collations with same type, param and value`() {
        testCreateRule(testCase = "multiple-collations", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with more than 2 restrictions with same type and param`() {
        testCreateRule(testCase = "excess-dimension-restrictions", expectedStatus = status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = ["incompatible-dimensions-1", "incompatible-dimensions-2"])
    @DatabaseSetup("/controller/admin/rule/create-rule/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with incompatible dimension restrictions`(testCase: String) {
        testCreateRule(testCase = testCase, expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/create-rule/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/create-rule/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRule with dimension restriction providing values instead of value`() {
        testCreateRule(
            testCase = "dimension-json-values",
            expectedStatus = status().isBadRequest,
            checkResponse = false
        )
    }


    @ParameterizedTest
    @ValueSource(strings = ["-1", "0"])
    fun `getRulesShort returns bad request status when limit is not positive`(limit: String) {
        val requestBuilder = get("/admin/rule/short")
            .param("limit", limit)
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rules-short/before.xml")
    fun `getRulesShort returns default number of rules when query is empty and limit is empty`() {
        val requestBuilder = get("/admin/rule/short")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/rules-short/query-empty-limit-empty/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rules-short/before.xml")
    fun `getRulesShort returns valid count of rules when query is empty and limit is not empty`() {
        val requestBuilder = get("/admin/rule/short")
            .param("limit", "5")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/rules-short/query-empty-limit-not-empty/response.json"),
                    true
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["Rule-1", "ule-1", "ule-10"])
    @DatabaseSetup("/controller/admin/rules-short/before.xml")
    fun `getRulesShort returns valid rules when query is not empty and limit is empty`(query: String) {
        val requestBuilder = get("/admin/rule/short")
            .param("query", query)
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/rules-short/query-not-empty-limit-empty/response_$query.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rules-short/before.xml")
    fun `getRulesShort returns valid rules when query is not empty and limit is not empty`() {
        val requestBuilder = get("/admin/rule/short")
            .param("query", "ule-1")
            .param("limit", "6")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/admin/rules-short/query-not-empty-limit-not-empty/response.json"),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rules-short/before.xml")
    fun `getRulesShort returns empty result when valid rules are not present in database`() {
        val requestBuilder = get("/admin/rule/short")
            .param("query", "Some rule")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk)
            .andExpect(
                content()
                    .json(getFileContent("controller/admin/rules-short/rules-not-found/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-rule-info/data.xml")
    fun `getRuleInfo without restrictions`() {
        testGetRuleInfo("without-restrictions", ruleId = 1, expectedStatus = status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-rule-info/data.xml")
    fun `getRuleInfo with restrictions`() {
        testGetRuleInfo("with-restrictions", ruleId = 2, expectedStatus = status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-rule-info/data.xml")
    fun `getRuleInfo not found`() {
        testGetRuleInfo("not-found", ruleId = 100, expectedStatus = status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules without parameters`() {
        testGetAllRules(testCase = "without-params")
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules filter by loc type`() {
        testGetAllRules(testCase = "loc-object-type", mapOf("filter" to "objectType=='LOC'"))
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules filter by id`() {
        testGetAllRules(testCase = "filter-id", mapOf("filter" to "id<=2"))
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules filter by title and id`() {
        testGetAllRules(testCase = "filter-title-id", mapOf("filter" to "title=='Rule%';id==4"))
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules with offset and limit`() {
        testGetAllRules(testCase = "offset-and-limit", mapOf("offset" to "2", "limit" to "2"))
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/get-all/data.xml")
    fun `getAllRules with descending title sort`() {
        testGetAllRules(testCase = "title-sort", mapOf("limit" to "2", "sort" to "title", "order" to "DESC"))
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/delete/ok/before.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/delete/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `deleteRule with restrictions successfully`() {
        mockMvc.perform(delete("/admin/rule/1"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/delete/existing-groups/immutable.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/delete/existing-groups/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `deleteRule failed due to linked rule groups`() {
        mockMvc.perform(delete("/admin/rule/1"))
            .andExpect(status().isBadRequest)
            .andExpect(
                content()
                    .json(getFileContent("controller/admin/rule/delete/existing-groups/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/update-rule/data.xml")
    @ExpectedDatabase(
        "/controller/admin/rule/update-rule/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `updateRule with valid restrictions`() {
        testUpdateRule("ok", ruleId = 2, expectedStatus = status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/update-rule/data.xml")
    fun `updateRule with invalid restrictions`() {
        testUpdateRule("invalid-restrictions", ruleId = 2, expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule/update-rule/data.xml")
    fun `updateRule with non-existent rule id`() {
        testUpdateRule("rule-not-found", ruleId = 100, expectedStatus = status().isNotFound)
    }

    private fun testCreateRule(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) {
        val request = post("/admin/rule")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/admin/rule/create-rule/$testCase/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            resultActions.andExpect(
                content().json(getFileContent("controller/admin/rule/create-rule/$testCase/response.json"), true)
            )
        }
    }

    private fun testGetRuleInfo(testCase: String, ruleId: Int, expectedStatus: ResultMatcher) {
        val request = get("/admin/rule/$ruleId")

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(
                content().json(getFileContent("controller/admin/rule/get-rule-info/$testCase/response.json"), true)
            )
    }

    private fun testGetAllRules(testCase: String, params: Map<String, String> = emptyMap()) {
        val request = get("/admin/rule")
        params.forEach { request.param(it.key, it.value) }

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/admin/rule/get-all/$testCase/response.json"), true)
            )
    }

    private fun testUpdateRule(
        testCase: String,
        ruleId: Int,
        expectedStatus: ResultMatcher,
        checkResponse: Boolean = true
    ) {
        val request = put("/admin/rule/$ruleId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/admin/rule/update-rule/$testCase/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            resultActions.andExpect(
                content().json(getFileContent("controller/admin/rule/update-rule/$testCase/response.json"), true)
            )
        }
    }
}
