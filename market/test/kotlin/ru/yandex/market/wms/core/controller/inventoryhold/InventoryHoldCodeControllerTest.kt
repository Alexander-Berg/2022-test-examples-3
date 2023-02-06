package ru.yandex.market.wms.core.controller.inventoryhold

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert
import java.util.stream.Stream

@DatabaseSetup("/controller/inventory-hold-code/inventory-hold-code.xml")
class InventoryHoldCodeControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/add/smoke/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Add new hold code`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/add/smoke/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/add/expiring/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Add new hold code with expiring`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/add/expiring/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/add/damaged/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Add new hold code with damaged`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/add/damaged/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/add/exclusive/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Add new hold code with exclusive`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/add/exclusive/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/add/duplicate/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Should fail if add exists code`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.post("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isConflict,
            requestFile = "controller/inventory-hold-code/add/duplicate/request.json"
        )
    }

    companion object {
        @JvmStatic
        fun argumentProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("sort=code", "sorted-by-code"),
                Arguments.of("limit=1&offset=1", "offset"),
                Arguments.of("limit=1", "limit"),
                Arguments.of("order=desc", "reversed-order"),
                Arguments.of("", "no-filter"),
            )
        }

        @JvmStatic
        fun filterProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("code", "PIHOLD"),
                Arguments.of("isdamage", "true"),
                Arguments.of("description", "OK"),
                Arguments.of("isexclusive", "true"),
                Arguments.of("isexpiring", "true"),
                Arguments.of("rank", "0"),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    @DatabaseSetup("/controller/inventory-hold-code/inventory-hold-code.xml")
    fun `Get inventory hold with argument query`(filter: String, directory: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/inventory-hold/codes?$filter"),
            responseFile = "controller/inventory-hold-code/base/$directory/response.json"
        )
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    @DatabaseSetup("/controller/inventory-hold-code/filter/inventory-hold-code.xml")
    fun `Get inventory hold list filter with filter`(filter: String, value: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/inventory-hold/codes?filter=$filter==$value"),
            responseFile = "controller/inventory-hold-code/filter/$filter/response.json")
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/remove/smoke/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Remove exists hold code`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/inventory-hold/code/OK"),
            status = MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Remove absent hold code should not fail`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.delete("/inventory-hold/code/UNKNOWN"),
            status = MockMvcResultMatchers.status().isOk
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/update/description/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Update existing description`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/update/description/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/update/everything/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Update everything`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/update/everything/request.json"
        )
    }

    @Test
    @ExpectedDatabase("/controller/inventory-hold-code/update/rank/expected/inventory-hold-code.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Update existing rank`() {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.put("/inventory-hold/code"),
            status = MockMvcResultMatchers.status().isOk,
            requestFile = "controller/inventory-hold-code/update/rank/request.json"
        )
    }
}
