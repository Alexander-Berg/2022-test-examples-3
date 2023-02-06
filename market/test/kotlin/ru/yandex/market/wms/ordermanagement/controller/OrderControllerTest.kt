package ru.yandex.market.wms.ordermanagement.controller

import com.github.springtestdbunit.annotation.DatabaseOperation
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.model.enums.OrderStatus
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.enums.WaveType
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient
import ru.yandex.market.wms.common.spring.servicebus.model.request.OrderMaxParcelDimensionsRequest
import ru.yandex.market.wms.common.spring.servicebus.model.response.OrderMaxParcelDimensionsResponse
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.ordermanagement.client.api.WmsApiClient
import java.nio.charset.StandardCharsets

@DatabaseSetup(value = ["/shipment-order/before/setup.xml"])
@SpringBootTest(classes = [IntegrationTestConfig::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderControllerTest(
    @MockBean
    @Autowired
    private val apiClient: WmsApiClient,

    @MockBean(name = "servicebusClient")
    @Autowired
    private val servicebusClient: ServicebusClient,
) : IntegrationTest() {

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsLastThreeOrders() {
        mockMvc.perform(
            get("/orders")
                .param("offset", "5")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-three-last-orders.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsOrdersWithoutWave() {
        mockMvc.perform(
            get("/orders")
                .param("withoutWave", "true")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-without-wave.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithSortByStatus() {
        mockMvc.perform(
            get("/orders")
                .param("sort", "status")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-all-orders-sort-by-status.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstTwoOrdersWithSortByType() {
        mockMvc.perform(
            get("/orders")
                .param("sort", "type")
                .param("limit", "2")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-two-first-orders-sort-by-type.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstFourOrdersWithFilterByStorerIdAndStatus() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "4")
                .param("filter", "storerId==10264169;status==DID_NOT_ALLOCATE")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-four-first-orders-filter-by-storerid-and-status.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstFourOrdersWithFilterByRegex() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "carrierName==%D_regi%")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-regex.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortByEditDate() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "editDate")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndEditDate() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;editDate=='2021-07-28 15:20:02'")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortBySusr2() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "susr2")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr2() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr2==SUSR2-2020")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortBySusr3() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "susr3")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr3() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr3==SUSR3-2020")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortBySusr5() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "susr5")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr5() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr5==SUSR5-2020")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-susr-empty.xml")
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr5_WhenSusr2_IsEmpty() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr2==''")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-susr-empty.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-susr-empty.xml")
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr5_WhenSusr3_IsEmpty() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr3==''")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-susr-empty.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-susr-empty.xml")
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndSusr5_WhenSusr5_IsEmpty() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;susr5==''")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-susr-empty.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortByActualShipDate() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "actualShipDate")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndActualShipDate() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;actualShipDate=='2021-07-28 15:20:02'")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortByEditWho() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("sort", "editWho")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndEditWho() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;editWho==SOMENAMEOFEDITWHO1")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-edit-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-unit-operation-type.xml")
    fun listOrdersReturnsAllOrdersWithFilterByStorerIdAndSortByOrderUnitOperationType() {
        mockMvc.perform(
            get("/orders")
                .param("limit", "1")
                .param("order", "desc")
                .param("sort", "orderUnitOperationType")
                .param("filter", "storerId==10264140")
                .contentType(APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-sort-by-order-unit-operation-type.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-unit-operation-type.xml")
    fun listOrdersReturnsAllOrdersWithFilterStorerIdAndOrderUnitOperationType() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "storerId==10264140;orderUnitOperationType==CROSSDOCK")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders" +
                            "/list-one-first-orders-filter-by-storerid-and-order-unit-operation-type.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldGetOrdersByWaveKey() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "waveKey==0000000528")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("controller/order-controller/listorders/list-by-wave-key.json")))
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-with-exclude-status.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-with-exclude-status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldGetOrdersWithoutExcludeStatuses() {
        mockMvc.perform(
            get("/orders")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller" +
                            "/listorders/list-by-exclude-status.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersWithBuilding() {
        mockMvc.perform(
            get("/orders")
                .param("offset", "5")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-three-last-orders.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterShipmentDateTime() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "shipmentDateTime=='2021-08-29 20:00:00'")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-one-first-orders-filter-by-shipment-date-time.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByBuilding() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "building==BUILDING-B")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-building.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByBatchOrderNumber() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "batchOrderNumber==B000000555")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-batch-order-number.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstFourOrdersWithSortByBatchOrderNumber() {
        val perform = mockMvc.perform(
            get("/orders")
                .param("order", "desc")
                .param("sort", "batchOrderNumber")
                .param("limit", "4")
                .contentType(APPLICATION_JSON)
        )
        perform
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-four-first-orders-sort-by-batch-order-number.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByTotalGrossWeight() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "totalGrossWeight==2.200")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-total-gross-weight.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstFourOrdersWithSortByTotalGrossWeight() {
        val perform = mockMvc.perform(
            get("/orders")
                .param("order", "desc")
                .param("sort", "totalGrossWeight")
                .param("limit", "4")
                .contentType(APPLICATION_JSON)
        )
        perform
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-four-first-orders-sort-by-total-gross-weight.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByTotalCube() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "totalCube==1322.200")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-total-cube.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstFourOrdersWithSortByTotalCube() {
        val perform = mockMvc.perform(
            get("/orders")
                .param("order", "desc")
                .param("sort", "totalCube")
                .param("limit", "4")
                .contentType(APPLICATION_JSON)
        )
        perform
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-four-first-orders-sort-by-total-cube.json"
                    ),
                    true
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByScheduledShipDate() {
        mockMvc.perform(
            get("/orders")
                .param(
                    "filter",
                    "scheduledShipDate=ge='2021-06-06 00:00:00';scheduledShipDate=le='2021-06-06 00:00:00'"
                )
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-scheduled-ship-date.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByAssigmentType() {
        mockMvc.perform(
            get("/orders")
                .param(
                    "filter",
                    "shipmentDateTime=='2021-08-29 20:00:00';(assigmentType==NON_SORTABLE_NON_CONVEYABLE,assigmentType==SORTABLE_NON_CONVEYABLE)"
                )
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-assigment-type.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-edit-date.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByAssigmentTypeFashion() {
        mockMvc.perform(
            get("/orders")
                .param("filter", "assigmentType==SORTABLE_FASHION")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-assigment-type-fashion.json"
                    )
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["scheduledShipDate", "shipmentDateTime", "orderDate", "actualShipDate", "editDate"])
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-date-time-fields.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-date-time-fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByDateTimeField(field: String) {
        mockMvc.perform(
            get("/orders")
                .param("filter", "$field=='2021-07-28 15:20:02'")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-date-time-fields.json"
                    )
                )
            )
    }

    @ParameterizedTest
    @ValueSource(strings = ["scheduledShipDate", "shipmentDateTime", "orderDate", "actualShipDate", "editDate"])
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-for-date-time-fields.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-for-date-time-fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsAllOrdersWithFilterByDateTimeFieldRange(field: String) {
        mockMvc.perform(
            get("/orders")
                .param("filter", "$field=ge='2021-07-28 15:00:00';$field=le='2021-07-29 15:00:59'")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/listorders/list-orders-filter-by-date-time-fields-range.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/listorders/immutable-state-order-by-date-carrier-status-wave.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/listorders/immutable-state-order-by-date-carrier-status-wave.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `list orders by shipmentDateTime, carrier, status, waveType`() {
        testListOrdersByWaveType(
            null,
            "controller/order-controller/listorders/list-orders-filter-by-date-carrier-status-wave-null.json"
        )
        testListOrdersByWaveType(
            WaveType.SINGLE,
            "controller/order-controller/listorders/list-orders-filter-by-date-carrier-status-wave-SINGLE.json"
        )
        testListOrdersByWaveType(
            WaveType.OVERSIZE,
            "controller/order-controller/listorders/list-orders-filter-by-date-carrier-status-wave-OVERSIZE.json"
        )
        testListOrdersByWaveType(
            WaveType.HOBBIT,
            "controller/order-controller/listorders/list-orders-filter-by-date-carrier-status-wave-HOBBIT.json"
        )
        testListOrdersByWaveType(
            WaveType.ALL,
            "controller/order-controller/listorders/list-orders-filter-by-date-carrier-status-wave-ALL.json"
        )
    }

    private fun testListOrdersByWaveType(waveType: WaveType?, responseFileName: String) {
        var filter = """
            shipmentDateTime=ge='2021-08-30 12:00:00'
            shipmentDateTime=le='2021-08-31 11:59:59'
            carrierName=='DPD region'
            status=='${OrderStatus.CREATED_EXTERNALLY}'
        """.trimIndent().lines().joinToString(";")

        waveType?.let { filter += ";waveType=='$waveType'" }

        val request = get("/orders")
            .param("filter", filter)
            .contentType(APPLICATION_JSON)


        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(responseFileName)))
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/controller/order-controller/create-order/create-order-common.xml"),
        DatabaseSetup("/controller/order-controller/create-order/before-create-order-with-2-flow-types.xml")
    )
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/controller/order-controller/create-order/before-create-order-with-2-flow-types.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/controller/order-controller/create-order/after-create-order-with-2-flow-types.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    fun createOrderWith2FlowTypes() {
        `when`(apiClient.putItemBatch(anyList(), eq(false), eq(true))).thenReturn(emptyList())
        val mvcResult: MvcResult = mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/order-controller/create-order" +
                            "/create-order-with-2-flow-types-request.json"
                    )
                )
        )
            .andExpect(status().isOk)
            .andReturn()
        val response = mvcResult.response
        response.characterEncoding = "UTF-8"
        JsonAssertUtils.assertFileLenientEquals(
            "controller/order-controller/create-order/create-order-with-2-flow-types-response.json",
            response.contentAsString,
            getOrderIgnoreFields()
        )
    }

    @Test
    @DatabaseSetup("/controller/order-controller/carriers-stat/orders.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/carriers-stat/orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getCarriesStatNoORdersTest() {
        mockMvc.perform(
            get("/orders/carriers/statuses")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/carriers-stat/no-orders.json"
                    )
                )
            )
    }


    @Test
    @DatabaseSetup("/controller/order-controller/carriers-stat/orders.xml")
    @ExpectedDatabase(
        value = "/controller/order-controller/carriers-stat/orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getCarriesStatTest() {
        mockMvc.perform(
            get("/orders/carriers/statuses")
                .contentType(APPLICATION_JSON)
                .param("day", "2021-07-29")
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    getFileContent(
                        "controller/order-controller/carriers-stat/orders-stat.json"
                    )
                )
            )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/after/after-created-orders.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    fun updateOrderTestHappyPath() {
        createOrder(
            "shipment-order/request/update-order.json",
            "shipment-order/response/updated-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-cancel-order.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-cancel-order-1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun cancelOrderHappyPathWhenOneOrder() {
        val externOrderKey = "22899370"
        mockMvc.perform(post("/ENTERPRISE/shipments/external-order-key/$externOrderKey/cancel"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-cancel-order.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-cancel-order-2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun cancelOrderHappyPathWhenTwoOrders() {
        val externOrderKey = "22899375"
        mockMvc.perform(post("/ENTERPRISE/shipments/external-order-key/$externOrderKey/cancel"))
            .andExpect(status().isOk)
    }


    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-cancel-order.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/before/before-cancel-order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun cancelOrderWithWrongExternOrderKey() {
        mockMvc.perform(post("/ENTERPRISE/shipments/22899371/cancel"))
            .andExpect(status().isOk)
    }

    @ParameterizedTest
    @ValueSource(strings = ["22899370", "22899375", "22899385"])
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/before/before-cancel-order-with-record-in-pickdetail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun cancelOrderThrowsCannonCancel(externOrderKey: String) {
        val mvcResult = mockMvc
            .perform(post("/ENTERPRISE/shipments/external-order-key/$externOrderKey/cancel"))
            .andExpect(status().isBadRequest)
            .andReturn()
        assertions.assertThat(mvcResult.response.contentAsString)
            .contains("Cannot cancel this Order.")
    }

    @ParameterizedTest
    @ValueSource(strings = ["22899370", "22899375", "23899375", "23909375"])
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-cancel-order-wrong-statuses.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/before/before-cancel-order-wrong-statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun cancelOrderIsNotCancelable(externOrderKey: String) {
        val mvcResult = mockMvc
            .perform(post("/ENTERPRISE/shipments/external-order-key/$externOrderKey/cancel"))
            .andExpect(status().isBadRequest)
            .andReturn()
        assertions.assertThat(mvcResult.response.contentAsString)
            .contains("Order is not cancelable.")
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @ExpectedDatabase(
        value = "/shipment-order/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestIncorrectRequest() {
        mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml")
    @ExpectedDatabase(
        value = "/shipment-order/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestWithNotExistingStorer() {
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/create-order-with-not-existing-storer.json", emptyList<String>(),
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer.xml")
    @ExpectedDatabase(
        value = "/shipment-order/before/before-with-storer.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestGetItemBatchWithoutExistingCarrier() {
        val mvcResult = mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("shipment-order/request/create-order.json"))
        )
            .andExpect(status().isBadRequest)
            .andReturn()
        assertions.assertThat(mvcResult.response.contentAsString)
            .contains("Storer/Sku is not valid")
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier.xml")
    @ExpectedDatabase(
        value = "/shipment-order/before/before-with-storer-courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestGetItemBatchWithoutExistingSku() {
        val mvcResult = mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("shipment-order/request/create-order.json"))
        )
            .andExpect(status().isBadRequest)
            .andReturn()
        assertions.assertThat(mvcResult.response.contentAsString)
            .contains("Storer/Sku is not valid")
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-one-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/before/before-with-storer-courier-one-sku.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestGetItemBatchWithoutExistingOneSku() {
        val mvcResult = mockMvc.perform(
            post("/orders")
                .contentType(APPLICATION_JSON)
                .content(getFileContent("shipment-order/request/create-order.json"))
        )
            .andExpect(status().isBadRequest)
            .andReturn()
        assertions.assertThat(mvcResult.response.contentAsString)
            .contains("Storer/Sku is not valid")
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml")
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
    fun createOrderTestHappyPath() {
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/created-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-max-order-dimensions-enabled.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-created-orders.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-max-order-dimensions-enabled.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestWithOrderMaxParcelDimensionsTask() {

        `when`(
            servicebusClient.getOrderMaxParcelDimensions(
                OrderMaxParcelDimensionsRequest.builder().externalOrderKey("22899370").build()
            )
        ).thenReturn(
            OrderMaxParcelDimensionsResponse.builder()
                .length(100)
                .width(200)
                .height(300)
                .dimSum(600)
                .weight(1000)
                .build()
        )
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/created-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-max-order-dimensions-enabled.xml")
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
    fun createOrderTestWithOrderMaxParcelDimensionsTaskEmptyResponse() {

        `when`(
            servicebusClient.getOrderMaxParcelDimensions(
                OrderMaxParcelDimensionsRequest.builder().externalOrderKey("22899370").build()
            )
        ).thenReturn(OrderMaxParcelDimensionsResponse.builder().build())
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/created-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-created-orders-anomaly-withdrawal.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestAnomalyWithdrawal() {
        createOrder(
            "shipment-order/request/create-order-anomaly-withdrawal.json",
            "shipment-order/response/created-order-anomaly-withdrawal.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-ignore-nonsort.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderIgnoreNonSort() {
        createOrder(
            "shipment-order/request/create-order-ignore-nonsort.json",
            "shipment-order/response/create-order-ignore-nonsort.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-force-nonsort.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-force-nonsort.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderForceNonSort() {
        createOrder(
            "shipment-order/request/create-order-force-nonsort.json",
            "shipment-order/response/create-order-force-nonsort.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-nonsort-height.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createNonSortOrderHeightExceeded() {
        createOrder(
            "shipment-order/request/create-order-nonsort-height.json",
            "shipment-order/response/create-order-nonsort-height.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-nonsort-width.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createNonSortOrderWidthExceeded() {
        createOrder(
            "shipment-order/request/create-order-nonsort-width.json",
            "shipment-order/response/create-order-nonsort-width.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-nonsort-length.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createNonSortOrderLengthExceeded() {
        createOrder(
            "shipment-order/request/create-order-nonsort-length.json",
            "shipment-order/response/create-order-nonsort-length.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-nonsort-sku.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-created-order-nonsort-weight.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createNonSortOrderWeightExceeded() {
        createOrder(
            "shipment-order/request/create-order-nonsort-weight.json",
            "shipment-order/response/create-order-nonsort-weight.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    // в заказе конв товары - заказ конв
    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/split-enabled/before.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/split-enabled/create-order-conv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createConvOrderWhenSplitEnabled() {
        createOrder(
            "shipment-order/request/split-enabled/create-order-conv.json",
            "shipment-order/response/split-enabled/create-order-conv.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    // в заказе конв и нонконв товары - заказ нонконв
    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/before/split-enabled/before.xml"],
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/split-enabled/create-order-conv-nonconv.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createConvAndNonConvOrderWhenSplitEnabled() {
        createOrder(
            "shipment-order/request/split-enabled/create-order-conv-nonconv.json",
            "shipment-order/response/split-enabled/create-order-conv-nonconv.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    // в заказе КГТ товары - заказ КГТ
    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/before/split-enabled/before.xml"],
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/split-enabled/create-order-oversize.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createOversizeOrderWhenSplitEnabled() {
        createOrder(
            "shipment-order/request/split-enabled/create-order-oversize.json",
            "shipment-order/response/split-enabled/create-order-oversize.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    // в заказе конв, нонконв и КГТ товары - заказ разделяется на КГТ и конв + нонконв
    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/before/split-enabled/before.xml"],
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/split-enabled/create-order-conv-nonconv-oversize.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createConvAndNonConvAndOversizeOrderWhenSplitEnabled() {
        createOrder(
            "shipment-order/request/split-enabled/create-order-conv-nonconv-oversize.json",
            "shipment-order/response/split-enabled/create-order-conv-nonconv-oversize.json",
            getOrderIgnoreFields(),
            status().isOk
        )
        // ручка пока не реализована
        // getOrderByOriginOrderKey("0000039466", "split-enabled/created-order-conv-nonconv-oversize")
    }

    // в заказе конв, нонконв и КГТ товары, но СД однопосылочная - заказ не разделяется
    @Test
    @DatabaseSetup(type = DatabaseOperation.REFRESH, value = [
        "/shipment-order/before/common.xml",
        "/shipment-order/before/split-enabled/before.xml"
    ])
    @ExpectedDatabase(
        value = "/shipment-order/after/split-enabled/create-order-conv-nonconv-oversize-ds-sp.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createConvAndNonConvAndOversizeOrderWhenSplitEnabledButDsSingleParcel() {
        createOrder(
            "shipment-order/request/split-enabled/create-order-conv-nonconv-oversize-ds-sp.json",
            "shipment-order/response/split-enabled/create-order-conv-nonconv-oversize-ds-sp.json",
            getOrderIgnoreFields(),
            status().isOk
        )
        // ручка пока не реализована
        // getOrderByOriginOrderKey("0000039466", "split-enabled/created-order-conv-nonconv-oversize-ds-sp")
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/shipment-order/before/before-with-storer-courier-all-sku.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-created-loadtest-orders.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestLoadTestOrder() {
        createOrder(
            "shipment-order/request/create-loadtest-order.json",
            "shipment-order/response/created-loadtest-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier.xml")
    @ExpectedDatabase(
        value = "/shipment-order/after/after-put-outbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestPutOutboundSuccess() {
        createOrder(
            "shipment-order/request/put-outbound.json",
            "shipment-order/response/put-outbound.json", emptyList<String>(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/before-with-storer-courier.xml",
        "/shipment-order/after/after-put-outbound.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-update-put-outbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestUpdatePutOutboundSuccess() {
        createOrder(
            "shipment-order/request/update-put-outbound.json",
            "shipment-order/response/updated-put-outbound.json", emptyList<String>(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/before-with-storer-courier.xml",
        "/shipment-order/before/before-outbound-with-details.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-update-outbound-with-details-by-put-outbound.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestUpdateOutboundWithDetailsByPutOutboundSuccess() {
        createOrder(
            "shipment-order/request/update-outbound-with-details-put-outbound.json",
            "shipment-order/response/updated-outbound-with-details-put-outbound.json", emptyList<String>(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml", "/map-carrier/1/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
        value = "/map-carrier/1/after-created-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestPutMappedOrderHappyPath() {
        createOrder(
            "map-carrier/1/create-order-request.json",
            "map-carrier/1/created-order-response.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml", "/map-carrier/3/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
        value = "/map-carrier/3/after-created-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestPutMappedOrderOutboundAutoHappyPath() {
        createOrder(
            "map-carrier/3/create-order-request.json",
            "map-carrier/3/created-order-response.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-3-flow-types.xml")
    @ExpectedDatabases(
        ExpectedDatabase(
            value = "/shipment-order/before/before-with-3-flow-types.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        ),
        ExpectedDatabase(
            value = "/shipment-order/after/after-created-3-flow-types.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
        )
    )
    @Throws(
        Exception::class
    )
    fun postOrderWith3FlowTypes() {
        createOrder(
            "shipment-order/request/create-order.json",
            "shipment-order/response/created-order-3-flow-types.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/map-carrier/common.xml", "/map-carrier/2/before-with-storer-courier-all-sku.xml")
    @ExpectedDatabase(
        value = "/map-carrier/2/after-created-orders.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOrderTestStorerTypeShouldBeConsidered() {
        createOrder(
            "map-carrier/2/create-order-request.json",
            "map-carrier/2/created-order-response.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/common.xml", "/shipment-order/before/before-with-storer-courier-all-sku.xml")
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
    fun createOrderTestWithShipmentDateTime() {
        createOrder(
            "shipment-order/request/create-order-with-shipmentdatetime.json",
            "shipment-order/response/created-order-with-shipmentdatetime.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/common.xml",
        "/shipment-order/before/before-create-outbound-auction-order.xml"
    )
    @ExpectedDatabase(
        value = "/shipment-order/after/after-create-outbound-auction-order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Throws(
        Exception::class
    )
    fun createOutboundAuctionOrderHappyPath() {
        createOrder(
            "shipment-order/request/create-outbound-auction-order.json",
            "shipment-order/response/create-outbound-auction-order.json",
            getOrderIgnoreFields(),
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/shipment-order/before/before-with-storer-courier-all-sku.xml",
        "/shipment-order/before/before-get-order.xml"
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByExternalOrderKeyTestSuccess() {
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/external-order-key/22899370")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andReturn()
        JsonAssertUtils
            .assertFileNonExtensibleEquals(
                "shipment-order/response/get-order-response.json",
                mvcResult.response.contentAsString
            )
    }

    @Test
    @DatabaseSetup("/shipment-order/before/before-with-storer-courier-all-sku.xml")
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByExternalOrderKeyTestNoOrders() {
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/external-order-key/10101010")
                .contentType(APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andReturn()
        Assertions.assertEquals("", mvcResult.response.contentAsString)
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/after/after-order-with-multi-items.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithMultiItemsBomsAndPack() {
        val originOrderKey = "0000001057"
        val orderKey = "0000001047"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/after/after-order-with-bom.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithBom() {
        val originOrderKey = "0000010879"
        val orderKey = "0000000879"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    @DatabaseSetup(value = ["/shipment-order/after/after-order-with-bom.xml"], type = DatabaseOperation.INSERT)
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWhenIdNotFound() {
        mockMvc.perform(get("/ENTERPRISE/shipments/fakeid").accept(APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.containsString("Order not found: fakeid")))
    }

    @Test
    @Throws(java.lang.Exception::class)
    @Disabled
    fun getOrderByOriginOrderKeyTest404WhenIdNotFound() {
        mockMvc.perform(get("/ENTERPRISE/shipments/fakeid").accept(APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message", Matchers.containsString("Order not found: fakeid")))
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/settings.xml", "/shipment-order/before/fake-parcel/shipped-cancelled-not-picked.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderWithNonsortSippedCancelledAndNotPickedWithFake() {
        getOrderByOriginOrderKey("0018682523", "fake/real-and-fake-shipped-cancelled-not-picked")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/settings.xml", "/shipment-order/before/fake-parcel/one-detail-not-picked.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsInDetailNotPickedWithFake() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/one-detail-not-picked.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsInDetailNotPickedWithoutFake() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-NO-FAKE")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/settings.xml", "/shipment-order/before/fake-parcel/one-detail-partial-packed.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsInDetailPartialPackedWithFake() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/one-detail-partial-packed.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsInDetailPartialPackedWithNoFake() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL-NO-FAKE")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/one-detail-partial-canceled.xml", "/shipment-order/before/fake-parcel/settings.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsWithAdjustedQty() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-PARTIAL-CANCELED")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/fake-parcel/no-picks.xml", "/shipment-order/before/fake-parcel/settings.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestOrderTwoItemsNoPicksFakeFlagEnabled() {
        getOrderByOriginOrderKey("0000FAKE01", "fake/0000FAKE01-NO-PICKS")
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/before/common.xml", "/shipment-order/after/after-order-with-identities.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithIdentities() {
        val originOrderKey = "0000002048"
        val orderKey = "0000001048"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/after/after-order-simple-with-pack-and-pallet.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithPackAndPallet() {
        val originOrderKey = "0000081223"
        val orderKey = "0000081213"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    @DatabaseSetup(value = ["/shipment-order/after/after-order-simple-with-pack.xml"], type = DatabaseOperation.INSERT)
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithPack() {
        val originOrderKey = "0000081113"
        val orderKey = "0000000410"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Test
    @DatabaseSetup(
        value = ["/shipment-order/after/after-order-simple-without-pack.xml"],
        type = DatabaseOperation.INSERT
    )
    @Throws(
        java.lang.Exception::class
    )
    @Disabled
    fun getOrderByOriginOrderKeyTestWithoutPack() {
        val originOrderKey = "0000010881"
        val orderKey = "0000000881"
        val mvcResult = mockMvc.perform(
            get("/ENTERPRISE/shipments/$originOrderKey")
                .accept(APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "get-order/response/$orderKey.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
        )
    }

    @Throws(java.lang.Exception::class)
    private fun getOrderByOriginOrderKey(originOrderKey: String, response: String) {
        val mvcResult = mockMvc.perform(get("/ENTERPRISE/shipments/$originOrderKey").accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn()
        JsonAssertUtils.assertFileEquals(
            "shipment-order/response/$response.json",
            mvcResult.response.contentAsString,
            JSONCompareMode.LENIENT
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
