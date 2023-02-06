package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerDoorInfoTest : IntegrationTest() {
    private val httpAssert = HttpAssert { mockMvc }

    /**
     * Trying to get door information when shipment in status NEW
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipment-door-info/shipment-not-started-before.xml")
    fun `get door info for standard shipment not started`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR-1/shipment"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/shipment-door-info/shipment-not-started-response.json"
        )
    }

    /**
     * Trying to get door information when shipment in status GATE
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipment-door-info/shipment-started-before.xml")
    fun `get door info for duty shipment started`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR-1/shipment"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/shipment-door-info/shipment-started-response.json"
        )
    }

    /**
     * Trying to get door information when no active shipments found
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipment-door-info/no-active-shipments-before.xml")
    fun `get door info with no active shipments`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR-1/shipment"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipment-door-info/no-active-shipments-response.json"
        )
    }

    /**
     * Trying to get door information when multiple active shipments found
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipment-door-info/multiple-active-shipments-before.xml")
    fun `get door info with multiple active shipments`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR-1/shipment"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipment-door-info/multiple-active-shipments-response.json"
        )
    }

    /**
     * Trying to get door information for shipment with withdrawal ids
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipment-door-info/shipment-with-withdrawals-before.xml")
    fun `get door info for shipment with withdrawal ids`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR-1/shipment"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/shipment-door-info/shipment-with-withdrawals-response.json"
        )
    }
}
