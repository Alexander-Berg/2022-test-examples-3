package ru.yandex.market.wms.core.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.core.base.dto.Turnover
import ru.yandex.market.wms.core.base.request.GetHighTurnoverSkuRequest
import ru.yandex.market.wms.core.base.request.GetSerialsTurnoverRequest
import ru.yandex.market.wms.core.base.request.MajorityZoneRequest
import ru.yandex.market.wms.core.base.request.SerialsTurnoverRequest
import ru.yandex.market.wms.core.base.response.GetHighTurnoverSkuResponse
import ru.yandex.market.wms.core.base.response.GetSerialsTurnoverResponse
import ru.yandex.market.wms.core.base.response.MajorityZoneResponse
import ru.yandex.market.wms.core.base.response.SerialsTurnoverResponse
import ru.yandex.market.wms.core.fromJson
import java.math.BigDecimal

class TurnoverControllerTest : IntegrationTest() {
    private val mapper = ObjectMapper()

    @Test
    @DatabaseSetup("/controller/turnover/locs_zones.xml")
    fun `Single putawayzone`() =
        Assertions.assertEquals("Z1", majorityZone(listOf(Pair("L1_1", 10), Pair("L1_2", 100))).zone)

    @Test
    @DatabaseSetup("/controller/turnover/locs_zones.xml")
    fun `Three zones`() =
        Assertions.assertEquals("Z3", majorityZone(listOf(Pair("L1_2", 10), Pair("L2", 10), Pair("L3", 20))).zone)

    @Test
    @DatabaseSetup("/controller/turnover/locs_zones.xml")
    fun `Three locs two zones`() =
        Assertions.assertEquals("Z1", majorityZone(listOf(Pair("L1_1", 10), Pair("L1_2", 10), Pair("L3", 10))).zone)

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `No bestsellers`() =
        Assertions.assertEquals(0, countBestsellers(listOf("102", "104")).highTurnoverCount)

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `Single bestseller`() =
        Assertions.assertEquals(1, countBestsellers(listOf("100", "102", "104")).highTurnoverCount)

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `All bestsellers`() =
        Assertions.assertEquals(3, countBestsellers(listOf("100", "101", "102", "103", "104")).highTurnoverCount)

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `getSerialsTurnover returns SLOW when there is no bestsellers`() {
        val response: GetSerialsTurnoverResponse = getSerialsTurnover(listOf("102", "104"))
        Assertions.assertEquals(Turnover.SLOW, response.turnover)
        Assertions.assertEquals(BigDecimal.ZERO, response.percent)
    }

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `getSerialsTurnover returns SLOW when the percentage of bestsellers is less than minimal percent`() {
        val response: GetSerialsTurnoverResponse = getSerialsTurnover(listOf("100", "102", "104"))
        Assertions.assertEquals(Turnover.SLOW, response.turnover)
        Assertions.assertEquals(BigDecimal.valueOf(33), response.percent)
    }

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `getSerialsTurnover returns FAST when the percentage of bestsellers is equals to minimal percent`() {
        val response: GetSerialsTurnoverResponse = getSerialsTurnover(listOf("100", "102"))
        Assertions.assertEquals(Turnover.FAST, response.turnover)
        Assertions.assertEquals(BigDecimal.valueOf(50), response.percent)
    }

    @Test
    @DatabaseSetup("/controller/turnover/serials_turnover.xml")
    fun `getSerialsTurnover returns FAST when the percentage of bestsellers is greater than minimal percent`() {
        val response: GetSerialsTurnoverResponse = getSerialsTurnover(listOf("100", "101", "102", "103", "104"))
        Assertions.assertEquals(Turnover.FAST, response.turnover)
        Assertions.assertEquals(BigDecimal.valueOf(60), response.percent)
    }

    @Test
    @DatabaseSetup("/controller/turnover/parent-id-hint/show-hint/before.xml")
    fun `getParentIdHint show hint`() {
        testGetParentIdHint("RCP01", "show-hint")
    }

    @Test
    @DatabaseSetup("/controller/turnover/parent-id-hint/no-parent-id/before.xml")
    fun `getParentIdHint id without parent`() {
        testGetParentIdHint("TM01", "no-parent-id")
    }

    @Test
    @DatabaseSetup("/controller/turnover/parent-id-hint/disabled-config/before.xml")
    fun `getParentIdHint with disabled config`() {
        testGetParentIdHint("RCP01", "disabled-config")
    }

    @Test
    @DatabaseSetup("/controller/turnover/parent-id-hint/not-in-fast-zone/before.xml")
    fun `getParentIdHint not in fast zone`() {
        testGetParentIdHint("RCP01", "not-in-fast-zone")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/disabled-config/before.xml")
    fun `recommendCart returns showHint = false when config disabled`() {
        testRecommendCart("RCP01", "1-01", "disabled-config")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/not-in-fast-zone/before.xml")
    fun `recommendCart returns showHint = false when loc not in fast zone`() {
        testRecommendCart("RCP01", "9-99", "not-in-fast-zone")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/id-without-children/before.xml")
    fun `recommendCart returns successful response when id doesnt have children`() {
        testRecommendCart("RCP01", "1-01", "id-without-children")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/id-with-children/before.xml")
    fun `recommendCart returns successful response when id has children`() {
        testRecommendCart("TM01", "1-01", "id-with-children")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/loc-not-found/before.xml")
    fun `recommendCart returns error response when location is not found by loc`() {
        testRecommendCart("RCP01", "1-01", "loc-not-found", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/id-empty/before.xml")
    fun `recommendCart returns error response when id is empty`() {
        testRecommendCart("RCP01", "1-01", "id-empty", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/nested-cart-exists/before.xml")
    fun `recommendCart returns cart id if there is nested cart for current worker`() {
        testRecommendCart("TM01", "1-01", "nested-cart-exists")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/non-nested-cart-exists/before.xml")
    fun `recommendCart returns cart id if there is no nested cart for current worker`() {
        testRecommendCart("RCP01", "1-01", "non-nested-cart-exists")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/fast-turnover/before.xml")
    fun `recommendCart returns fast turnover when there is fast turnover sku`() {
        testRecommendCart("TM01", "1-01", "fast-turnover")
    }

    @Test
    @DatabaseSetup("/controller/turnover/recommend-cart/loc-empty/before.xml")
    fun `recommendCart returns result without cartId when loc is blank`() {
        testRecommendCart("TM01", null, "loc-empty")
    }

    @Test
    @DatabaseSetup("/controller/turnover/get-high-turnover-skus/db.xml")
    fun `getHighTurnoverSkus with empty request`() {
        testGetHighTurnoverSku(
            requestSkus = emptyList(),
            expectedSkus = emptyList()
        )
    }

    @Test
    @DatabaseSetup("/controller/turnover/get-high-turnover-skus/db.xml")
    fun `getHighTurnoverSkus with multiple skus`() {
        testGetHighTurnoverSku(
            requestSkus = listOf(SkuId("123", "321"), SkuId("100", "ROV01"), SkuId("101", "ROV03")),
            expectedSkus = listOf(SkuId("100", "ROV01"), SkuId("101", "ROV03"))
        )
    }

    private fun majorityZone(locs: List<Pair<String, Int>>): MajorityZoneResponse =
        mockMvc.perform(MockMvcRequestBuilders.post("/turnover/majority-zone")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(MajorityZoneRequest(locs))))
            .andExpect(status().isOk).andReturn().response.contentAsString.fromJson()

    private fun countBestsellers(serials: Collection<String>): SerialsTurnoverResponse =
        mockMvc.perform(MockMvcRequestBuilders.post("/turnover/count-bestseller")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(SerialsTurnoverRequest(serials))))
            .andExpect(status().isOk).andReturn().response.contentAsString.fromJson()

    private fun getSerialsTurnover(serials: Collection<String>): GetSerialsTurnoverResponse =
        mockMvc.perform(MockMvcRequestBuilders.post("/turnover/get-for-serials")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(GetSerialsTurnoverRequest(serials))))
            .andExpect(status().isOk).andReturn().response.contentAsString.fromJson()

    private fun testGetParentIdHint(id: String, testCase: String) =
        mockMvc.perform(MockMvcRequestBuilders.get("/turnover/$id/parent-id-hint"))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("controller/turnover/parent-id-hint/$testCase/response.json")))

    private fun testRecommendCart(id: String, loc: String?, testCase: String, status: ResultMatcher = status().isOk) {
        val url = MockMvcRequestBuilders.get("/turnover/$id/recommend-cart")
        loc?.let {
            url.param("loc", loc)
        }
        mockMvc.perform(url)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status)
            .andExpect(content().json(getFileContent("controller/turnover/recommend-cart/$testCase/response.json")))
    }

    private fun testGetHighTurnoverSku(requestSkus: List<SkuId>, expectedSkus: List<SkuId>) {
        val response = mockMvc.perform(MockMvcRequestBuilders.post("/turnover/get-high-turnover-skus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsBytes(GetHighTurnoverSkuRequest(requestSkus))))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString.fromJson<GetHighTurnoverSkuResponse>()

        assertThat(response.skuIds)
            .containsExactlyInAnyOrderElementsOf(expectedSkus)
    }
}
