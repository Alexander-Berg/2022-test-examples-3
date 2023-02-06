package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.shipping.HttpAssert

class ShippingControllerShipOrdersTest : IntegrationTest() {
    @MockBean(name="servicebusClient")
    @Autowired
    lateinit var servicebusClient: ServicebusClient

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-packed-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-packed-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship one packed order`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-picked-withdrawal-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-picked-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship one picked withdrawal`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-part-dropped-withdrawal-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-part-dropped-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship partially dropped withdrawal`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-two-orders-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-two-orders-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship two orders`() = assertShipOrders("ship-two-orders.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-part-shipped-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-part-shipped-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with partially shipped orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-shipped-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-shipped-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with fully shipped orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-part-adjusted-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-part-adjusted-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with partially adjusted orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-adjusted-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-adjusted-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with fully adjusted orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-part-cancelled-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-part-cancelled-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with partially cancelled orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-cancelled-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-cancelled-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with fully cancelled orderDetails`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-zero-openqty-order-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-zero-openqty-order-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order and orderDetail with zero openQty`() = assertShipOrders("ship-one-order.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-orders-common-drop-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-orders-common-drop-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship orders with common pallet dropId`() = assertShipOrders("ship-two-orders.json")

    @Test
    @DatabaseSetup("/controller/shipping/ship-orders/happy/db/ship-order-with-another-order-on-drop-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-orders/happy/db/ship-order-with-another-order-on-drop-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship order with more qty for the same items`() = assertShipOrders("ship-one-order.json")

    private fun assertShipOrders(requestFile: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-orders"),
            requestFile = "controller/shipping/ship-orders/request/$requestFile"
        )

        Mockito.verify(servicebusClient, Mockito.never())
            .pushCarrierState(Mockito.any())
    }
}
