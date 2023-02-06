package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerStartTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
        "/controller/shipping/shipments/start/new_shipment_door1.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/start/gate_shipment_door1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun startShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
        "/controller/shipping/shipments/start/gate_shipment_door1.xml",
    )
    fun startShipmentWrongStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json",

            )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
        "/controller/shipping/shipments/start/new_shipment2_door1.xml",
        "/controller/shipping/shipments/start/gate_shipment_door1.xml",
    )
    fun startShipmentDoorInUse() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json",
            responseFile = "controller/shipping/shipments/start/shipment_already_started_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
    )
    fun startShipmentNothingToStart() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json",
            responseFile = "controller/shipping/shipments/start/shipment_nothing_to_start_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
        "/controller/shipping/shipments/start/new_shipment_door1.xml",
        "/controller/shipping/shipments/start/new_shipment2_door1.xml",
    )
    fun startShipmentTooManyToStart() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json",
            responseFile = "controller/shipping/shipments/start/shipment_too_many_to_start_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/start/shipment_setup_empty.xml",
        "/controller/shipping/shipments/start/new_shipment_door1_no_vehicle.xml",
    )
    fun startShipmentNoVehicle() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/start"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/start/start_shipment_request.json",
            responseFile = "controller/shipping/shipments/start/shipment_no_vehicle_response.json"
        )
    }
}
