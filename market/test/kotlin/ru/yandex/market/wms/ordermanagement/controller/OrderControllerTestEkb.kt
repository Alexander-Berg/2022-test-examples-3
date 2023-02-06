package ru.yandex.market.wms.ordermanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.ordermanagement.client.api.WmsApiClient
import java.nio.charset.StandardCharsets

@DatabaseSetup(value = ["/shipment-order/before/setup.xml"])
@SpringBootTest(
    classes = [IntegrationTestConfig::class],
    properties = ["warehouse-timezone = Asia/Yekaterinburg"]
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTestEkb(
    @MockBean
    @Autowired
    private val apiClient: WmsApiClient,

    @MockBean(name = "servicebusClient")
    @Autowired
    private val servicebusClient: ServicebusClient,
) : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-ekb-time.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-ekb-time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldGetOrdersByWaveKeyEKBTime() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "waveKey==0000000528")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent("controller/order-controller/listorders/list-by-wave-key-ekb-time.json")
                )
            )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/shipmentdatetime-null-scenario.xml"
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-created-orders.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestWithShipmentDateTimeIfNull() {
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/created-order-with-shipmentdatetime-2.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Throws(Exception::class)
    private fun createOrder(
        requestFileName: String,
        responseFileName: String,
        ignoreFields: List<String>,
        status: ResultMatcher
    ) {
        val mvcResult = mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
                .content(getFileContent(requestFileName))
        )
            .andExpect(status)
            .andReturn()
        JsonAssertUtils.assertFileNonExtensibleEquals(
            responseFileName,
            mvcResult.response.getContentAsString(StandardCharsets.UTF_8),
            ignoreFields
        )
    }

    private fun getOrderIgnoreFields(): List<String> {
        return java.util.List.of(
            "ordersid",
            "orderdetails[**].orderdetailid"
        )
    }
}
