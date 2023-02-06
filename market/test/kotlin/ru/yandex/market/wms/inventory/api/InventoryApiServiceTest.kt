package ru.yandex.market.wms.inventory.api

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.Test

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryApiServiceTest(
    private val jdbcTemplate: JdbcTemplate
) : AbstractApiTest() {
    @BeforeEach
    fun setUp() = jdbcTemplate.execute("alter sequence item_id_seq restart with 1;")

    //1) QtyExpected != QtyInvented -> + update Log + has_discrepancies=true
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-qty/has-discrepancies/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-qty/has-discrepancies/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun skuByQtyHasDiscrepancies() {
        assertApiCall(
            "json/api/inventory/by-qty/has-discrepancies/request.json",
            "json/api/inventory/by-qty/has-discrepancies/response.json",
            MockMvcRequestBuilders.put("/inventory/by-qty"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //2) QtyExpected == QtyInvented update log
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-qty/success/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-qty/success/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun skuByQtySuccess() {
        assertApiCall(
            "json/api/inventory/by-qty/success/request.json",
            "json/api/inventory/by-qty/success/response.json",
            MockMvcRequestBuilders.put("/inventory/by-qty"),
            MockMvcResultMatchers.status().isOk, JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // log.qtyInvented!=0 -> Exception sku already invented
    @Test
    @DatabaseSetup("/json/api/inventory/by-qty/already-invented/before.xml")
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-qty/already-invented/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun alreadyInventedInventoryByQty() {
        assertApiCallClientError(
            requestFile = "json/api/inventory/by-qty/already-invented/request.json",
            request = MockMvcRequestBuilders.put("/inventory/by-qty"),
            errorInfo = "SKU_ALREADY_INVENTED"
        )
    }

    // task in terminal status exception
    @Test
    @DatabaseSetup("/json/api/inventory/by-qty/status-finish/before.xml")
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-qty/status-finish/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun taskInIllegalStatus() {
        assertApiCallClientError(
            requestFile = "/json/api/inventory/by-qty/status-finish/request.json",
            request = MockMvcRequestBuilders.put("/inventory/by-qty"),
            errorInfo = "ILLEGAL_STATUS_EXCEPTION"
        )
    }

    // has discrepancies for sku
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/discrepancies/true/before.xml"),
    )
    fun getDiscrepanciesForSkuTrue() {

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/inventory/discrepancies/123/1")
                .contentType(MediaType.APPLICATION_JSON)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk).andExpect(
            MockMvcResultMatchers.content().json(
                resourceAsString(
                    "/json/api/inventory/discrepancies/true/response.json"
                )
            )
        )
    }

    // has discrepancies for sku, but in a big way -> discrepancies = false
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/discrepancies/false/before.xml"),
    )
    fun getDiscrepanciesForSkuFalse() {

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/inventory/discrepancies/123/1")
                .contentType(MediaType.APPLICATION_JSON)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk).andExpect(
            MockMvcResultMatchers.content().json(
                resourceAsString(
                    "/json/api/inventory/discrepancies/false/response.json"
                )
            )
        )
    }

    // * invent by identity for sku * //
    //1) identity do not exist -> exception
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-sku/identity-not-exist/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-sku/identity-not-exist/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun identityDoNotExist() {
        assertApiCallClientError(
            requestFile = "json/api/inventory/by-identity/by-sku/identity-not-exist/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "ITEM_NOT_FOUND"
        )
    }

    //2) identity from another sku ->faliure
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-sku/another-sku/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-sku/another-sku/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun identityFromAnotherSku() {
        assertApiCallClientError(
            requestFile = "json/api/inventory/by-identity/by-sku/another-sku/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "ITEM_FROM_ANOTHER_SKU"
        )
    }

    //3) identity success - insert log
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-sku/success-new-log/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-sku/success-new-log/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun identityForSkuSuccessCreateLog() {
        assertApiCall(
            requestFile = "/json/api/inventory/by-identity/by-sku/success-new-log/request.json",
            responseFile = null,
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //4) identity success - update log
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-sku/success-update-log/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-sku/success-update-log/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun identityForSkuSuccessUpdateLog() {
        assertApiCall(
            requestFile = "/json/api/inventory/by-identity/by-sku/success-update-log/request.json",
            responseFile = null,
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    // * invent by item * //
    // one identity value for different types
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/duplicate-identities/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/duplicate-identities/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun duplicateIdentity() {
        assertApiCallClientError(
            requestFile = "/json/api/inventory/by-identity/by-item/duplicate-identities/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "ITEM_NOT_FOUND_UNIQUELY"
        )
    }

    // scanned ean
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/illegal-identity/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/illegal-identity/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun illegalIdentityTypeScanned() {
        assertApiCallClientError(
            requestFile = "/json/api/inventory/by-identity/by-item/illegal-identity/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "IDENTITY_SCANNED_ILLEGAL"
        )
    }

    // already invented with this identity
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/already-invented-by-identity/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/already-invented-by-identity/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun alreadyInventedByIdentity() {
        assertApiCallClientError(
            requestFile = "/json/api/inventory/by-identity/by-item/already-invented-by-identity/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "ITEM_ALREADY_INVENTED"
        )
    }

    // already invented item by another identity
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/already-invented-item/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/already-invented-item/before.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun alreadyInventedItem() {
        assertApiCallClientError(
            requestFile = "/json/api/inventory/by-identity/by-item/already-invented-item/request.json",
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            errorInfo = "ITEM_ALREADY_INVENTED"
        )
    }

    // already invented item by ean -> invent this item by identity - success
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/already-invented-by-ean/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/already-invented-by-ean/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun alreadyInventedByEan() {

        assertApiCall(
            requestFile = "/json/api/inventory/by-identity/by-item/already-invented-by-ean/request.json",
            responseFile = null,
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //qty_invented = 2; qty_expected=2
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/has-discrepancies-n-false/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/has-discrepancies-n-false/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun hasDiscrepanciesFalseQtyExpectedN() {

        assertApiCall(
            requestFile = "/json/api/inventory/by-identity/by-item/has-discrepancies-n-false/request.json",
            responseFile = null,
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //qty_invented = 1; qty_expected=1
    @Test
    @DatabaseSetups(
        DatabaseSetup("/json/api/inventory/by-identity/by-item/has-discrepancies-false/before.xml"),
    )
    @ExpectedDatabase(
        value = ("/json/api/inventory/by-identity/by-item/has-discrepancies-false/after.xml"),
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun hasDiscrepanciesFalseQtyExpectedOne() {

        assertApiCall(
            requestFile = "/json/api/inventory/by-identity/by-item/has-discrepancies-false/request.json",
            responseFile = null,
            request = MockMvcRequestBuilders.post("/inventory/by-identity"),
            status = MockMvcResultMatchers.status().isOk,
            mode = JSONCompareMode.NON_EXTENSIBLE
        )
    }

    //get progress bar info
    // start task (nothing invented)
    @Test
    @DatabaseSetup("/json/api/inventory/progress-bar/expected/before.xml")
    fun getExpectedLogs() {

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/inventory/progress-bar/123")
                .contentType(MediaType.APPLICATION_JSON)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk).andExpect(
            MockMvcResultMatchers.content().json(
                resourceAsString(
                    "/json/api/inventory/progress-bar/expected/response.json"
                )
            )
        )
    }

    // invented 1 sku + 1 unknown
    @Test
    @DatabaseSetup("/json/api/inventory/progress-bar/expected-and-invented/before.xml")
    fun getExpectedAndInventedLogs() {

        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("/inventory/progress-bar/123")
                .contentType(MediaType.APPLICATION_JSON)
        )

        result.andExpect(MockMvcResultMatchers.status().isOk).andExpect(
            MockMvcResultMatchers.content().json(
                resourceAsString(
                    "/json/api/inventory/progress-bar/expected-and-invented/response.json"
                )
            )
        )
    }
}
