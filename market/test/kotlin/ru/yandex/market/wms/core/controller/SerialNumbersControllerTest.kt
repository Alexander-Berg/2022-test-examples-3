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
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class SerialNumbersControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/all-ok/before.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/all-ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocAndIdCheckOnHoldHappyPass() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/all-ok/request.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocAndIdCheckOnHoldWhenSerialsNotMoved() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/request.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocAndIdCheckOnHoldWithCheckSerialMovedFalseWhenSerialsNotMoved() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/request-checked-moved-false.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/all-ok/before.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/all-ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocAndIdCheckOnHoldWithCheckSerialMovedTrueHappyPass() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/all-ok/request-check-moved-true.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocAndIdCheckOnHoldWithCheckSerialMovedTrueWhenSerialsNotMoved() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/request-checked-moved-true.json",
            "controller/serials/move-to-loc-and-id-check-on-hold/serials-not-moved/response.json",
            MockMvcResultMatchers.status().is5xxServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/to-loc/before.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/to-loc/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocCheckOnHold() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/to-loc/request.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocCheckOnHoldWithEmptySerialList() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/empty-serials/request.json",
            null,
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocCheckOnHoldWithHoldOnLocAndLot() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/from-hold-loc/request.json",
            "controller/serials/move-to-loc-and-id-check-on-hold/from-hold-loc/response.json",
            MockMvcResultMatchers.status().is5xxServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/serials/move-to-loc-and-id-check-on-hold/with-pick-details/immutable.xml")
    @ExpectedDatabase("/controller/serials/move-to-loc-and-id-check-on-hold/with-pick-details/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun moveSerialNumbersToLocCheckOnHoldWithPickDetails() {
        testPostRequest(
            "/serials/move-serial-numbers",
            "controller/serials/move-to-loc-and-id-check-on-hold/with-pick-details/request.json",
            "controller/serials/move-to-loc-and-id-check-on-hold/with-pick-details/response.json",
            MockMvcResultMatchers.status().is5xxServerError
        )
    }

    private fun testPostRequest(path: String, request: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(FileContentUtils.getFileContent(request))
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(MockMvcResultMatchers.content().json(FileContentUtils.getFileContent(response), false))
        }
    }
}
