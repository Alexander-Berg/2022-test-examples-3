package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerPostTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    fun `create standard shipment without departure time`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_standard_without_departure_time_request.json",
            responseFile = "controller/shipping/shipments/post/add_standard_without_departure_time_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_duty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create duty shipment without departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_duty_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create withdrawal shipment without departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_withdrawal_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `carriers and door are not connected`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/post/add_to_wrong_door_request.json",
            responseFile = "controller/shipping/shipments/post/wrong_door_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/post/common.xml",
        "/controller/shipping/shipments/post/before_added_to_not_ready_door.xml"
    )
    @ExpectedDatabases(
        ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase("/controller/shipping/shipments/post/before_added_to_not_ready_door.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
    )
    fun `door already has open shipment`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/post/add_to_not_ready_door_request.json",
            responseFile = "controller/shipping/shipments/post/not_ready_door_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/post/common.xml",
        "/controller/shipping/shipments/post/before_added_already_processing_vehicle.xml"
    )
    @ExpectedDatabases(
        ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase("/controller/shipping/shipments/post/before_added_already_processing_vehicle.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
    )
    fun `vehicle is already involved in the shipment`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/post/add_to_wrong_vehicle_request.json",
            responseFile = "controller/shipping/shipments/post/wrong_vehicle_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create duty shipment with carrier codes`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_duty_and_standard_request.json",
            responseFile = "controller/shipping/shipments/post/add_duty_and_standard_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create duty shipment with withdrawal ids`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_duty_and_withdrawal_request.json",
            responseFile = "controller/shipping/shipments/post/add_duty_and_withdrawal_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `standard shipment with withdrawal ids`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_standard_and_withdrawal_request.json",
            responseFile = "controller/shipping/shipments/post/add_standard_and_withdrawal_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create standard shipment without carrier codes`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_standard_without_carriers_request.json",
            responseFile = "controller/shipping/shipments/post/add_standard_without_carriers_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create withdrawal shipment with carrier codes`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_withdrawal_with_carriers_request.json",
            responseFile = "controller/shipping/shipments/post/add_withdrawal_with_carriers_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create withdrawal shipment without withdrawal ids`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/add_withdrawal_without_withdrawal_ids_request.json",
            responseFile = "controller/shipping/shipments/post/add_withdrawal_without_withdrawal_ids_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_standard_current_departure_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create standard shipment with current departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_standard_current_departure_date_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_standard_future_departure_date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create standard shipment with future departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_standard_future_departure_date_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_standard.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `add standard shipment with vehicle in lower case and with spaces in request`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_standard_request_with_vehicle_lowercase.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/after_fill_standard_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create standard shipment with manual vehicle number input`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/add_standard_manual_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `vehicle number does not match regex`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/wrong_vehicle_number_format_request.json",
            responseFile = "controller/shipping/shipments/post/wrong_vehicle_number_format_response.json"
        )
    }

    /**
     * withdrawal 10 exists,
     * withdrawal 40 has wrong type,
     * withdrawal 50 is in final status,
     * order 60 is not a withdrawal at all,
     * withdrawal 70 does not exist
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/post/common.xml")
    @ExpectedDatabase("/controller/shipping/shipments/post/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `trying to create shipment with wrong withdrawalIds`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/wrong_withdrawal_ids_request.json",
            responseFile = "controller/shipping/shipments/post/wrong_withdrawal_ids_response.json"
        )

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/post/common.xml",
        "/controller/shipping/shipments/post/existing_withdrawal_shipments_before.xml"
    )
    @ExpectedDatabase(
        "/controller/shipping/shipments/post/existing_withdrawal_shipments_before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `trying to create shipment with withdrawalIds that exist in another shipment`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/shipments/post/existing_invalid_withdrawal_ids_request.json",
            responseFile = "controller/shipping/shipments/post/existing_invalid_withdrawal_ids_response.json"
        )

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/post/common.xml",
        "/controller/shipping/shipments/post/existing_withdrawal_shipments_before.xml"
    )
    @ExpectedDatabase(
        "/controller/shipping/shipments/post/existing_withdrawal_shipments_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `creating shipment with withdrawalIds that exist in closed and cancelled shipments`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/shipments"),
            requestFile = "controller/shipping/shipments/post/existing_valid_withdrawal_ids_request.json",
        )
}
