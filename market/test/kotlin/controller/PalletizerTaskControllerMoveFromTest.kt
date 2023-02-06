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

class PalletizerTaskControllerMoveFromTest : IntegrationTest() {

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
    @DatabaseSetup("/controller/tasks/move-from/happy/1/before.xml")
    @ExpectedDatabase(
        value = "/controller/tasks/move-from/happy/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
    )
    fun `move to happy 1`() {
        setupMock()
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/from-pltz"),
            MockMvcResultMatchers.status().isOk,
            "controller/tasks/move-from/happy/1/request.json",
            "controller/tasks/move-from/happy/1/response.json",
        )
        verify(shippingClient).moveDrop(MoveDropRequest("DRP0001", "DOOR1", DropMoveType.PLTZ_MOVE))
    }

    @Test
    @DatabaseSetup("/controller/tasks/move-from/error/1/before.xml")
    fun `move to error 1`() {
        assertHttpCall(
            MockMvcRequestBuilders.post("/tasks/move/from-pltz"),
            MockMvcResultMatchers.status().isBadRequest,
            "controller/tasks/move-from/error/1/request.json",
            "controller/tasks/move-from/error/1/response.json",
        )
    }

}
