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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.maret.wms.shipping.core.enums.DropMoveType
import ru.yandex.maret.wms.shipping.core.model.MoveDropRequest
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.client.ShippingClient

class PalletizerTaskControllerFlowTest : IntegrationTest() {

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
    @DatabaseSetup("/controller/tasks/flow/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/flow/after1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `flow 1`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/create/request.json",
        )
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/to-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-to/happy/1/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/start"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-start/happy/1/request.json",
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/1/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/from-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-from/happy/1/request.json",
            null,
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_MOVE))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

    @Test
    @DatabaseSetup("/controller/tasks/flow/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/flow/after2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `flow 2`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/create/request.json",
        )
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/to-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-to/happy/2/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/1/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/from-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-from/happy/1/request.json",
            null,
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ1", DropMoveType.PLTZ_MOVE))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

    @Test
    @DatabaseSetup("/controller/tasks/flow/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/flow/after3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `flow 3`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/create/request.json",
        )
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/to-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-to/happy/1/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/start"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-start/happy/1/request.json",
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/2/request.json",
            null,
        )

        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_MOVE))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

    @Test
    @DatabaseSetup("/controller/tasks/flow/setup.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/flow/after4.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `flow 4`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/create"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/create/request.json",
        )
        assertHttpCall(
            MockMvcRequestBuilders.get("/tasks/by-drop"),
            MockMvcResultMatchers.status().isOk,
            mapOf("dropId" to "DRP0001"),
            null,
        )
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/to-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-to/happy/2/request.json",
            null,
        )

        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/2/request.json",
            null,
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ1", DropMoveType.PLTZ_MOVE))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

}
