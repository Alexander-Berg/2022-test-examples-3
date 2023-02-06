package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerFinishTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/shipped_shipment_door1.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/closed_shipment_door1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
    )
    fun `finish shipment that was not found`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/finish"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/finish/finish_shipment_request.json",
            responseFile = "controller/shipping/shipments/finish/shipment_not_found_response.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/gate_shipment_door1.xml",
    )
    fun `finish shipment with wrong status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/finish"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/finish/finish_shipment_request.json",
            responseFile = "controller/shipping/shipments/finish/shipment_wrong_status_response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/closed_shipment_door1.xml",
    )
    fun `finish shipment that was already closed`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/finish"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/shipments/finish/finish_shipment_request.json",
            responseFile = "controller/shipping/shipments/finish/closed_shipment_response.json"
        )
    }


    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/one_outbound_no_number.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with one outbound and no number`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/three_outbounds_no_numbers.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with three outbounds and no numbers`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/three_outbounds_no_matching_numbers.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with three outbounds and no matching numbers`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/three_outbounds_one_matching_number.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_4.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with three outbounds and one matching number`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/three_outbounds_two_matching_numbers.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_5.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with three outbounds and two matching numbers`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/no_outbounds_today.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_6.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with not today's outbounds`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/closed_outbounds.xml",
    )
    @ExpectedDatabase(
        value =  "/controller/shipping/shipments/finish/outbound/closed_outbounds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with closed outbounds`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/one_closed_outbound.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_7.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with one closed outbound`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment.xml",
        "/controller/shipping/shipments/finish/outbound/no_outbound.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/no_outbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with no outbounds`() = finishShipmentOk()

    private fun finishShipmentOk() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/finish"),
            requestFile = "controller/shipping/shipments/finish/finish_shipment_request.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment_departure_yesterday.xml",
        "/controller/shipping/shipments/finish/outbound/one_outbound_yesterday.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_9.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with yesterday's departure date`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment_two_carriers.xml",
        "/controller/shipping/shipments/finish/outbound/two_outbounds_for_one_vehicle.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_10.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment with two outbounds for one vehicle`() = finishShipmentOk()

    @Test
    @DatabaseSetup(
        "/controller/shipping/shipments/finish/shipment_setup_empty.xml",
        "/controller/shipping/shipments/finish/outbound/shipped_shipment_same_drps.xml",
        "/controller/shipping/shipments/finish/outbound/two_outbounds_mapped_to_one_carrier.xml",
    )
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/finish/outbound/outbound_register_11.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `finish shipment when two carriers mapped to one`() = finishShipmentOk()
}
