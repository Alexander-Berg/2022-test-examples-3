package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.common.spring.servicebus.model.request.PushCarrierStateRequest
import ru.yandex.market.wms.core.base.response.GetLostInventoriesResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.shipping.HttpAssert
import ru.yandex.market.wms.shipping.service.VehicleShippingAsyncService

class ShippingControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @MockBean
    @Autowired
    lateinit var defaultJmsTemplate: JmsTemplate

    @Autowired
    lateinit var vehicleShippingAsyncService: VehicleShippingAsyncService

    @MockBean(name="servicebusClient")
    @Autowired
    lateinit var servicebusClient: ServicebusClient

    @Autowired
    @MockBean
    lateinit var coreClient: CoreClient

    @BeforeEach
    fun setUp() {
        Mockito.reset(defaultJmsTemplate)
        MockitoAnnotations.openMocks(this)
    }

    /**
     * Shipping drop with multiple orders and multiple dropIds
     */
    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/ship-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop happy`() {
        Mockito.`when`(coreClient.getLostSerials("0000012345"))
            .thenReturn(GetLostInventoriesResponse(emptyList()))
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
        verify(servicebusClient, Mockito.atLeastOnce())
            .pushCarrierState(PushCarrierStateRequest.builder().carrierCode("CARRIER-01").build())
    }

    /**
     * Shipping drop with multiple orders and multiple dropIds
     */
    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/ship-before-short.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-after-short.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop and short order happy`() {
        Mockito.`when`(coreClient.getLostSerials("0000012345"))
            .thenReturn(GetLostInventoriesResponse(emptyList()))

        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
        verify(servicebusClient, Mockito.atLeastOnce())
            .pushCarrierState(PushCarrierStateRequest.builder().carrierCode("CARRIER-01").build())
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/drop_without_child_drops_before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/drop_without_child_drops_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop without child drops`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json",
        )
    }

    /**
     * Shipping withdrawal drop with hold
     */
    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/ship-withdrawal-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-withdrawal-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship withdrawal with hold`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/ship-with-splitted-orderdetail-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-with-splitted-orderdetail-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop with one orderdetail on other drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
    }

    /** MARKETWMS-12350 */
    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/ship-one-pickdetail-three-lotxiddetails-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-one-pickdetail-three-lotxiddetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop with one pickdetail line and multiple lotxiddetail lines`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/ship/happy/db/drop_id_is_its_own_parent_before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/drop_id_is_its_own_parent_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop that is its own parent`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/errors/request/ship.json"
        )
    }



    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle happy`() = assertShipVehicleHappyRequest()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-part-shipped-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-part-shipped-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship partially shipped vehicle`() = assertShipVehicleHappyRequest()


    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/ship-part-shipped-without-validation-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-part-shipped-without-validation-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship partially shipped drop with validation off`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
    }

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/ship-with-missing-pickdetails-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship/happy/db/ship-with-missing-pickdetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship drop with missing pickDetails without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship"),
            requestFile = "controller/shipping/ship/happy/request.json"
        )
    }

    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-part-shipped-drop-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-part-shipped-drop-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle with partially shipped drop without validation`() = assertShipVehicleHappyRequest()

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-missing-pickdetails-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-missing-pickdetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle with missing pickDetails without validation`() = assertShipVehicleHappyRequest()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-withdrawal-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-withdrawal-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship withdrawal vehicle happy`() = assertShipVehicleHappyRequest()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-with-splitted-orderdetail-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-with-splitted-orderdetail-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle with orderdetail splitted between drops`() = assertShipVehicleHappyRequest()

    @Test
    @DatabaseSetup(
        "/controller/shipping/ship/happy/db/common.xml",
        "/controller/shipping/ship-vehicle/happy/db/preship-vehicle-one-pickdetail-three-lotxiddetails-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/" +
            "preship-vehicle-one-pickdetail-three-lotxiddetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle with one pickdetail line and multiple lotxiddetail lines`() = assertShipVehicleHappyRequest()


    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/happy/db/drop_id_is_its_own_parent_before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/drop_id_is_its_own_parent_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `preship vehicle with drop that its own parent`() = assertShipVehicleHappyRequest()

    @Test
    @DatabaseSetup("/controller/shipping/ship-vehicle/happy/db/part_loaded_withdrawal_before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/ship-vehicle/happy/db/part_loaded_withdrawal_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `ship partially loaded withdrawal with flag enabled`() = assertShipVehicleHappyRequest()


    private fun assertShipVehicleHappyRequest() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/ship-vehicle"),
            requestFile = "controller/shipping/ship-vehicle/happy/request.json"
        )
        verify(defaultJmsTemplate, times(1))
            .convertAndSend(
                anyString(),
                eq(VehicleShippingAsyncService.ShipVehicleAsyncRequest("DOOR_A", "TEST")),
                any()
            )
    }

}
