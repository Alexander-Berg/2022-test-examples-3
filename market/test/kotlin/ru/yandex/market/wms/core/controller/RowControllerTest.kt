package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class RowControllerTest : IntegrationTest() {

    @Test
    fun `Get row fullness without zone returns a bad request response`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf("fullnessPlacementType" to "ITEM"),
            expectedStatus = status().isBadRequest
        )
    }

    @Test
    fun `Get row fullness without id type returns a bad request response`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf("zone" to "MEZONIN_1L"),
            expectedStatus = status().isBadRequest
        )
    }

    @Test
    fun `Get row fullness with wrong id type returns a bad request response`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf(
                "zone" to "MEZONIN_1L",
                "idType" to "TEST_WRONG_TYPE"
            ),
            expectedStatus = status().isBadRequest
        )
    }

    @Test
    fun `Get row fullness with non-existent id specification returns a bad request response`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf(
                "zone" to "MEZONIN_1L",
                "idType" to "RCP"
            ),
            expectedStatus = status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/row/fullness/before.xml")
    fun `Get row fullness with correct zone and id type type returns a successful response for id placement type`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf(
                "zone" to "MEZONIN_1L",
                "idType" to "RCP"
            ),
            expectedStatus = status().isOk,
            testResourceDir = "id-success"
        )
    }

    @Test
    @DatabaseSetup("/controller/row/fullness/before.xml")
    fun `Get row fullness with correct zone and id type returns a successful response for item placement type`() {
        getWithAllChecks(
            urlPart = "fullness",
            params = mapOf(
                "zone" to "MEZONIN_1L",
                "idType" to "CART"
            ),
            expectedStatus = status().isOk,
            testResourceDir = "item-success"
        )
    }

    @Test
    @DatabaseSetup("/controller/row/between-locs/ok/before.xml")
    fun `Get row between loc`() {
        getWithAllChecks(
            urlPart = "between-locs",
            params = mapOf(
                "startLoc" to "A1-01-01A2",
                "endLoc" to "A1-05-01A1"
            ),
            expectedStatus = status().isOk,
            testResourceDir = "ok"
        )
    }


    private fun getWithAllChecks(
        urlPart: String,
        params: Map<String, String>,
        expectedStatus: ResultMatcher,
        testResourceDir: String? = null,
    ) {
        val paramsMVM: MultiValueMap<String, String> = LinkedMultiValueMap()
        paramsMVM.setAll(params)

        val requestBuilder = MockMvcRequestBuilders.get("/row/$urlPart")
            .params(paramsMVM)
            .contentType(MediaType.APPLICATION_JSON)

        val result = mockMvc.perform(requestBuilder)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(expectedStatus)

        if (testResourceDir != null) {
            result.andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/row/$urlPart/$testResourceDir/response.json"), true
                )
            )
        }
    }
}
