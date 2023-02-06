package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.shipping.HttpAssert

class DropLocationControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @MockBean(name="servicebusClient")
    @Autowired
    lateinit var servicebusClient: ServicebusClient

    /**
     * Get locations (standard)
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0001-before.xml")
    fun `get locations standard`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0001/locations"),
            responseFile = "controller/shipping/locations/DRP0001-response.json"
        )
    }

    /**
     * Get locations (current - buffer)
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/locations-before-in-buf.xml")
    fun `get locations standard from buffer`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0001/locations"),
            responseFile = "controller/shipping/locations/locations-in-buf-response.json"
        )
    }


    /**
     * Get locations (withdrawal)
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0002-before.xml")
    fun `get locations withdrawals`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0002/locations"),
            responseFile = "controller/shipping/locations/DRP0002-response.json"
        )
    }

    /**
     * Get locations (withdrawal, current - buffer)
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/locations-withdrawal-before-in-buf.xml")
    fun `get locations withdrawals from buffer`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0002/locations"),
            responseFile = "controller/shipping/locations/locations-withdrawal-in-buf.json"
        )
    }

    /**
     * Get locations (standard) no door to carrier connections found
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0003-before.xml")
    fun `get locations standard no door to carrier connections`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0003/locations"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/locations/DRP0003-response.json"
        )
    }

    /**
     * Get not found locations (standard)
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0006-before.xml")
    fun `get locations standard location not found`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0006/locations"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/locations/DRP0006-response.json"
        )
    }

    /**
     * Get locations (standard) multiple door to carrier connections found
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0004-before.xml")
    fun `get locations standard multiple door to carrier connections`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0004/locations"),
            responseFile = "controller/shipping/locations/DRP0004-response.json"
        )
    }

    /**
     * Get locations (standard) STD_1 filtered out because of hold
     */
    @Test
    @DatabaseSetup("/controller/shipping/locations/DRP0005-before.xml")
    fun `get locations standard lock with hold`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/drops/DRP0005/locations"),
            responseFile = "controller/shipping/locations/DRP0005-response.json"
        )
    }

    /**
     * Move drop (standard)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/happy-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request.json"
        )
    }

    /**
     * Move drop (standard) to buffer (success even when occupied)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/happy-to-buffer-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard to buffer`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-to-buffer-request.json"
        )
    }

    /**
     * Trying to move standard drop to withdrawal cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard to wrong type location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/wrong-loc-type-request.json",
            responseFile = "controller/shipping/move-drop/standard/wrong-loc-type-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/drop-wrong-mask.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/drop-wrong-mask.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop with wrong mask`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/move-drop/standard/wrong-drop-mask-request.json",
            responseFile = "controller/shipping/move-drop/standard/wrong-drop-mask-response.json"
        )
    }

    /**
     * Trying to move standard drop to occupied cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard to occupied location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/occupied-loc-request.json",
            responseFile = "controller/shipping/move-drop/standard/occupied-loc-response.json"
        )
    }

    /**
     * Trying to move standard drop to current cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard to current location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/current-loc-request.json",
            responseFile = "controller/shipping/move-drop/standard/current-loc-response.json"
        )
    }

    /**
     * Trying to move standard drop to cell in wrong zone (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun moveDropStandardToLocInWrongZone() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/wrong-zone-loc-request.json",
            responseFile = "controller/shipping/move-drop/standard/wrong-zone-loc-response.json"
        )
    }

    /**
     * Move drop (withdrawal)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/happy-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop withdrawal`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/withdrawal/happy-request.json"
        )
    }

    /**
     * Trying to move withdrawal drop to withdrawal buffer cell
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/move-to-buf-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop withdrawal to buffer`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/withdrawal/move-to-buf-request.json",
        )

    }

    /**
     * Trying to move withdrawal drop to std buffer cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop withdrawal to wrong type location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/withdrawal/wrong-loc-type-request.json",
            responseFile = "controller/shipping/move-drop/withdrawal/wrong-loc-type-response.json"
        )
    }

    /**
     * Trying to move withdrawal drop to occupied cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop withdrawal to occupied location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/withdrawal/occupied-loc-request.json",
            responseFile = "controller/shipping/move-drop/withdrawal/occupied-loc-response.json"
        )
    }

    /**
     * Trying to move withdrawal drop to current cell (expecting no changes in database and error response)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop withdrawal to current location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/withdrawal/current-loc-request.json",
            responseFile = "controller/shipping/move-drop/withdrawal/current-loc-response.json"
        )
    }


    /** Тест на работу флага SHIPPING_STATUS_VALIDATION_OFF, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/part-shipped.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/part-shipped-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move standard partially shipped drop without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request.json"
        )
    }

    /** Тест на работу флага SHIPPING_WRITE_OFF_ALL_UITS, TODO удалить после удаления флага */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/missing-pickdetails.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/missing-pickdetails-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move standard with missing pickdetails without validation`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/move-loaded-drop-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/move-loaded-drop-before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move already loaded drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/move-loaded-drop-request.json",
            responseFile = "controller/shipping/move-drop/standard/move-loaded-drop-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/orderdetail_pickdetail_inconsistency.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/orderdetail_pickdetail_inconsistency.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move standard drop with orderdetail qty less than pickdetail qty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/happy-request.json",
            responseFile = "controller/shipping/move-drop/standard/" +
                "move-drop-orderdetail-pickdetail-inconsistency-response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/move-drop-with-splitted-orderdetail-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/move-drop-with-splitted-orderdetail-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop with one orderdetail on other drop`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request.json"
        )
    }

    /**
     * Move drop (standard)
     */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/move-drop-two-pickdetails-same-uit-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/move-drop-two-pickdetails-same-uit-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop with second pickdetail for same uit`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/move-drop-two-pickdetails-same-uit-request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/move-withdrawal-parcel-before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/move-withdrawal-parcel-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move not dropped withdrawal parcel`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/withdrawal/move-parcel-request.json"
        )

        Mockito.verify(servicebusClient, Mockito.atLeastOnce())
            .pushOutboundStatus(Mockito.any())
        Mockito.verify(servicebusClient, Mockito.atLeastOnce())
            .pushCarrierState(Mockito.any())
    }

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/withdrawal/move-not-packed-withdrawal.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/withdrawal/move-not-packed-withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move not packed withdrawal`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/withdrawal/move-parcel-request.json",
            responseFile = "controller/shipping/move-drop/withdrawal/move-not-packed-withdrawal-response.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/move-packed-order.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/move-packed-order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move packed order`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/move-drop/standard/move-parcel-request.json",
            responseFile = "controller/shipping/move-drop/standard/move-packed-order-response.json"
        )

    /** Move drop (standard) with type in request */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/happy-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard with type`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request.json"
        )
    }

    /** Move drop (standard) with inprocessable type in request */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard with wrong type`() = httpAssert.assertApiCall(
        MockMvcRequestBuilders.post("/move-drop"),
        MockMvcResultMatchers.status().isBadRequest,
        requestFile = "controller/shipping/move-drop/standard/wrong-move-type-request.json",
        responseFile = "controller/shipping/move-drop/standard/wrong-move-type-response.json",
    )

    /** Move drop (standard) with palletizer type in request */
    @Test
    @DatabaseSetup("/controller/shipping/move-drop/standard/before.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/move-drop/standard/palletizer-move-type-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `move drop standard with palletizer type`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/move-drop"),
            requestFile = "controller/shipping/move-drop/standard/happy-request-with-palletizer-type.json"
        )
    }


}
