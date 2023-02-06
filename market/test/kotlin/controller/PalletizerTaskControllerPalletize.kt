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

class PalletizerTaskControllerPalletize : IntegrationTest() {
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
    @DatabaseSetup("/controller/tasks/palletize-start/happy/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/palletize-start/happy/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `palletize start happy 1`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/start"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-start/happy/1/request.json",
            "controller/tasks/palletize-start/happy/1/response.json",
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ1", DropMoveType.PLTZ_PRE_PALLET))
        verify(shippingClient).getDropLocationInfo(ArgumentMatchers.anyString())
    }

    @Test
    @DatabaseSetup("/controller/tasks/palletize-start/error/1/before.xml")
    fun `palletize start error 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/start"),
            MockMvcResultMatchers.status().isBadRequest,
            "controller/tasks/palletize-start/error/1/request.json",
            "controller/tasks/palletize-start/error/1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/palletize-finish/happy/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/palletize-finish/happy/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `palletize finish happy 1`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/1/request.json",
            "controller/tasks/palletize-finish/happy/1/response.json",
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
    }

    @Test
    @DatabaseSetup("/controller/tasks/palletize-finish/happy/2/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/palletize-finish/happy/2/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `palletize finish happy 2`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/palletize-finish/happy/2/request.json",
            "controller/tasks/palletize-finish/happy/2/response.json",
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "PLTZ_BUF1", DropMoveType.PLTZ_PALLET))
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

    @Test
    @DatabaseSetup("/controller/tasks/palletize-finish/error/1/before.xml")
    fun `palletize finish error 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isBadRequest,
            "controller/tasks/palletize-finish/error/1/request.json",
            "controller/tasks/palletize-finish/error/1/response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/tasks/palletize-finish/error/2/before.xml")
    fun `palletize finish error 2`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/palletize/finish"),
            MockMvcResultMatchers.status().isBadRequest,
            "controller/tasks/palletize-finish/error/2/request.json",
            "controller/tasks/palletize-finish/error/2/response.json",
        )
    }

}
