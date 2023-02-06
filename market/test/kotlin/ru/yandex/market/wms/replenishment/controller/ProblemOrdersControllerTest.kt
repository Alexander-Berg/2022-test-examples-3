package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.CollectionUtils
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class ProblemOrdersControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    fun `list problem orders`() =
        callRestEndpointAndAssertResponse(
            endpoint = get("/problem-orders"),
            expectedStatus = status().is2xxSuccessful,
            expectedResponseFileName = "controller/problem-orders/response/list.json",
        )

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    fun `list problem orders with filter`() =
        callRestEndpointAndAssertResponse(
            endpoint = get("/problem-orders"),
            params = mapOf(
                "filter" to "STORERKEY==STORER3",
                "limit" to "2",
                "offset" to "1",
                "sort" to "SKU",
                "order" to "ASC",
            ),
            expectedStatus = status().is2xxSuccessful,
            expectedResponseFileName = "controller/problem-orders/response/list-filtered.json",
        )

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    fun `get problem order`() =
        callRestEndpointAndAssertResponse(
            endpoint = get("/problem-orders/12"),
            expectedStatus = status().is2xxSuccessful,
            expectedResponseFileName = "controller/problem-orders/response/get-single.json",
        )

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    @ExpectedDatabase(
        value = "/controller/problem-orders/after/created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `create problem order`() =
        callRestEndpointAndAssertResponse(
            endpoint = post("/problem-orders"),
            expectedStatus = status().is2xxSuccessful,
            requestFileName = "controller/problem-orders/request/create.json",
            expectedResponseFileName = "controller/problem-orders/response/created.json",
        )

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    @ExpectedDatabase(
        value = "/controller/problem-orders/after/updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `update problem order`() =
        callRestEndpointAndAssertResponse(
            endpoint = put("/problem-orders/16"),
            requestFileName = "controller/problem-orders/request/update.json",
            expectedStatus = status().is2xxSuccessful,
            expectedResponseFileName = "controller/problem-orders/response/updated.json",
        )

    @Test
    @DatabaseSetup("/controller/problem-orders/before/before.xml")
    @ExpectedDatabase(
        value = "/controller/problem-orders/after/deleted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `delete problem order`() =
        callRestEndpointAndAssertResponse(
            endpoint = delete("/problem-orders/14"),
            expectedStatus = status().is2xxSuccessful
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
