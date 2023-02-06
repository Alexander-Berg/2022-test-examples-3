package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShipmentControllerCancelTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before_standard_new_no_details.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/after_standard.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel standard shipment in status new without dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 1 }")
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before_standard_gate_no_details.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/after_standard.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel standard shipment in status gate without dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 1 }")
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before_withdrawal_new_no_details.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/after_withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel withdrawal shipment in status new without dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 1 }")
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before_withdrawal_gate_no_details.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/after_withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel withdrawal shipment in status gate without dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 1 }")
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel shipment in status shipping`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 1 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_status_shipping.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel shipment in status shipped`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 2 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_status_shipped.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel shipment in status closed`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 3 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_status_closed.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel shipment in status cancelled`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 4 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_status_cancelled.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel standard shipment with dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 5 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_standard_with_details.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/shipments/cancel/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/shipments/cancel/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `cancel withdrawal shipment with dropIds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"shipmentId\" : 7 }"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/shipments/cancel/response_withdrawals_with_details.json"
        )
    }
}
