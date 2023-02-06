package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import java.nio.charset.StandardCharsets

class PickDetailControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/pickdetail/get-by-tracking-id/immutable.xml")
    @ExpectedDatabase("/controller/pickdetail/get-by-tracking-id/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getPickDetailsByTrackingIdSuccessful() {
        testGetRequest(
            "/pickdetail/TRACK-F1",
            "controller/pickdetail/get-by-tracking-id/happy-path/response.json",
            expectedStatus = MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/pickdetail/get-by-tracking-id/immutable.xml")
    @ExpectedDatabase("/controller/pickdetail/get-by-tracking-id/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getPickDetailsByNonExistingTrackingId() {
        testGetRequest(
            "/pickdetail/TRACK-F5",
            "controller/pickdetail/get-by-tracking-id/non-existing/response.json",
            expectedStatus = MockMvcResultMatchers.status().isOk
        )
    }

    private fun testGetRequest(path: String, expectedFileResponse: String, expectedStatus: ResultMatcher) {
        val requestBuilder = MockMvcRequestBuilders.get(path)
            .contentType(MediaType.APPLICATION_JSON)

        val mvcResult = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)
            .andReturn()

        JsonAssertUtils.assertFileNonExtensibleEquals(expectedFileResponse,
            mvcResult.response.getContentAsString(StandardCharsets.UTF_8))
    }
}
