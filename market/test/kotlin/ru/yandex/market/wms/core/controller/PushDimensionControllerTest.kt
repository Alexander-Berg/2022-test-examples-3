package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class PushDimensionControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/push-dimension/db/before/pushDimensionSyncHappyPathBefore.xml")
    @ExpectedDatabase(
        "/controller/push-dimension/db/after/pushDimensionSyncHappyPathAfter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushDimensionSyncHappyPath() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/push-dimension")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils
                        .getFileContent(
                            "controller/push-dimension/request/pushDimensionSyncHappyPathRequest.json"
                        )
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/push-dimension/db/before/pushDimensionUpdateBefore.xml")
    @ExpectedDatabase(
        "/controller/push-dimension/db/after/pushDimensionUpdateAfter.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushDimensionInsertNewOne() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/push-dimension")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils
                        .getFileContent("controller/push-dimension/request/pushDimensionUpdateRequest.json")
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/push-dimension/db/before/pushDimensionNotFoundBefore.xml")
    @ExpectedDatabase(
        "/controller/push-dimension/db/before/pushDimensionNotFoundBefore.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushDimensionNotFound() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/push-dimension")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils
                        .getFileContent("controller/push-dimension/request/pushDimensionNotFoundRequest.json")
                )
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils.getFileContent(
                        "controller/push-dimension/response/pushDimensionNotFoundResponse.json"
                    ),
                    false
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/push-dimension/db/before/pushDimensionNotFoundBefore.xml")
    @ExpectedDatabase(
        "/controller/push-dimension/db/before/pushDimensionNotFoundBefore.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun pushDimensionUnknownUnitId() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/push-dimension")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    FileContentUtils
                        .getFileContent(
                            "controller/push-dimension/request/pushDimensionUnknownUnitIdRequest.json"
                        )
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
