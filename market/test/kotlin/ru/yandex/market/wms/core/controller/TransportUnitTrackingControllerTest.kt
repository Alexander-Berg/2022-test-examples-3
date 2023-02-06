package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

@DatabaseSetup("/controller/transport-unit-tracking/get-tracking/common/immutable-state.xml")
@ExpectedDatabase(value = "/controller/transport-unit-tracking/get-tracking/common/immutable-state.xml",
    assertionMode = NON_STRICT)
class TransportUnitTrackingControllerTest : IntegrationTest() {

    @Test
    fun getTracking() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/common/request.json",
            "controller/transport-unit-tracking/get-tracking/common/response.json",
            status().isOk
        )
    }

    @Test
    fun getTrackingWhenUnitIdParameterIsNull() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/unit-id-is-null/request.json",
            null,
            status().isBadRequest
        )
    }

    @Test
    fun getTrackingWhenNoRowsForUnit() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/no-rows-for-unit/request.json",
            "controller/transport-unit-tracking/get-tracking/no-rows-for-unit/response.json",
            status().isOk
        )
    }

    @Test
    fun getTrackingWhenStatusesParameterIsNull() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/statuses-array-is-null/request.json",
            null,
            status().isBadRequest
        )
    }

    @Test
    fun getTrackingWhenNoStatusesAreSpecified() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/statuses-array-is-empty/request.json",
            "controller/transport-unit-tracking/get-tracking/statuses-array-is-empty/response.json",
            status().isOk
        )
    }

    @Test
    fun getTrackingWhenAnotherStatusesAreSpecified() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/another-statuses/request.json",
            "controller/transport-unit-tracking/get-tracking/another-statuses/response.json",
            status().isOk
        )
    }

    @Test
    fun getTrackingWhenTimeFromIsNotSpecified() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/no-time-from/request.json",
            "controller/transport-unit-tracking/get-tracking/no-time-from/response.json",
            status().isOk
        )
    }

    @Test
    fun getTrackingWhenTimeFromIsEqualToRowTime() {
        testPostRequest(
            "controller/transport-unit-tracking/get-tracking/time-from-is-equal-to-row-time/request.json",
            "controller/transport-unit-tracking/get-tracking/time-from-is-equal-to-row-time/response.json",
            status().isOk
        )
    }

    private fun testPostRequest(request: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                post("/transport-unit-tracking")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(request))
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response)))
        }
    }
}
