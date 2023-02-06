package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerPatchTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    /**
     * Trying to change shipment that not found in DB.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_empty.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNotFoundShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/patch/edit_not_found_shipment_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_not_found_shipment_response.json"
        )
    }

    /**
     * Trying to change shipment with SHIPPED status.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentInShippedStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_in_shipped_status_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_in_shipped_status_response.json"
        )
    }

    /**
     * Trying to change door for shipment with LOAD status.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorInLoadStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_in_load_status_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_in_load_status_response.json"
        )
    }

    /**
     * Trying to change door for shipment with GATE status.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorInGateStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_in_gate_status_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_in_gate_status_response.json"
        )
    }

    /**
     * Trying to change door to blank string.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorToBlank() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_to_blank_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_to_blank_response.json"
        )
    }

    /**
     * Trying to change vehicle to blank string.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsVehicleToBlank() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_vehicle_to_blank_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_vehicle_to_blank_response.json"
        )
    }

    /**
     * Trying to change door to not found door.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorToNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_to_not_found_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_to_not_found_response.json"
        )
    }

    /**
     * Trying to change door to door with other carrier code.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorToDoorWithOtherCarrier() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_to_door_with_other_carrier_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_to_door_with_other_carrier_response.json"
        )
    }

    /**
     * Trying to change door to door with other not terminated shipment.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsDoorToDoorWithOtherShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_door_to_door_with_other_shipment_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_door_to_door_with_other_shipment_response.json"
        )
    }

    /**
     * Trying to change vehicle to another that on load shipment now.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShipmentsVehicleToVehicleWithOtherShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/patch/edit_shipment_vehicle_to_other_that_on_gate_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipment_vehicle_to_other_that_on_gate_response.json"
        )
    }

    /**
     * Changing vehicle to another for shipment in shipped status.
     * Must throw exception.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editShippedShipmentsVehicle() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_shipped_shipment_vehicle_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_shipped_shipment_vehicle_response.json"
        )
    }

    /**
     * Changing vehicle to another for shipment in gate status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_gate_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editGateShipmentsVehicle() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_gate_shipment_vehicle_request.json"
        )
    }

    /**
     * Changing door to another for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_door_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsDoor() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_door_request.json"
        )
    }

    /**
     * Changing vehicle to another for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_vehicle_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsVehicle() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_vehicle_request.json"
        )
    }

    /**
     * Changing vehicle and door to another for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_vehicle_and_door_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsVehicleAndDoor() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_vehicle_and_door_request.json"
        )
    }

    /**
     * Changing door to another and vehicle to same for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_same_vehicle_and_door_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsSameVehicleAndDoor() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_same_vehicle_and_door_request.json"
        )
    }

    /**
     * Changing vehicle to another and same door for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_vehicle_and_same_door_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsVehicleAndSameDoor() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_vehicle_and_same_door_request.json"
        )
    }

    /**
     * Try to change vehicle and door to same vehicle and door for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_same_vehicle_and_same_door_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun editNewShipmentsSameVehicleAndSameDoor() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_same_vehicle_and_same_door_request.json"
        )
    }

    /**
     * Changing vehicle to another for shipment in NEW status.
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_vehicle_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `edit new shipments vehicle with lower case and with spaces in request`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_vehicle_request_with_lower_case.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `vehicle number does not match regex`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/wrong_vehicle_number_format_request.json",
            responseFile = "controller/shipping/shipments/patch/wrong_vehicle_number_format_response.json"
        )
    }

    /**
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_departure_time_future_gate_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `edit departure time after current time for shipment in GATE status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_gate_shipment_departure_time_future_request.json"
        )
    }

    /**
     * Positive case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/after_change_departure_time_past_new_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `edit departure time before current time for shipment in NEW status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            requestFile = "controller/shipping/shipments/patch/edit_new_shipment_departure_time_past_request.json"
        )
    }

    /**
     * Negative case.
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/patch/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/patch/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `edit departure time for withdrawal shipment`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.patch("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/patch/edit_withdrawal_shipment_departure_time_request.json",
            responseFile = "controller/shipping/shipments/patch/edit_withdrawal_shipment_departure_time_response.json"
        )
    }
}
