package ru.yandex.market.wms.replenishment.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.json.JSONException
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class CommonTaskControllerTest : IntegrationTest() {

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/move-in-work/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/move-in-work/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get move task in work`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-01.json",
            "controller/replenishment-task/common/response/move-in-work.json",
            MockMvcRequestBuilders.put("/common/buffer/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-move-up-task/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-move-up-task/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get move up task by buffer`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-05.json",
            "controller/replenishment-task/common/response/buffer-move-up-task.json",
            MockMvcRequestBuilders.put("/common/buffer/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-pick-task-not-assigned/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-pick-task-not-assigned/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    @Test
    fun `get only move task by buffer no tasks`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-04.json",
            null,
            MockMvcRequestBuilders.put("/common/buffer/current/move"),
            MockMvcResultMatchers.status().isNoContent,
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-move-up-task/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-move-up-task/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    @Test
    fun `get only move up task by buffer`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-05.json",
            "controller/replenishment-task/common/response/buffer-move-up-task.json",
            MockMvcRequestBuilders.put("/common/buffer/current/move"),
            MockMvcResultMatchers.status().isOk,
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-pick-task/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-pick-task/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get pick task by buffer`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-05.json",
            "controller/replenishment-task/common/response/buffer-pick-task.json",
            MockMvcRequestBuilders.put("/common/buffer/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/pick-in-work/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/pick-in-work/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get pick task in work`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-01.json",
            "controller/replenishment-task/common/response/pick-in-work.json",
            MockMvcRequestBuilders.put("/common/buffer/current"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-pick-task/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-pick-task/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    @Test
    fun `get only pick task by buffer`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-05.json",
            "controller/replenishment-task/common/response/buffer-pick-task.json",
            MockMvcRequestBuilders.put("/common/buffer/current/pick"),
            MockMvcResultMatchers.status().isOk,
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-move-up-task-not-assigned/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-move-up-task-not-assigned/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get only pick up task by buffer no tasks`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-04.json",
            null,
            MockMvcRequestBuilders.put("/common/buffer/current/pick"),
            MockMvcResultMatchers.status().isNoContent,
        )
    }

    @DatabaseSetup(
        "/controller/replenishment-task/sku-loc-common.xml",
        "/controller/replenishment-task/common/db/buffer-move-down-task/before.xml",
    )
    @ExpectedDatabase(
        value = "/controller/replenishment-task/common/db/buffer-move-down-task/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `get task by buffer no tasks`() {
        assertApiCall(
            "controller/replenishment-task/common/request/buffer-05.json",
            null,
            MockMvcRequestBuilders.put("/common/buffer/current"),
            MockMvcResultMatchers.status().isNoContent
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/change-buffer/before.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/change-buffer/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change buffer for tasks`() {
        assertApiCall(
            "controller/replenishment-task/common/request/change-buffer.json",
            null,
            MockMvcRequestBuilders.post("/manage/change-buffer"),
            MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/change-buffer/before-not-empty.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/change-buffer/before-not-empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change buffer not empty test`() {
        assertApiCall(
            "controller/replenishment-task/common/request/change-buffer.json",
            null,
            MockMvcRequestBuilders.post("/manage/change-buffer"),
            MockMvcResultMatchers.status().is4xxClientError
        )
    }

    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/change-priority/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/change-priority/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change priority happy test`() {
        assertApiCall(
            "controller/replenishment-task/common/request/change-priority.json",
            null,
            MockMvcRequestBuilders.post("/manage/change-priority"),
            MockMvcResultMatchers.status().isOk
        )
    }


    @Test
    @DatabaseSetup("/controller/replenishment-task/move/db/change-priority/2/before.xml")
    @ExpectedDatabase(
        value = "/controller/replenishment-task/move/db/change-priority/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `change priority user value happy test`() {
        assertApiCall(
            "controller/replenishment-task/common/request/change-priority-1.json",
            null,
            MockMvcRequestBuilders.post("/manage/change-priority"),
            MockMvcResultMatchers.status().isOk
        )
    }

    private fun assertApiCall(
        requestFile: String?, responseFile: String?, request: MockHttpServletRequestBuilder, status: ResultMatcher
    ) {
        val requestBody = if (requestFile == null) "" else FileContentUtils.getFileContent(requestFile)
        val result = mockMvc.perform(
            request
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status)
        if (responseFile != null) {
            val response = FileContentUtils.getFileContent(responseFile)
            try {
                result.andExpect(MockMvcResultMatchers.content().json(response, false))
            } catch (e: JSONException) {
                result.andExpect(MockMvcResultMatchers.content().string(response))
            }
        }
    }
}
