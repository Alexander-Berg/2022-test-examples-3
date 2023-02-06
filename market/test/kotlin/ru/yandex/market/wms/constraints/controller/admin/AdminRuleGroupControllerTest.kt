package ru.yandex.market.wms.constraints.controller.admin

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.CollectionUtils
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class AdminRuleGroupControllerTest : ConstraintsIntegrationTest() {

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/success-sku-group/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create rule group with rules on SKU`() {
        testCreateRuleGroup(
            testCase = "create/success-sku-group",
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/success-loc-group/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create rule group with rules on LOC`() {
        testCreateRuleGroup(
            testCase = "create/success-loc-group",
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/success-empty-group/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create empty rule group`() {
        testCreateRuleGroup(
            testCase = "create/success-empty-group",
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/success-duplicated-rules/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create group with duplicated rules`() {
        testCreateRuleGroup(
            testCase = "create/success-duplicated-rules",
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/empty-group-table.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to create rule group with incompatible object types`() {
        testCreateRuleGroup(
            testCase = "create/incompatible-object-types",
            expectedStatus = MockMvcResultMatchers.status().isConflict,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/create/empty-group-table.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/create/empty-group-table.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to create rule group with not existing rule`() {
        testCreateRuleGroup(
            testCase = "create/not-existing-rule",
            expectedStatus = MockMvcResultMatchers.status().isNotFound,
            checkResponse = true
        )
    }

    private fun testCreateRuleGroup(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) {
        val filePath = "controller/admin/rule-group/$testCase"

        val request = post("/admin/rule-group")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("$filePath/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            checkResponse(resultActions, filePath)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["-1", "0"])
    fun `getRuleGroupsShort returns bad request status when limit is not positive`(limit: String) {
        val requestBuilder = get("/admin/rule-group/short")
            .param("limit", limit)
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/short/before.xml")
    fun `getRuleGroupsShort returns default number of rules when query is empty and limit is empty`() {
        val requestBuilder = get("/admin/rule-group/short")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/admin/rule-group/short/query-empty-limit-empty/response.json"
                    ), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/short/before.xml")
    fun `getRuleGroupsShort returns valid count of rules when query is empty and limit is not empty`() {
        val requestBuilder = get("/admin/rule-group/short")
            .param("limit", "5")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/admin/rule-group/short/query-empty-limit-not-empty/response.json"
                    ), true)
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["Rule-group-1", "ule-group-1", "ule-group-10"])
    @DatabaseSetup("/controller/admin/rule-group/short/before.xml")
    fun `getRuleGroupsShort returns valid rules when query is not empty and limit is empty`(query: String) {
        val requestBuilder = get("/admin/rule-group/short")
            .param("query", query)
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/admin/rule-group/short/query-not-empty-limit-empty/response_$query.json"
                    ), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/short/before.xml")
    fun `getRuleGroupsShort returns valid rules when query is not empty and limit is not empty`() {
        val requestBuilder = get("/admin/rule-group/short")
            .param("query", "ule-group-1")
            .param("limit", "6")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/admin/rule-group/short/query-not-empty-limit-not-empty/response.json"
                    ), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/short/before.xml")
    fun `getRuleGroupsShort returns empty result when valid rules are not present in database`() {
        val requestBuilder = get("/admin/rule-group/short")
            .param("query", "Some rule")
            .contentType(MediaType.APPLICATION_JSON)

        mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/admin/rule-group/short/rule-groups-not-found/response.json"
                    ), true)
            )
    }

    /*******************GET /admin/rule-group/ ****************************/
    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group without params`() {
        testGetAllRuleGroup(
            testCase = "get-all/without-params",
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group with filter by exact type - SKU`() {
        testGetAllRuleGroup(
            testCase = "get-all/only-sku-groups",
            params = mapOf(
                "filter" to listOf("objectType=='SKU'")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group with filter by part of title`() {
        testGetAllRuleGroup(
            testCase = "get-all/filter-by-part-of-title",
            params = mapOf(
                "filter" to listOf("title=='%2'")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group with limit`() {
        testGetAllRuleGroup(
            testCase = "get-all/with-limit",
            params = mapOf(
                "limit" to listOf("2")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group with limit and offset`() {
        testGetAllRuleGroup(
            testCase = "get-all/with-limit-and-offset",
            params = mapOf(
                "limit" to listOf("2"),
                "offset" to listOf("2")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group with sort and order`() {
        testGetAllRuleGroup(
            testCase = "get-all/with-sort-order",
            params = mapOf(
                "sort" to listOf("title"),
                "order" to listOf("DESC")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/get-all/rule-groups.xml")
    fun `get all rule group - empty result`() {
        testGetAllRuleGroup(
            testCase = "get-all/empty-result",
            params = mapOf(
                "filter" to listOf("objectType=='NOT_EXISTING_TYPE'")
            ),
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = true
        )
    }

    private fun testGetAllRuleGroup(
        testCase: String,
        params: Map<String, List<String>> = emptyMap(),
        expectedStatus: ResultMatcher,
        checkResponse: Boolean = true
    ) {
        val filePath = "controller/admin/rule-group/$testCase"

        val request = MockMvcRequestBuilders.get("/admin/rule-group")

        if (params.isNotEmpty()) {
            request.queryParams(CollectionUtils.toMultiValueMap(params))
        }

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            checkResponse(resultActions, filePath)
        }
    }

    /*******************PUT /admin/rule-group/{ruleGroupId} ****************************/

    @Test
    @DatabaseSetup("/controller/admin/rule-group/update/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/update/success-update-same-type/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `update rule group with new rules and same type`() {
        testUpdateRuleGroup(
            testCase = "update/success-update-same-type",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/update/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/update/success-update-different-type/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `update rule group with new rules and different type`() {
        testUpdateRuleGroup(
            testCase = "update/success-update-different-type",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/update/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/update/success-update-empty/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `update rule group with empty rules list`() {
        testUpdateRuleGroup(
            testCase = "update/success-update-empty",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/update/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/update/rule-groups.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to update rule group with incompatible object types`() {
        testUpdateRuleGroup(
            testCase = "update/incompatible-object-types",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isConflict,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/update/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/update/rule-groups.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to update rule group with not existing rule`() {
        testUpdateRuleGroup(
            testCase = "update/not-existing-rule",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isNotFound,
            checkResponse = true
        )
    }

    private fun testUpdateRuleGroup(
        testCase: String,
        ruleGroupId: Int,
        expectedStatus: ResultMatcher,
        checkResponse: Boolean = true
    ) {
        val filePath = "controller/admin/rule-group/$testCase"

        val request = MockMvcRequestBuilders.put("/admin/rule-group/$ruleGroupId")
            .contentType(MediaType.APPLICATION_JSON)
            .content(FileContentUtils.getFileContent("$filePath/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            checkResponse(resultActions, filePath)
        }
    }

    /*******************DELETE /admin/rule-group/{ruleGroupId} ****************************/

    @Test
    @DatabaseSetup("/controller/admin/rule-group/delete/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/delete/rule-groups.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `delete rule group - cannot delete`() {
        testDeleteRuleGroup(
            testCase = "delete/cannot-delete",
            ruleGroupId = 1,
            expectedStatus = MockMvcResultMatchers.status().isBadRequest,
            checkResponse = true
        )
    }

    @Test
    @DatabaseSetup("/controller/admin/rule-group/delete/rule-groups.xml")
    @ExpectedDatabase(
        value = "/controller/admin/rule-group/delete/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `delete rule group - ok`() {
        testDeleteRuleGroup(
            testCase = "delete/ok",
            ruleGroupId = 2,
            expectedStatus = MockMvcResultMatchers.status().isOk,
            checkResponse = false
        )
    }

    private fun testDeleteRuleGroup(
        testCase: String,
        ruleGroupId: Int,
        expectedStatus: ResultMatcher,
        checkResponse: Boolean = true
    ) {
        val filePath = "controller/admin/rule-group/$testCase"

        val request = MockMvcRequestBuilders.delete("/admin/rule-group/$ruleGroupId")
            .contentType(MediaType.APPLICATION_JSON)

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            checkResponse(resultActions, filePath)
        }
    }

    private fun checkResponse(resultActions: ResultActions, filePath: String) {
        resultActions.andExpect(
            MockMvcResultMatchers.content().json(
                FileContentUtils.getFileContent("$filePath/response.json"),
                true
            )
        )
    }
}
