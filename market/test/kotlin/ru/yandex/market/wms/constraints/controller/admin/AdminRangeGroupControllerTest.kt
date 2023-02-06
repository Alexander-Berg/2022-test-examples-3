package ru.yandex.market.wms.constraints.controller.admin

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.reactive.function.client.WebClientResponseException
import ru.yandex.market.wms.common.spring.enums.WmsErrorCode
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest
import ru.yandex.market.wms.constraints.core.response.CreateRangeGroupResponse
import ru.yandex.market.wms.constraints.core.response.GetPagedRangeGroupResponse
import ru.yandex.market.wms.constraints.integration.dto.WmsServiceErrorResponse
import ru.yandex.market.wms.core.base.response.ResultResponse

class AdminRangeGroupControllerTest : ConstraintsIntegrationTest() {

    @BeforeEach
    fun clearAndSetupDefaults() {
        clearInvocations(coreClient)

        whenever(coreClient.validateLocationsZone(isA(), isA()))
            .thenReturn(ResultResponse(true))
    }

    @Test
    fun `create range group`() {
        Assertions.assertNotNull(testPostRangeGroups().id)
    }

    @Test
    fun `create two range groups`() {
        val id1 = testPostRangeGroups().id
        val id2 = testPostRangeGroups().id
        Assertions.assertNotEquals(id1, id2)
    }

    @Test
    fun `create and list groups`() {
        val id1 = testPostRangeGroups().id
        val id2 = testPostRangeGroups().id
        Assertions.assertNotEquals(id1, id2)

        val listResponse = testGetRangeGroupsAndReturn(mapOf("putawayZone" to "RACK"))
        Assertions.assertEquals(2, listResponse.content.size)
        Assertions.assertTrue(listResponse.content.any { dto -> dto.id == id1 })
        Assertions.assertTrue(listResponse.content.any { dto -> dto.id == id2 })

        Assertions.assertTrue(testGetRangeGroupsAndReturn(mapOf("putawayZone" to "EMPTY_ZONE")).content.isEmpty())
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/create-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/create-range/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRange successfully`() {
        testCreateRange("ok", expectedStatus = status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/create-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/create-range/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRange with non-existent range group id`() {
        testCreateRange("range-group-not-found", expectedStatus = status().isNotFound)
    }

    @ParameterizedTest
    @ValueSource(strings = ["intersecting-ranges-1", "intersecting-ranges-2", "intersecting-ranges-3"])
    @DatabaseSetup("/controller/admin/range-group/create-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/create-range/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRange with intersecting ranges`(testCase: String) {
        testCreateRange(testCase, expectedStatus = status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = ["tier-range-1", "tier-range-2"])
    @DatabaseSetup("/controller/admin/range-group/create-range/before_test_tiers.xml")
    fun `createRange with tier ranges`(testCase: String) {
        testCreateRange(testCase, expectedStatus = status().isOk, false)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/create-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/create-range/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRange returns bad request because locs are not in putaway zone`() {
        whenever(coreClient.validateLocationsZone(any(), any()))
            .thenReturn(ResultResponse(false))

        testCreateRange("invalid-locs-zone", expectedStatus = status().isBadRequest)

        verify(coreClient)
            .validateLocationsZone(listOf("A1-01-01D1", "A1-01-01D4"), "ZONE 1")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/create-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/create-range/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `createRange returns not found because loc doesn't exist`() {
        val coreErrorBody = WmsServiceErrorResponse(
            "Loc not found",
            HttpStatus.NOT_FOUND,
            WmsErrorCode.LOCATION_NOT_FOUND,
            mapOf("loc" to "A1-01-01D1")
        )

        whenever(coreClient.validateLocationsZone(any(), any()))
            .thenThrow(coreErrorBody.toWebClientResponseException())

        testCreateRange("loc-not-found", expectedStatus = status().isNotFound)

        verify(coreClient)
            .validateLocationsZone(listOf("A1-01-01D1", "A1-01-01D4"), "ZONE 1")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/get-by-id/data.xml")
    fun `getRangeGroup ok`() {
        mockMvc.perform(get("/admin/range-group/2"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/admin/range-group/get-by-id/ok/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/get-by-id/data.xml")
    fun `getRangeGroup not found`() {
        mockMvc.perform(get("/admin/range-group/300"))
            .andExpect(status().isNotFound)
            .andExpect(
                content().json(getFileContent("controller/admin/range-group/get-by-id/not-found/response.json"), true)
            )
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/all/data.xml")
    fun `getAllRangeGroups without parameters`() {
        testGetRangeGroups("without-parameters")
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/all/data.xml")
    fun `getAllRangeGroups filter by putaway zone`() {
        testGetRangeGroups("putaway-zone-filter", mapOf("putawayZone" to "ZONE 1"))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/all/data.xml")
    fun `getAllRangeGroups filter out all`() {
        testGetRangeGroups("empty-result", mapOf("putawayZone" to "ZONE 100"))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/all/data.xml")
    fun `getAllRangeGroups with limit and offset`() {
        testGetRangeGroups("limit-offset", mapOf("limit" to "1", "offset" to "1"))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/link-rule-group/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/link-rule-group/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `linkRuleGroup successfully`() {
        testLinkRuleGroup("ok", status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/link-rule-group/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/link-rule-group/link-already-exists/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `linkRuleGroup when link already exists updates collation`() {
        testLinkRuleGroup("link-already-exists", status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/link-rule-group/data.xml")
    fun `linkRuleGroup when range group not found`() {
        testLinkRuleGroup("not-found-range-group", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/link-rule-group/data.xml")
    fun `linkRuleGroup when rule group not found`() {
        testLinkRuleGroup("not-found-rule-group", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/unlink-rule-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/unlink-rule-group/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unlinkRuleGroup successfully`() {
        mockMvc.perform(delete("/admin/range-group/1/rule-group/2"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/unlink-rule-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/unlink-rule-group/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unlinkRuleGroup twice successfully`() {
        mockMvc.perform(delete("/admin/range-group/1/rule-group/2"))
            .andExpect(status().isOk)
        mockMvc.perform(delete("/admin/range-group/1/rule-group/2"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-range/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteRange successfully`() {
        mockMvc.perform(delete("/admin/range-group/1/range/2"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-range/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteRange twice successfully`() {
        mockMvc.perform(delete("/admin/range-group/1/range/2"))
            .andExpect(status().isOk)
        mockMvc.perform(delete("/admin/range-group/1/range/2"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-range/data.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-range/data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteRange that not in specified group`() {
        mockMvc.perform(delete("/admin/range-group/1000/range/2"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(content().json(getFileContent("controller/admin/range-group/delete-range/not-in-group/response.json")))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-group/empty/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `delete empty range group - ok`() {
        mockMvc.perform(delete("/admin/range-group/4"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-group/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to delete not empty range group - has linked ranges - exception`() {
        mockMvc.perform(delete("/admin/range-group/2"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.wmsErrorCode", equalTo("RANGE_GROUP_NOT_EMPTY")))
            .andExpect(jsonPath("$.wmsErrorData.rangeGroupId", equalTo(2)))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-group/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to delete not empty range group - has linked rule groups - exception`() {
        mockMvc.perform(delete("/admin/range-group/5"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.wmsErrorCode", equalTo("RANGE_GROUP_NOT_EMPTY")))
            .andExpect(jsonPath("$.wmsErrorData.rangeGroupId", equalTo(5)))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-group/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to delete not empty range group - has linked ranges and rule groups - exception`() {
        mockMvc.perform(delete("/admin/range-group/1"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.wmsErrorCode", equalTo("RANGE_GROUP_NOT_EMPTY")))
            .andExpect(jsonPath("$.wmsErrorData.rangeGroupId", equalTo(1)))
    }

    @Test
    @DatabaseSetup("/controller/admin/range-group/delete-group/before.xml")
    @ExpectedDatabase(
        "/controller/admin/range-group/delete-group/force/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try to force delete not empty range group - ok`() {
        mockMvc.perform(delete("/admin/range-group/3?force=true"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
    }

    private fun testCreateRange(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) {
        val dirPath = "controller/admin/range-group/create-range/$testCase"

        val request = post("/admin/range-group/range")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("$dirPath/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(expectedStatus)

        if (checkResponse) {
            resultActions
                .andExpect(content().json(getFileContent("$dirPath/response.json"), true))
        }
    }

    private fun testGetRangeGroupsAndReturn(params: Map<String, String> = mapOf()): GetPagedRangeGroupResponse {
        val request = get("/admin/range-group")
        params.forEach { request.param(it.key, it.value) }

        return mapper.readValue(
            mockMvc.perform(request)
                .andExpect(status().isOk)
                .andReturn().response.contentAsString
        )
    }

    private fun testPostRangeGroups(putawayZone: String = "RACK"): CreateRangeGroupResponse =
        mapper.readValue(
            mockMvc.perform(
                post("/admin/range-group")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"putawayZone\": \"$putawayZone\"}")
            )
                .andExpect(status().isOk)
                .andReturn().response.contentAsString,
        )

    private fun testGetRangeGroups(testCase: String, params: Map<String, String> = mapOf()) {
        val request = get("/admin/range-group")
        params.forEach { request.param(it.key, it.value) }

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(
                content().json(getFileContent("controller/admin/range-group/all/$testCase/response.json"), true)
            )
    }

    private fun testLinkRuleGroup(testCase: String, status: ResultMatcher, checkResponse: Boolean = true) {
        val request = post("/admin/range-group/rule-group")
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("controller/admin/range-group/link-rule-group/$testCase/request.json"))

        val resultActions = mockMvc.perform(request)
            .andExpect(status)

        if (checkResponse) {
            resultActions.andExpect(
                content().json(
                    getFileContent("controller/admin/range-group/link-rule-group/$testCase/response.json"),
                    true
                )
            )
        }
    }

    private fun WmsServiceErrorResponse.toWebClientResponseException(): WebClientResponseException {
        return WebClientResponseException.create(
            status.value(),
            message,
            HttpHeaders(),
            mapper.writeValueAsBytes(this),
            Charsets.UTF_8
        )
    }
}
