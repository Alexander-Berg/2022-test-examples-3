package ru.yandex.market.wms.palletizer.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest

class PalletizerTaskControllerByDropTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/happy/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/happy/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop happy 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/happy/1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/happy/1.1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/happy/1.1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop happy 1-1`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/happy/1.1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/happy/1.2/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/happy/1.2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop happy 1-2`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/happy/1.2/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/happy/2/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/happy/2/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop happy 2`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/happy/2/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/happy/3/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/happy/3/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop happy 3`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/happy/3/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/error/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/error/1/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop error 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isBadRequest,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/error/1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/error/2/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/error/2/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop error 2`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/error/2/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/error/3/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/error/3/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop error 3`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isInternalServerError,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/error/3/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/by-drop/error/4/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/by-drop/error/4/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get task by drop error 4`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isInternalServerError,
            mapOf("dropId" to "DRP0001"),
            "controller/tasks/by-drop/error/4/response.json",
        )
    }

    @Test
    fun `get task by drop error 5`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isBadRequest,
            mapOf("dropId" to "NOTDROP"),
            "controller/tasks/by-drop/error/5/response.json",
        )
    }
}
