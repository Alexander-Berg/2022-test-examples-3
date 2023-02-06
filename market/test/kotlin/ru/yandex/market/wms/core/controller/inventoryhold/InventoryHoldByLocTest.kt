package ru.yandex.market.wms.core.controller.inventoryhold

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert

@DatabaseSetups(
    DatabaseSetup("/controller/inventory-hold/inventory-hold-codes.xml"),
    DatabaseSetup("/controller/inventory-hold/add-hold/inventory-hold-empty.xml")
)
class InventoryHoldByLocTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/simple/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/simple/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `hold loc`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/simple/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/simple/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/simple/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `if hold by loc twice, same result as hold once`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/simple/request.json"
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/simple/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/different-codes/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/different-codes/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `hold loc with different codes`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/different-codes/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/add-two-codes-sequentially/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/add-two-codes-sequentially/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `add to different hold codes sequentially`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/add-two-codes-sequentially/request-one.json"
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/add-two-codes-sequentially/request-two.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/lot-on-hold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/lot-on-hold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `should put on hold only location if lot already on hold`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/lot-on-hold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/hold-without-lotxloc/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/hold-without-lotxloc/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `should put on hold location without lotxlocxid`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/hold-without-lotxloc/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/loc/one-loc/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/loc/one-loc/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold one location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/loc/one-loc/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/loc/two-locs/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/loc/two-locs/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold two locations`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/loc/two-locs/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/loc/one-loc-with-two-lots/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/loc/one-loc-with-two-lots/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold one locations with two lots`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/loc/one-loc-with-two-lots/request.json"
        )
    }


    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/loc/one-loc-two-holds/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/loc/one-loc-two-holds/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold one of two holds by locations`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/loc/one-loc-two-holds/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/loc/one-loc-except-by-lot/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/loc/one-loc-except-by-lot/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold location, remain hold by lot`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/loc/one-loc-except-by-lot/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/loc/simple/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/loc/simple/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `set hold on loc that has record in inventoryhold`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-loc"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/loc/simple/request.json"
        )
    }
}