package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShippingControllerShipVehicleErrorTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }


    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/drop_id_not_found.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/errors/db/drop_id_not_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `dropId not found`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/drop_id_not_found.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/wrong_drop_id_format.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/errors/db/wrong_drop_id_format.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `wrong shipId format`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/wrong_ship_id_format.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/no_shipping_items.xml")
    fun `no shippingItems`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/no_shipping_items.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/parent_drop_id_shipped.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/errors/db/parent_drop_id_shipped.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `parent dropId shipped`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/parent_drop_id_shipped.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/child_drop_id_shipped.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/errors/db/child_drop_id_shipped.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `child drop shipped`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/child_drop_id_shipped.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/lotxiddetail_serialinventory_inconsistency.xml")
    fun `lotXIdDetail and serialInventory inconsistency`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/lotxiddetail_serialinventory_inconsistency.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/lotxlocxid_serialinventory_inconsistency.xml")
    fun `lotXLocXId and serialInventory inconsistency`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/lotxlocxid_serialinventory_inconsistency.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/shipped_pickdetails.xml")
    fun `partially shipped pickDetails`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/shipped_pickdetails.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/batch_orders.xml")
    fun `ship batch orders`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/batch_orders.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/part_loaded_withdrawal.xml")
    fun `ship partially loaded withdrawal`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/part_loaded_withdrawal.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/withdrawal_loaded_at_two_doors.xml")
    fun `ship withdrawal loaded in two different vehicles`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/withdrawal_loaded_at_two_doors.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/ship-empty-vehicle.xml")
    fun `ship vehicle with no drops loaded`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/ship_empty_vehicle.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/errors/db/orderdetail_pickdetail_inconsistency.xml")
    fun `ship vehicle with orderDetail qty less than pickDetail qty`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-vehicle/errors/request/ship-vehicle.json",
            responseFile = "controller/shipping/ship-vehicle/errors/response/" +
                "orderdetail_pickdetail_inconsistency.json"
        )
}
