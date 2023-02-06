package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerDeleteTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    /**
     * Trying to remove not exists shipment by id
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/delete/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/delete/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteNotExistShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/shipments/5"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/delete/shipment_not_found_response.json"
        )
    }

    /**
     * Trying to remove shipment by id with GATE status
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/delete/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/delete/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteGateShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/shipments/2"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/delete/shipment_2_not_new_response.json"
        )
    }

    /**
     * Trying to remove shipment by id with SHIPPING status
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/delete/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/delete/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteShippingShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/shipments/3"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/delete/shipment_3_not_new_response.json"
        )
    }

    /**
     * Trying to remove shipment by id with SHIPPED status
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/delete/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/delete/before_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteShippedShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/shipments/4"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/delete/shipment_4_not_new_response.json"
        )
    }

    /**
     * Trying to remove shipment by id with NEW status
     */
    @Test
    @DatabaseSetup("/controller/shipping/shipments/delete/before_fill.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/delete/after_fill.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/shipments/1")
        )
    }
}
