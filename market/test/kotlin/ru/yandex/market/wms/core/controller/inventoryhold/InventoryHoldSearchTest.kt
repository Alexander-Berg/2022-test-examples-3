package ru.yandex.market.wms.core.controller.inventoryhold

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert
import java.util.stream.Stream

class InventoryHoldSearchTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    companion object {
        @JvmStatic
        fun argumentProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("sort=loc", "sorted-by-loc"),
                Arguments.of("limit=1&offset=1", "offset"),
                Arguments.of("limit=1", "limit"),
                Arguments.of("order=desc", "reversed-order"),
                Arguments.of("", "no-filter"),
            )
        }

        @JvmStatic
        fun filterProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("status", "OTHER"),
                Arguments.of("whooff", "AD9"),
                Arguments.of("whoon", "LocationFlag"),
                Arguments.of("id", "someid"),
                Arguments.of("loc", "1-08"),
                Arguments.of("dateoff", "\'2018-12-12 17:34:11\'"),
                Arguments.of("dateon", "\'2020-02-18 19:16:48\'"),
                Arguments.of("hold", "HOLD"),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    @DatabaseSetup("/controller/inventory-hold/base/inventory-hold.xml")
    fun `Get inventory hold with argument query`(filter: String, directory: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/inventory-hold?$filter"),
            responseFile = "controller/inventory-hold/base/$directory/response.json"
        )
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    @DatabaseSetup("/controller/inventory-hold/base/inventory-hold.xml")
    fun `Get inventory hold list filter with filter`(filter: String, value: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/inventory-hold?filter=$filter==$value"),
            responseFile = "controller/inventory-hold/filter/$filter/response.json"
        )
    }

}
