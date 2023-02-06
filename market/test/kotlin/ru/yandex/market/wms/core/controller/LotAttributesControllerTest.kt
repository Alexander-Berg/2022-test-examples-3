package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.core.HttpAssert
import java.util.stream.Stream

class LotAttributesControllerTest : IntegrationTest() {

    private val httpAssert = HttpAssert { mockMvc }

    companion object {
        @JvmStatic
        fun argumentProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("", "no-filter"),
                Arguments.of("order=desc", "reversed-order"),
                Arguments.of("limit=1", "limit"),
                Arguments.of("limit=1&offset=1", "offset"),
                Arguments.of("sort=sku", "sorted-by-sku"),
            )
        }

        @JvmStatic
        fun filterProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("sku", "ROV0000000000000000005"),
                Arguments.of("lot", "0000000016"),
                Arguments.of("storerkey", "10264171"),
                Arguments.of("lottable01", "lot14"),
                Arguments.of("lottable02", "lot25"),
                Arguments.of("lottable03", "lot31"),
                Arguments.of("lottable04", "'2019-11-23 16:16:17'"),
                Arguments.of("lottable05", "'2021-11-23 16:16:17'"),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("argumentProvider")
    @DatabaseSetup("/controller/lot-attribute/base/lot-attribute.xml")
    fun `Get lot attribute list`(filter: String, directory: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/lot-attributes?$filter"),
            responseFile = "controller/lot-attribute/base/$directory/response.json"
        )
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    @DatabaseSetup("/controller/lot-attribute/filter/lot-attribute.xml")
    fun `Filter lot attribute`(filter: String, value: String) {
        httpAssert.assertApiCall(
            MockMvcRequestBuilders.get("/lot-attributes?filter=$filter==$value"),
            responseFile = "controller/lot-attribute/filter/$filter/response.json"
        )
    }
}
