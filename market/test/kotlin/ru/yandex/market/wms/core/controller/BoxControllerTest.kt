package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent

class BoxControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/common/immutable.xml", "/controller/box/db/immutable.xml")
    @ExpectedDatabase("/controller/box/db/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLocationsHappyPath() {
        testPostRequest(
            "/box/locations",
            "controller/box/request/getLocationsHappyPath.json",
            "controller/box/response/getLocationsHappyPath.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/common/immutable.xml", "/controller/box/db/immutable.xml")
    @ExpectedDatabase("/controller/box/db/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLocationsReturnsEmptyLocations() {
        testPostRequest(
            "/box/locations",
            "controller/box/request/getLocationsUnknownBox.json",
            "controller/box/response/getLocationsUnknownBox.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/common/immutable.xml", "/controller/box/db/immutable.xml")
    @ExpectedDatabase("/controller/box/db/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getLocationsNoBoxNameSpecifiedReturnsError() {
        testPostRequest(
            "/box/locations",
            "controller/box/request/getLocationsNoBoxName.json",
            null,
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/common/immutable.xml", "/controller/box/db/immutable.xml")
    @ExpectedDatabase("/controller/box/db/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxInfoHappyPath() {
        testPostRequest(
            "/box/info",
            "controller/box/request/getBoxInfoHappyPath.json",
            "controller/box/response/getBoxInfoHappyPath.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/common/immutable.xml", "/controller/box/db/immutable.xml")
    @ExpectedDatabase("/controller/box/db/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxInfoNotFoundBoxReturnsError() {
        testPostRequest(
            "/box/info",
            "controller/box/request/getBoxInfoNotFoundBox.json",
            null,
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/box/get-dimensions/immutable-state.xml")
    @ExpectedDatabase("/controller/box/get-dimensions/immutable-state.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxDimensions() {
        testGetRequest(
            "/box/dimensions/UNIT001",
            "controller/box/get-dimensions/one-dimension-measurement/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/get-dimensions/immutable-state.xml")
    @ExpectedDatabase("/controller/box/get-dimensions/immutable-state.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxDimensionsWhenSeveralMeasurementsWereMade() {
        testGetRequest(
            "/box/dimensions/UNIT002",
            "controller/box/get-dimensions/several-dimension-measurements/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/get-dimensions/immutable-state.xml")
    @ExpectedDatabase("/controller/box/get-dimensions/immutable-state.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxDimensionsWhenNoMeasurementsWereMade() {
        testGetRequest(
            "/box/dimensions/UNIT003",
            "controller/box/get-dimensions/no-dimension-measurements/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/get-dimensions/immutable-state.xml")
    @ExpectedDatabase("/controller/box/get-dimensions/immutable-state.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxDimensionsWhenNoBoxIdSpecified() {
        testGetRequest(
            "/box/dimensions/",
            null,
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/box/status/before.xml")
    @ExpectedDatabase("/controller/box/status/before.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxStatusTest() {
        testGetRequest(
            "/box/status/UNIT003",
            "controller/box/status/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/status/before-is-dropped.xml")
    @ExpectedDatabase("/controller/box/status/before-is-dropped.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxStatusTest_isDropped() {
        testGetRequest(
            "/box/status/UNIT003",
            "controller/box/status/response-is-dropped.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/status/before-is-loaded.xml")
    @ExpectedDatabase("/controller/box/status/before-is-loaded.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxStatusTest_isLoaded() {
        testGetRequest(
            "/box/status/UNIT003",
            "controller/box/status/response-is-loaded.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/box/status/before-is-shipped.xml")
    @ExpectedDatabase("/controller/box/status/before-is-shipped.xml", assertionMode = NON_STRICT_UNORDERED)
    fun getBoxStatusTest_isShipped() {
        testGetRequest(
            "/box/status/UNIT003",
            "controller/box/status/response-is-shipped.json",
            status().isOk
        )
    }

    private fun testPostRequest(path: String, request: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(request))
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response), false))
        }
    }

    private fun testGetRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                get(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            println(result.andReturn().response.contentAsString)
            result.andExpect(content().json(getFileContent(response), false))
        }
    }
}
