package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.shipping.HttpAssert

class LoadingControllerTest : IntegrationTest() {

    @MockBean(name="servicebusClient")
    @Autowired
    lateinit var servicebusClient: ServicebusClient

    private val httpAssert = HttpAssert { mockMvc }

    /** Loading standard drop into an empty vehicle */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-empty-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-empty-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard empty vehicle`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-empty-vehicle.json"
        )
    }

    /** Loading anomaly drop into an empty vehicle */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/anomaly-empty-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/anomaly-empty-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load anomaly empty vehicle`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/anomaly-empty-vehicle.json"
        )
    }

    /** Loading last standard drop into not empty vehicle */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/last-standard-not-empty-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/last-standard-not-empty-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load last standard not empty vehicle`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/last-standard-not-empty-vehicle.json"
        )
    }

    /** Loading standard drop from loc not near gate */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-unconnected-loc-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-unconnected-loc-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard from unconnected loc`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-unconnected-loc.json"
        )
    }

    /** Loading withdrawal drop into not empty vehicle with scheduledShipDate > today */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/withdrawal-shipdate-tomorrow-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/withdrawal-shipdate-tomorrow-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load withdrawal with tomorrow scheduled date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/withdrawal-shipdate-tomorrow.json"
        )
    }

    /** Loading second drop for one partially loaded withdrawal */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/withdrawal-second-drop-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/withdrawal-second-drop-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load second withdrawal drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/withdrawal-second-drop.json"
        )
    }

    /** Loading standard drop into not empty duty vehicle */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/duty-not-empty-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/duty-not-empty-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load into not empty duty vehicle`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/duty-not-empty-vehicle.json"
        )
    }

    /** Loading standard drop with scheduledShipDate > today under scheduledshipdate ignore flag*/
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-shipdate-tomorrow-under-flag-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-shipdate-tomorrow-under-flag-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard scheduled date tomorrow under flag`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-shipdate-tomorrow-under-flag.json"
        )
    }

    /** Loading standard drop with scheduledDepartureDateTime > current operational day */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-tomorrow-departure-date-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-tomorrow-departure-date-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard with tomorrow's scheduled date and scheduled departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-tomorrow-departure-date.json"
        )
    }

    /** Loading standard drop with scheduledDepartureDateTime in current operational day */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-today-departure-date-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-today-departure-date-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard with today's scheduled ship date and departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-today-departure-date.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/load-drop-with-splitted-orderdetail-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/load-drop-with-splitted-orderdetail-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load drop with one orderdetail on other drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/standard-splitted-orderdetail.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/load-drop/happy/db/load-withdrawal-parcel-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/load-withdrawal-parcel-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load not dropped withdrawal parcel`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-parcel.json",
            responseFile = "controller/shipping/load-drop/happy/response/withdrawal-parcel.json"
        )

    /** validate drop loaded - happy path */
    @Test
    @DatabaseSetup("/controller/shipping/validate-drop-loaded/valid_setup.xml")
    fun `validate drop loaded`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/validate-drop-loaded"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/validate-drop-loaded/validate-drop-loaded-request.json",
        )
    }

    /** get info standard empty */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-empty-vehicle.xml"
    )
    fun `get drops info std empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-empty-vehicle.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-with-two-shipment-dates-on-one-drop.xml"
    )
    fun `get drops info std with two shipment dates on one drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-with-two-shipment-dates-on-one-drop.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info standard half full */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-half-vehicle.xml"
    )
    fun `get drops info std half`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-half-vehicle.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info standard half full */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-half-vehicle-with-multi-carriers.xml"
    )
    fun `get drops info std half multi carriers`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-half-vehicle-with-multi-carriers.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info standard full */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-full-vehicle.xml"
    )
    fun `get drops info std full`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-full-vehicle.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info duty empty */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/duty-empty-vehicle.xml"
    )
    fun `get drops info duty empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/duty-empty-vehicle.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info duty full */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/duty-full-vehicle.xml"
    )
    fun `get drops info duty full`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/duty-full-vehicle.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info withdrawal empty */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/withdrawal-empty.xml"
    )
    fun `get drops info wth empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/withdrawal-empty.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info withdrawal full */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/withdrawal-full.xml"
    )
    fun `get drops info wth full`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/withdrawal-full.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    /** get info withdrawal empty multi-doors */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/withdrawal-empty-multi-doors.xml"
    )
    fun `get drops info wth empty multi doors`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/withdrawal-empty-multi-doors.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-shipdate-tomorrow.xml"
    )
    fun `get drops info std tomorrow`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-shipdate-tomorrow.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-shipdate-tomorrow-under-flag.xml"
    )
    fun `get drops info std tomorrow under flag`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-shipdate-tomorrow-under-flag.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-half-shipped.xml"
    )
    fun getDropsStandardWithShipped() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-half-shipped-with-time.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-tomorrow-departure-date.xml"
    )
    fun `get standard drops info with tomorrow's scheduled ship date and departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-tomorrow-departure-date.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-today-departure-date.xml"
    )
    fun `get standard drops info with today's scheduled ship date and departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-today-departure-date.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-half-shipped.xml"
    )
    fun `get drops info by shipmentId for standard shipment in GATE status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments/1/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-half-shipped.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/withdrawal-by-shipping-shipment.xml"
    )
    fun `get drops info by shipmentId for withdrawal shipment in SHIPPING status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments/1/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/withdrawal-by-shipping-shipment.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/duty-by-shipped-shipment.xml"
    )
    fun `get drops info by shipmentId for duty shipment in SHIPPED status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments/1/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/duty-by-shipped-shipment.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/happy/standard-by-closed-shipment.xml"
    )
    fun `get drops info by shipmentId for standard shipment in CLOSED status`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/shipments/1/drop-info"),
            MockMvcResultMatchers.status().isOk,
            responseFile = "controller/shipping/drops-info/happy/standard-by-closed-shipment.json",
            compareMode = JSONCompareMode.STRICT,
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/standard-empty-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/standard-empty-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop std empty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/standard-empty-vehicle.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/standard-half-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/standard-half-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop std half`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/standard-half-vehicle.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/standard-full-vehicle-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/standard-full-vehicle-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop std full`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/standard-full-vehicle.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/withdrawal-full-vehicle.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/withdrawal-empty-vehicle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop wth`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop-withdrawal.json",
            responseFile = "controller/shipping/unload-drop/happy/withdrawal-empty.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/anomaly-withdrawal-full-vehicle.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/anomaly-withdrawal-empty-vehicle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop_anomaly wth`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop-withdrawal.json",
            responseFile = "controller/shipping/unload-drop/happy/withdrawal-empty.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/withdrawal-two-drops-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/withdrawal-two-drops-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload one out of two withdrawal drops`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop-withdrawal.json",
            responseFile = "controller/shipping/unload-drop/happy/withdrawal-two-drops.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/withdrawal-full-vehicle.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/withdrawal-empty-vehicle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop wth to buf`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop-withdrawal.json",
            responseFile = "controller/shipping/unload-drop/happy/withdrawal-empty.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/duty-full-vehicle.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/duty-empty-vehicle.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload drop duty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isOk,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/duty-empty-vehicle.json",
        )
    }

    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-part-shipped-without-validation-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-part-shipped-without-validation-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard partially shipped drop without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/single-loaded-drop.json"
        )
    }

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/happy/db/standard-missing-pickdetails-without-validation-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/happy/db/standard-missing-pickdetails-without-validation-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard with missing pickdetails without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            requestFile = "controller/shipping/load-drop/happy/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/happy/response/single-loaded-drop.json"
        )
    }

    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/standard-part-shipped-without-validation-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/standard-part-shipped-without-validation-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload standard partially shipped drop without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/single-unloaded-drop.json"
        )
    }

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/happy/standard-missing-pickdetails-without-validation-before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/unload-drop/happy/standard-missing-pickdetails-without-validation-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unload standard with missing pickdetails without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/happy/single-unloaded-drop.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/standard-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/standard-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay standard`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0001&door=DOOR_A"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-delay-response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/standard-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/standard-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay standard by drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0001"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-delay-response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/withdrawal-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/withdrawal-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay withdrawal`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0002&door=DOOR_A"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-withdrawal-delay-response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/withdrawal-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/withdrawal-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay withdrawal by drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0002"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-withdrawal-delay-response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/duty-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/duty-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay duty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0001&door=DOOR_A"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-delay-response.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drop-load-delay/duty-load-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/drop-load-delay/duty-load-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `get drop load delay duty by drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drop-load-delay?&dropId=DRP0001"),
            responseFile = "controller/shipping/drop-load-delay/load-drop-delay-response.json"
        )
    }
}
