package ru.yandex.market.wms.core.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.util.LinkedMultiValueMap
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.utils.FileContentUtils

class SerialNumberControllerTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/serial/get-outbound/before.xml")
    @ExpectedDatabase("/controller/serial/get-outbound/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `Get outbound serial with qty picked`() =
        getWithAllChecks(
            urlPart = "get-outbound",
            params = mapOf("serialNumber" to "002"),
            expectedStatus = status().isOk,
            testResourceDir = "qty-picked"
        )

    @Test
    @DatabaseSetup("/controller/serial/get-outbound/before.xml")
    @ExpectedDatabase("/controller/serial/get-outbound/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `Get outbound serial with qty allocated`() =
        getWithAllChecks(
            urlPart = "get-outbound",
            params = mapOf("serialNumber" to "004"),
            expectedStatus = status().isOk,
            testResourceDir = "qty-allocated"
        )

    @Test
    @DatabaseSetup("/controller/serial/get-outbound/before.xml")
    @ExpectedDatabase("/controller/serial/get-outbound/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `Get outbound serial with pick details`() =
        getWithAllChecks(
            urlPart = "get-outbound",
            params = mapOf("serialNumber" to "006"),
            expectedStatus = status().isOk,
            testResourceDir = "pick-details"
        )

    @Test
    @DatabaseSetup("/controller/serial/get-outbound/before.xml")
    @ExpectedDatabase("/controller/serial/get-outbound/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `Get outbound serial which is not outbound`() =
        getWithAllChecks(
            urlPart = "get-outbound",
            params = mapOf("serialNumber" to "008"),
            expectedStatus = status().isOk,
            testResourceDir = "not-outbound"
        )

    @Test
    @DatabaseSetup("/controller/serial/by-serial-number/immutable.xml")
    @ExpectedDatabase(
        "/controller/serial/by-serial-number/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Get serial inventory by serial number successfully`() =
        getWithAllChecks(
            urlPart = "by-serial-number",
            params = mapOf("serialNumber" to "002"),
            expectedStatus = status().isOk,
            testResourceDir = "found"
        )

    @Test
    @DatabaseSetup("/controller/serial/by-serial-number/immutable.xml")
    @ExpectedDatabase(
        "/controller/serial/by-serial-number/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun `Get serial inventory by serial number - not found`() =
        getWithAllChecks(
            urlPart = "by-serial-number",
            params = mapOf("serialNumber" to "12345"),
            expectedStatus = status().isOk,
            testResourceDir = "not-found"
        )

    @Test
    @DatabaseSetup("/controller/serial/lost-serials/immutable.xml")
    fun `Get lost serials`() =
        getWithAllChecks(
            urlPart = "lost-serials",
            params = mapOf("receiptKey" to "12345"),
            expectedStatus = status().isOk,
            testResourceDir = "found"
        )

    @Test
    @DatabaseSetup("/controller/serial/characteristics/before.xml")
    fun `Get characteristics returns ok response when serial inventory, sku and cargo types were found`() =
        getWithAllChecks(
            urlPart = "characteristics",
            params = mapOf("serialNumber" to "1"),
            expectedStatus = status().isOk,
            testResourceDir = "sku-found-cargo-types-found"
        )

    @Test
    @DatabaseSetup("/controller/serial/characteristics/before.xml")
    fun `Get characteristics not found error when serial inventory was not found`() =
        getWithAllChecks(
            urlPart = "characteristics",
            params = mapOf("serialNumber" to "12345"),
            expectedStatus = status().isNotFound,
            testResourceDir = "serial-inventory-not-found"
        )

    @Test
    @DatabaseSetup("/controller/serial/characteristics/before.xml")
    fun `Get characteristics returns not found error when SKU was not not found`() =
        getWithAllChecks(
            urlPart = "characteristics",
            params = mapOf("serialNumber" to "2"),
            expectedStatus = status().isOk,
            testResourceDir = "sku-characteristics-not-found"
        )

    @Test
    @DatabaseSetup("/controller/serial/characteristics/before.xml")
    fun `Get characteristics returns non found error when SKU was found and cargo types were not found`() =
        getWithAllChecks(
            urlPart = "characteristics",
            params = mapOf("serialNumber" to "3"),
            expectedStatus = status().isOk,
            testResourceDir = "sku-found-cargo-types-not-found"
        )

    private fun getWithAllChecks(
        urlPart: String,
        params: Map<String, String>,
        expectedStatus: ResultMatcher,
        testResourceDir: String? = null,
    ) {
        val paramsMVM = LinkedMultiValueMap<String, String>()
        paramsMVM.setAll(params)

        val requestBuilder = MockMvcRequestBuilders.get("/serial/$urlPart")
            .params(paramsMVM)
            .contentType(MediaType.APPLICATION_JSON)

        val result = mockMvc.perform(requestBuilder)
            .andExpect(expectedStatus)

        if (testResourceDir != null) {
            result.andExpect(
                MockMvcResultMatchers.content().json(
                    FileContentUtils
                        .getFileContent("controller/serial/$urlPart/$testResourceDir/response.json"), true
                )
            )
        }
    }
}
