package ru.yandex.market.wms.shipping.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.shipping.HttpAssert

class LoadingControllerErrorTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    /** Loading standard drop with scheduledShipDate > today */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-shipdate-tomorrow.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-shipdate-tomorrow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardScheduledDateTomorrow() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-shipdate-tomorrow.json"
        )
    }

    /** Loading standard drop with scheduledShipDate > today and today's departure date */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-shipdate-tomorrow-departure-today.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-shipdate-tomorrow-departure-today.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load standard drop with future scheduled ship date and today's departure date`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-shipdate-tomorrow.json"
        )
    }


    /** Loading standard drop to withdrawal shipment */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-to-withdrawal.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-to-withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardToWithdrawal() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-to-withdrawal.json"
        )
    }

    /** Loading standard drop to shipment with non matching carrierCode */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-not-matching-carrier.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-not-matching-carrier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardNotMatchingCarrierCode() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-not-matching-carrier.json"
        )
    }

    /** Loading standard drop to shipment in SHIPPING status */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-shipment-shipping.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-shipment-shipping.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardShipmentShipping() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-shipment-shipping.json"
        )
    }

    /** Loading standard drop that is already loaded in another shipment */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-drop-loaded.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-drop-loaded.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardDropLoaded() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-drop-loaded.json"
        )
    }

    /** Loading standard drop with one withdrawal order in it */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-with-withdrawal.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-with-withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardWithWithdrawal() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-with-withdrawal.json"
        )
    }

    /** Loading standard drop with one withdrawal order in it */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/errors/db/withdrawal-part-picked.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/withdrawal-part-picked.xml",
        assertionMode    = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadSWithdrawalPartPicked() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/withdrawal-part-picked.json"
        )
    }

    /** Loading entity that is not drop */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-not-drop.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-not-drop.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardNotDrop() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-not-drop.json"
        )
    }

    /** Loading withdrawal drop with withdrawalId not from current shipment */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/withdrawal-id-not-in-shipment.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/withdrawal-id-not-in-shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadWithdrawalNotFromShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/withdrawal-id-not-in-shipment.json"
        )
    }

    /** Loading withdrawal drop to standard shipment */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/withdrawal-to-standard.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/withdrawal-to-standard.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadWithdrawalToStandardShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/withdrawal-to-other-type.json"
        )
    }

    /** Loading standard drop to duty shipment with scheduledShipDate > today */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-to-duty-shipdate-tomorrow.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/standard-to-duty-shipdate-tomorrow.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadStandardToDutyShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-shipdate-tomorrow.json"
        )
    }

    /** Loading withdrawal drop to duty shipment */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/withdrawal-to-duty.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/withdrawal-to-duty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun loadWithdrawalToDutyShipment() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/withdrawal-to-other-type.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/drop-already-shipped.xml"
    )
    fun `loading drop that was already shipped`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/drop-already-shipped.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/orderdetail_pickdetail_inconsistency.xml"
    )
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/orderdetail_pickdetail_inconsistency.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load drop with orderdetail qty less than pickdetail qty`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/orderdetail_pickdetail_inconsistency.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/load-drop/errors/db/load-not-packed-withdrawal.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/load-not-packed-withdrawal.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load not packed withdrawal`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-parcel.json",
            responseFile = "controller/shipping/load-drop/errors/response/not-packed-withdrawal.json"
        )

    @Test
    @DatabaseSetup("/controller/shipping/load-drop/errors/db/load-packed-order.xml")
    @ExpectedDatabase(
        value = "/controller/shipping/load-drop/errors/db/load-packed-order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `load not dropped standard order parcel`() =
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/load-drop/errors/request/load-parcel.json",
            responseFile = "controller/shipping/load-drop/errors/response/packed-order.json"
        )

    /** Loading standard drop from forbidden location with SHIPPING_ALLOW_LOAD_FROM_SHIPPING_LOCS flag on */
    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/load-drop/errors/db/standard-invalid-location.xml"
    )
    fun `load standard drop from invalid loc with flag on`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/load-drop"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/shipping/load-drop/errors/request/load-drop.json",
            responseFile = "controller/shipping/load-drop/errors/response/standard-invalid-location.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/validate-drop-loaded/shipment_not_found_setup.xml")
    fun validateDropLoadedShipmentNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/validate-drop-loaded"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/validate-drop-loaded/validate-drop-loaded-request.json",
            responseFile = "controller/shipping/validate-drop-loaded/shipment-not-found-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/validate-drop-loaded/shipment_detail_not_found_setup.xml")
    fun validateDropLoadedShipmentDetailNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/validate-drop-loaded"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/validate-drop-loaded/validate-drop-loaded-request.json",
            responseFile = "controller/shipping/validate-drop-loaded/shipment-detail-not-found-response.json",
        )
    }

    @Test
    @DatabaseSetup("/controller/shipping/validate-drop-loaded/pickdetail_wrong_status_setup.xml")
    fun validateDropLoadedPickdetailWrongStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/validate-drop-loaded"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/validate-drop-loaded/validate-drop-loaded-request.json",
            responseFile = "controller/shipping/validate-drop-loaded/pickdetail-wrong-status-response.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/errors/shipment-not-found.xml"
    )
    fun getDropsInfoShipmentNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/drops-info/errors/shipment-not-found.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/load-drop/common.xml",
        "/controller/shipping/drops-info/errors/multi-shipment-found.xml"
    )
    fun getDropsInfoMultiShipmentFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/doors/DOOR_A/drop-info"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            responseFile = "controller/shipping/drops-info/errors/multi-shipment-found.json"
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/error/shipment-detail-not-found.xml"
    )
    fun unloadDropDetailNotFound() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/error/shipment-detail-not-found.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/error/wrong-balances.xml"
    )
    fun unloadDropWrongBalances() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/error/wrong-balances.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/error/wrong-pickdetail-status.xml"
    )
    fun unloadDropWrongPickdetailStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/error/wrong-pickdetail-status.json",
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/shipping/unload-drop/common.xml",
        "/controller/shipping/unload-drop/error/shipped-pickdetail-status.xml"
    )
    fun unloadDropShippedPickdetailStatus() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/unload-drop"),
            MockMvcResultMatchers.status().isUnprocessableEntity,
            requestFile = "controller/shipping/unload-drop/unload-drop.json",
            responseFile = "controller/shipping/unload-drop/error/shipped-pickdetail-status.json",
        )
    }
}
