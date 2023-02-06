package ru.yandex.market.wms.core.controller.inventoryhold;

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
class InventoryHoldByLotTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/simple/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/simple/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `hold by lot with lot, lotxlocxid, holdtrn updates`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/simple/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/simple/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/simple/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `hold same lot twice`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/simple/request.json"
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/simple/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/different-code/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/different-code/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `hold lot with different code`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/different-code/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/simple/before/inventory-hold.xml")
    fun `should throw exception if trying to hold with non-exists code`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/inventory-hold/add-hold/lot/forbidden-codes/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/lot-picked/before/inventory-hold.xml")
    fun `should throw exception if lot already picked`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/inventory-hold/add-hold/lot/lot-picked/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/held-loc/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/held-loc/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `should hold lot, that locate on held location`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/hold-by-lot"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/held-loc/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/lot/one-hold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/lot/one-hold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `unhold one lot`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/one-hold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/lot/two-hold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/lot/two-hold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `unhold two lots`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/two-hold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/lot/one-hold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/lot/one-hold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `unhold same lot, twice, result as unhold once`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/one-hold/request.json"
        )
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/one-hold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/lot/not-on-hold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/lot/not-on-hold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `unhold lot that not on hold, nothing change`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/not-on-hold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/remove-hold/lot/one-hold-except-loc/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/remove-hold/lot/one-hold-except-loc/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `unhold lot, remain loc holds`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/remove-hold/lot/one-hold-except-loc/request.json"
        )
    }

    @Test
    fun `fail if lot and loc filled`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/unhold"),
            status = MockMvcResultMatchers.status().isBadRequest,
            requestFile = "controller/inventory-hold/remove-hold/lot/fail-if-both-filled/request.json"
        )
    }

    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/hold-lot-with-inventoryhold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/hold-lot-with-inventoryhold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `set hold on lot that has record in inventoryhold`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/hold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/hold-lot-with-inventoryhold/request.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/inventory-hold/add-hold/lot/hold-lot-and-loc-with-inventoryhold/before/inventory-hold.xml")
    @ExpectedDatabase("/controller/inventory-hold/add-hold/lot/hold-lot-and-loc-with-inventoryhold/expected/inventory-hold-extended.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `set hold on lot that has record in 2`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/hold"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold/add-hold/lot/hold-lot-and-loc-with-inventoryhold/request.json"
        )
    }
}
