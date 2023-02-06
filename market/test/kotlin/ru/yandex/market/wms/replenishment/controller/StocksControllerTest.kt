package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.CollectionUtils
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class StocksControllerTest : IntegrationTest() {
    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s0 OK no stocks`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s0_ok_request.json",
            expectedResponseFileName = "controller/stocks/s0_ok_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1 OK`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1_ok_request.json",
            expectedResponseFileName = "controller/stocks/s1_ok_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1s2 OK`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1s2_ok_request.json",
            expectedResponseFileName = "controller/stocks/s1s2_ok_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1s2 Expire`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1s2_expire_request.json",
            expectedResponseFileName = "controller/stocks/s1s2_expire_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1 plan util conflicts with man util in request`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is4xxClientError,
            requestFileName = "controller/stocks/s1_two_exclusive_request.json",
            expectedResponseFileName = "controller/stocks/s1_two_exclusive_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1 man util by lot and by loc on same goods`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1_man_util_request.json",
            expectedResponseFileName = "controller/stocks/s1_man_util_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1 damage with partially taken to plan util`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1_damage_request.json",
            expectedResponseFileName = "controller/stocks/s1_damage_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/setup.xml")
    fun `s1 plan util totally conflicts with man util in balances`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/s1_plan_util_request.json",
            expectedResponseFileName = "controller/stocks/s1_plan_util_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/no-stocks.xml")
    fun `no stocks available without hold if id is empty`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/fit_no_stocks_request.json",
            expectedResponseFileName = "controller/stocks/fit_no_stocks_response.json",
        )

    @Test
    @DatabaseSetup("/controller/stocks/no-stocks.xml")
    fun `no stocks available with hold if id is empty`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/stocks/any-storage-area"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/stocks/hold_no_stocks_request.json",
            expectedResponseFileName = "controller/stocks/hold_no_stocks_response.json",
        )


    private fun callRestEndpointAndAssertResponse(
        endpoint: MockHttpServletRequestBuilder,
        params: Map<String, String> = mapOf(),
        requestFileName: String = "",
        expectedStatus: ResultMatcher,
        expectedResponseFileName: String = "",
    ) {
        mockMvc.perform(endpoint
            .accept(MediaType.APPLICATION_JSON)
            .params(CollectionUtils.toMultiValueMap(params.mapValues { listOf(it.value) }))
            .contentType(MediaType.APPLICATION_JSON)
            .content(if (requestFileName != "") FileContentUtils.getFileContent(requestFileName) else "")
        ).run {
            andExpect(expectedStatus)
            if (expectedResponseFileName != "") {
                andExpect(content().json(FileContentUtils.getFileContent(expectedResponseFileName)))
            }
        }
    }
}
