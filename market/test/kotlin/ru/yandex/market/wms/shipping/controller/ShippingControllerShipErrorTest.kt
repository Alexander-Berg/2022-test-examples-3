package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShippingControllerShipErrorTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }


    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/db.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/errors/db/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun dropIdNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/drop_id_not_found.json",
            responseFile = "controller/shipping/ship/errors/response/drop_id_not_found.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/db.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/errors/db/db.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun wrongShipIdFormat() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/ship/errors/request/wrong_ship_id_format.json",
            responseFile = "controller/shipping/ship/errors/response/wrong_ship_id_format.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/no_shipping_items.xml")
    fun noShippingItems() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/no_shipping_items.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/parent_drop_id_shipped.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/errors/db/parent_drop_id_shipped.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun parentDropIdShipped() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/parent_drop_id_shipped.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/child_drop_id_shipped.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/errors/db/child_drop_id_shipped.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun childDropIdShipped() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/child_drop_id_shipped.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/lotxiddetail_serialinventory_inconsistency.xml")
    fun lotXIdDetailAndSerialInventoryInconsistency() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/lotxiddetail_serialinventory_inconsistency.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/lotxlocxid_serialinventory_inconsistency.xml")
    fun lotXLocXIdAndSerialInventoryInconsistency() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/lotxlocxid_serialinventory_inconsistency.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/id_in_multiple_locs.xml")
    fun shipIdInMultipleLocs() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/id_in_multiple_locs.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/loc_not_found.xml")
    fun shipIdLocNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/loc_not_found.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/shipped_pickdetails.xml")
    fun shippedPickDetails() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/shipped_pickdetails.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/batch_orders.xml")
    fun shipBatchOrders() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/batch_orders.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/errors/db/pickdetail_orderdetail_inconsistency.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/errors/db/pickdetail_orderdetail_inconsistency.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `pickdetail qty greater than orderdetail qty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship/errors/request/ship.json",
            responseFile = "controller/shipping/ship/errors/response/pickdetail_orderdetail_inconsistency.json"
        )
    }
}
