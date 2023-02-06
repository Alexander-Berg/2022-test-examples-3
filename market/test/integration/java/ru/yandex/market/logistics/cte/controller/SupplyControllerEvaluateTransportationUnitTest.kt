package ru.yandex.market.logistics.cte.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.logistics.cte.base.MvcIntegrationTest

class SupplyControllerEvaluateTransportationUnitTest: MvcIntegrationTest() {

    @Test
    @DatabaseSetups(
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qattribute.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/group.xml"),
        DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qmatrix_group.xml"),
    )
    @ExpectedDatabase("classpath:controller/evaluateTransportationUnit/after-minimal-request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    internal fun minimalRequest() {
        testEndpointStatus(
            "/logistic_services/supplies/123/units",
            "controller/evaluateTransportationUnit/minimal_request.json", HttpStatus.OK
        )
    }

    @Test
    @DatabaseSetups(
            DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qattribute.xml"),
            DatabaseSetup("classpath:repository/get-attributes-by-unit_type/group.xml"),
            DatabaseSetup("classpath:repository/get-attributes-by-unit_type/qmatrix_group.xml"),
            DatabaseSetup("classpath:controller/evaluateTransportationUnit/updateRequest/unit.xml"),
            DatabaseSetup("classpath:controller/evaluateTransportationUnit/updateRequest/unit_attribute.xml"),
    )
    @ExpectedDatabase("classpath:controller/evaluateTransportationUnit/updateRequest/after-updating-request.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    internal fun updateRequest() {
        testEndpointStatus(
                "/logistic_services/supplies/123/units",
                "controller/evaluateTransportationUnit/updateRequest/update_request.json", HttpStatus.OK
        )
    }

    @Throws(Exception::class)
    private fun testEndpointStatus(url: String, requestFile: String, expectedStatus: HttpStatus) {
        mockMvc.perform(
            MockMvcRequestBuilders.put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(readFromFile(requestFile))
        ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value())).andReturn().response.contentAsString
    }
}
