package ru.yandex.market.wms.ordermanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils

@DatabaseSetup(value = ["/shipment-order/before/setup.xml"])
@SpringBootTest(classes = [IntegrationTestConfig::class])
class OrderControllerShortageTest : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/full/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/full/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/full/request.json",
            "controller/order-controller/shortage/full/response.json"

            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/shipped-orderdetails/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/shipped-orderdetails/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage when one detail is shipped`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/shipped-orderdetails/request.json",
            "controller/order-controller/shortage/shipped-orderdetails/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/batch-order/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/batch-order/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with batch order`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/batch-order/request.json",
            "controller/order-controller/shortage/batch-order/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/batch-order-details-remove/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/batch-order-details-remove/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with batch removal`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/batch-order-details-remove/request.json",
            "controller/order-controller/shortage/batch-order-details-remove/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/cancelled-orderdetails/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/cancelled-orderdetails/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage when all details are cancelled`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/cancelled-orderdetails/request.json",
            "controller/order-controller/shortage/cancelled-orderdetails/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/partial/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/partial/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage partial`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/partial/request.json",
            "controller/order-controller/shortage/partial/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/batch-to-picked/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/batch-to-picked/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with batch status update to picked`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/batch-to-picked/request.json",
            "controller/order-controller/shortage/batch-to-picked/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-to-picked/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-to-picked/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with big withdrawal status update to picked`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-to-picked/request.json",
            "controller/order-controller/shortage/withdrawal-to-picked/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-to-sorted/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-to-sorted/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with withdrawal status update to sorted`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-to-sorted/request.json",
            "controller/order-controller/shortage/withdrawal-to-sorted/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-to-packed/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-to-packed/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with withdrawal status update to packed`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-to-packed/request.json",
            "controller/order-controller/shortage/withdrawal-to-packed/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-to-allocated/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-to-allocated/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with withdrawal status update to allocated`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-to-allocated/request.json",
            "controller/order-controller/shortage/withdrawal-to-allocated/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-to-dropped/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-to-dropped/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with withdrawal status update to dropped`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-to-dropped/request.json",
            "controller/order-controller/shortage/withdrawal-to-dropped/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/big-withdrawal-cancelled/before.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/big-withdrawal-cancelled/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage with big withdrawal cancellation`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/big-withdrawal-cancelled/request.json",
            "controller/order-controller/shortage/big-withdrawal-cancelled/response.json"
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/non-existing-orders/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/non-existing-orders/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `try process shortage when non existing orders are passed`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/non-existing-orders/request.json",
            "controller/order-controller/shortage/non-existing-orders/response.json",
            MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/non-existing-order-details/immutable.xml")
    fun `try process shortage when non existing orderDetails are passed`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/non-existing-order-details/request.json",
            "controller/order-controller/shortage/non-existing-order-details/response.json",
            MockMvcResultMatchers.status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/shortage/withdrawal-check/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/shortage/withdrawal-check/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `process shortage for non withdrawal returns bad request`() {
        performMockMvcRequest(
            "controller/order-controller/shortage/withdrawal-check/request.json",
            "controller/order-controller/shortage/withdrawal-check/response.json",
            MockMvcResultMatchers.status().isBadRequest
        )
    }

    private fun performMockMvcRequest(
        requestFileName: String,
        responseFileName: String,
        status: ResultMatcher = MockMvcResultMatchers.status().isOk
    ) {
        val mvcResult = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/orders/shortage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(FileContentUtils.getFileContent(requestFileName))
        )
            .andExpect(status)
            .andReturn()

        JsonAssertUtils.assertFileNonExtensibleEquals(responseFileName, mvcResult.response.contentAsString)
    }
}
