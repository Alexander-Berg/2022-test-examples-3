package ru.yandex.market.wms.palletizer.controller

import ru.yandex.maret.wms.shipping.core.model.DropLocationsResponse
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.isA
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.maret.wms.shipping.core.model.MoveDropRequest
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.client.ShippingClient

class PalletizerTaskControllerTaskTest : IntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var shippingClient: ShippingClient

    private fun setupMock() {
        whenever(shippingClient.moveDrop(isA()))
            .then { inv ->
                val req = inv.arguments[0] as MoveDropRequest
                jdbc.update("UPDATE wmwhse1.DROPID SET DROPLOC = '${req.toLoc}' WHERE DROPID = '${req.dropId}'")
            }
        whenever(shippingClient.getDropLocationInfo(ArgumentMatchers.anyString()))
            .thenReturn(
                DropLocationsResponse(
                    allowedLocations = listOf("DOOR1", "DOOR_BUF1"),
                    currentLocation = "",
                    isPlacedAtGate = false,
                    carrierName = null,
                )
            )
    }

    @AfterEach
    private fun resetMock() = reset(shippingClient)


    @Test
    @DatabaseSetup("/controller/tasks/create/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/create/after-create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `create tasks`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/create/request.json",
        )
    }

    @Test
    fun `create tasks drop not found`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            "controller/tasks/create/request.json",
            "controller/tasks/create/drop-not-found-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/create/after-create.xml")
    fun `create tasks already created`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            "controller/tasks/create/request.json",
            "controller/tasks/create/tasks-already-created-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/1/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/1/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/2/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/2/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 2`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/2/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/3/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/3/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 3`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/3/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/4/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/4/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 4`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/4/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/5/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/5/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 5`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/5/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/current/happy/6/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/current/happy/6/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `get current happy 6`() {
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/current"),
            MockMvcResultMatchers.status().isOk,
            emptyMap(),
            "controller/tasks/current/happy/6/response.json",
        )
    }
}
