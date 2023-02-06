package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class ShippingControllerShipOrdersErrorTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/missing_pickdetail.xml")
    fun `ship order with missing pickDetails`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/missing_pickdetail.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/missing_orderdetail.xml")
    fun `ship order with missing orderDetails`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/missing_orderdetail.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/qtypicked_less_than_openqty.xml")
    fun `ship order with qtyPicked less than openQty`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/qtypicked_less_than_openqty.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/batch_order.xml")
    fun `ship batch order`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/batch_order.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/not_packed_order.xml")
    fun `ship not packed order`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/not_packed_order.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/not_picked_withdrawal.xml")
    fun `ship not picked withdrawal`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/not_picked_withdrawal.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/shipped_order.xml")
    fun `ship shipped order`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/shipped_order.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/wrong_orderdetail_status.xml")
    fun `ship order with orderDetails in wrong status`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/wrong_orderdetail_status.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/empty_orderdetail_02_status.xml")
    fun `ship order with orderDetails in 02 status with 0 qty`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/empty_orderdetail_02_status.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/lotxlocxid_inconsistency.xml")
    fun `ship order with lotxlocxid and serialinventory inconsistency`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/lotxlocxid_inconsistency.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/lotxiddetail_inconsistency.xml")
    fun `ship order with lotxiddetail inconsistency`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isUnprocessableEntity,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/lotxiddetail_inconsistency.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/errors/db/order_not_found.xml")
    fun `ship non-existent order`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            status = status().isBadRequest,
            requestFile = "controller/shipping/ship-orders/request/ship-one-order.json",
            responseFile = "controller/shipping/ship-orders/errors/response/order_not_found.json"
        )
}
