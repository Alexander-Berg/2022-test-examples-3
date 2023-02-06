package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class DoorConnectionControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/get_before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/get_before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDoorToCarriers() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors-to-carriers"),
            responseFile = "controller/shipping/doors-to-carriers/get_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/get_doors_with_shipments.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/get_doors_with_shipments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDoorToCarriersWithShipments() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors-to-carriers"),
            responseFile = "controller/shipping/doors-to-carriers/get_with_shipments_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/before_empty.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/before_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getDoorToCarriers_empty() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors-to-carriers"),
            responseFile = "controller/shipping/doors-to-carriers/get_empty_response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/put_before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/put_after_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putDoorToCarriers_success() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/doors-to-carriers"),
            requestFile = "controller/shipping/doors-to-carriers/put_request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/put_before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/put_before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putDoorToCarriers_doorsNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/doors-to-carriers"),
            requestFile = "controller/shipping/doors-to-carriers/put_request_wrong_doors.json",
            responseFile = "controller/shipping/doors-to-carriers/put_response_doors_not_found.json",
            status = MockMvcResultMatchers.status().isUnprocessableEntity
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/doors-to-carriers/put_before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/doors-to-carriers/put_before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun putDoorToCarriers_carriersNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/doors-to-carriers"),
            requestFile = "controller/shipping/doors-to-carriers/put_request_wrong_carriers.json",
            responseFile = "controller/shipping/doors-to-carriers/put_response_carriers_not_found.json",
            status = MockMvcResultMatchers.status().isUnprocessableEntity
        )
    }
}
