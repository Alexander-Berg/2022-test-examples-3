package ru.yandex.market.wms.dimensionmanagement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.wms.common.model.enums.ItrnSourceType.MEASUREMENT_MOVE_TO_ID
import ru.yandex.market.wms.common.model.enums.ItrnSourceType.MEASUREMENT_MOVE_TO_LOC
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.common.spring.utils.JsonAssertUtils
import ru.yandex.market.wms.core.base.dto.SerialInventoryDto
import ru.yandex.market.wms.core.base.request.DimensionItem
import ru.yandex.market.wms.core.base.request.MoveSerialNumbersRequest
import ru.yandex.market.wms.core.base.request.SaveDimensionsRequest
import ru.yandex.market.wms.core.base.request.SkuDimensionsItem
import ru.yandex.market.wms.core.base.request.SkuInfoForMeasurementRequest
import ru.yandex.market.wms.core.base.request.SkuIdRequestItem
import ru.yandex.market.wms.core.base.request.SourceType
import ru.yandex.market.wms.core.base.response.DimensionsResponseItem
import ru.yandex.market.wms.core.base.response.GetOutboundSerialNumberResponse
import ru.yandex.market.wms.core.base.response.GetSerialInventoryResponse
import ru.yandex.market.wms.core.base.response.IdInfoResponse
import ru.yandex.market.wms.core.base.response.SkuInfoForMeasurementResponse
import ru.yandex.market.wms.core.base.response.SkuInfoForMeasurementResponseItem
import ru.yandex.market.wms.core.base.response.SkuIdResponseItem
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.exception.MoveBySerialNumbersException
import ru.yandex.market.wms.dimensionmanagement.configuration.DimensionManagementIntegrationTest
import ru.yandex.market.wms.dimensionmanagement.exception.MeasureStationNotFoundByAssigneeException
import ru.yandex.market.wms.dimensionmanagement.service.ScanningOperationLog
import ru.yandex.market.wms.picking.client.PickingClient
import ru.yandex.market.wms.picking.core.model.response.CreateTasksResponse
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class MeasurementOrderControllerTest : DimensionManagementIntegrationTest() {

    @Autowired
    @MockBean
    private lateinit var coreClient: CoreClient

    @Autowired
    @MockBean
    private lateinit var pickingClient: PickingClient

    @Autowired
    private lateinit var securityDataProvider: SecurityDataProvider

    @Autowired
    @MockBean
    private lateinit var scanningOperationLog: ScanningOperationLog

    @BeforeEach
    fun clean() {
        reset(coreClient, scanningOperationLog)
        reset(pickingClient)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPage() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonStrictOrder("controller/measurement-order-controller/list-orders/first-page.json"))
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsNextPage() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("offset", "2")
                .param("limit", "2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonStrictOrder("controller/measurement-order-controller/list-orders/next-page.json"))
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsPageWhenOffsetIsEqualToOrdersCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("offset", "8")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/offset-is-equal-to-orders-count.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsPageWhenOffsetIsGreaterThanOrdersCount() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("offset", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/offset-is-greater-than-orders-count.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByStatus() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "status==ASSIGNED")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-status.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByType() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "type==FROM_STOCK")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-type.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByStorerAndSku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "storer==10264169;sku==0000000521")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-storer-and-sku.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterBySerialNumberWithRegex() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "serialNumber==%987%43%")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-serial-number-regex.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByAltSku() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "altSku==''")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-alt-sku.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithSortByAssignedAscending() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("sort", "assigned")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/sort-by-assigned.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithSortByStatusDescending() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("sort", "status")
                .param("limit", "4")
                .param("order", "DESC")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/sort-by-status-desc.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByAssignedAndSortBySerialNumberAscending() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "(assigned==user,assigned=='')")
                .param("sort", "serialNumber")
                .param("limit", "4")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-assigned-sort-by-serial-number.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByWeight() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "weight=='1.05'")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-weight.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByEmptyLength() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "length==''")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-empty-length.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByAddDateAndEditDate() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "addDate=='2022-07-07 14:00:00';editDate=='2022-07-07 16:00:00'")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-add-date.json"
                )
            )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/list-orders/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/list-orders/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun listOrdersReturnsFirstPageWithFilterByAddWho() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/api/v1/order")
                .param("filter", "addWho=='TEST3'")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonStrictOrder(
                    "controller/measurement-order-controller/list-orders/filter-by-add-who.json"
                )
            )
    }

    private fun jsonStrictOrder(expectedJsonFileName: String): ResultMatcher {
        return ResultMatcher { result: MvcResult ->
            val content = result.response.getContentAsString(StandardCharsets.UTF_8)
            JsonAssertUtils.assertFileEquals(expectedJsonFileName, content, JSONCompareMode.STRICT_ORDER)
        }
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/create-by-serialnumber-happy-path/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createFromSerialnumberHappyPath() {
        Mockito.doReturn(GetOutboundSerialNumberResponse(null))
            .`when`(coreClient).getOutboundSerialNumber(anyString())
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "32596110",
                    "10264169",
                    "0000000527",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        testPostRequest(
            "/api/v1/order/32596110",
            "controller/measurement-order-controller/create-by-serialnumber-happy-path/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/create-by-serialnumber-with-existing-order/" +
        "order-without-serialnumber.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/create-by-serialnumber-with-existing-order/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createFromSerialnumberWhenExistingOrderWithoutSerialNumber() {
        Mockito.doReturn(GetOutboundSerialNumberResponse(null))
            .`when`(coreClient).getOutboundSerialNumber(anyString())
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "32596110",
                    "10264169",
                    "0000000527",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        testPostRequest(
            "/api/v1/order/32596110",
            "controller/measurement-order-controller/create-by-serialnumber-happy-path/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/create-by-skuId-happy-path/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createFromSkuIdHappyPath() {
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "000001",
                    "STORER-01",
                    "ROV0001",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        testPostRequest(
            "/api/v1/order",
            "controller/measurement-order-controller/create-by-skuId-happy-path/request.json",
            "controller/measurement-order-controller/create-by-skuId-happy-path/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/initial-state.xml")
    fun createFromSerialnumberShouldFailCauseNoSerialInventoryWithSuchSerialNumberExists() {
        Mockito.doReturn(GetSerialInventoryResponse(null))
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        testPostRequest(
            "/api/v1/order/000001",
            null,
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/initial-state.xml")
    fun createFromOutboundSerialnumberShouldFail() {
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "000001",
                    "STORER-01",
                    "ROV0001",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        Mockito.doReturn(GetOutboundSerialNumberResponse("000001"))
            .`when`(coreClient).getOutboundSerialNumber(anyString())
        testPostRequest(
            "/api/v1/order/000001",
            null,
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measurement-order-controller/" +
            "create-by-serialnumber-should-return-existed-new-order/initial-state.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/" +
            "create-by-serialnumber-should-return-existed-new-order/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createFromSerialnumberShouldReturnExistedNewOrder() {
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "000001",
                    "STORER-01",
                    "ROV0001",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())
        Mockito.doReturn(GetOutboundSerialNumberResponse(null))
            .`when`(coreClient).getOutboundSerialNumber(anyString())
        testPostRequest(
            "/api/v1/order/000001",
            "controller/measurement-order-controller/" +
                "create-by-serialnumber-should-return-existed-new-order/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measurement-order-controller/" +
            "create-by-skuId-found-existed-in-progress-order/initial-state.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/" +
            "create-by-skuId-found-existed-in-progress-order/initial-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createFromSkuIdShouldReturnExistingInProgressOrder() {
        Mockito.doReturn(
            GetSerialInventoryResponse(
                SerialInventoryDto(
                    "000001",
                    "STORER-01",
                    "ROV0001",
                    "lot",
                    "loc",
                    "id",
                    BigDecimal.ONE,
                    "test",
                    "test"
                )
            )
        )
            .`when`(coreClient).getSerialInventoryBySerialNumber(anyString())

        testPostRequest(
            "/api/v1/order",
            "controller/measurement-order-controller/" +
                "create-by-skuId-found-existed-in-progress-order/request.json",
            "controller/measurement-order-controller/" +
                "create-by-skuId-found-existed-in-progress-order/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup(
        "/controller/measurement-order-controller/create-multiple-by-skuIds-happy-pass/before.xml"
    )
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/create-multiple-by-skuIds-happy-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createMultipleOrdersBySkuIdsHappyPass() {
        Mockito.doReturn(
            CreateTasksResponse.builder()
                .taskDetails(listOf<CreateTasksResponse.TaskDetailItem>(
                    CreateTasksResponse.TaskDetailItem.builder()
                        .assignmentNumber("0001")
                        .serialKey("0001")
                        .skuId(SkuId.of("STORER_KEY_1", "SKU_002"))
                        .build(),
                    CreateTasksResponse.TaskDetailItem.builder()
                        .assignmentNumber("0001")
                        .serialKey("0002")
                        .skuId(SkuId.of("STORER_KEY_2", "SKU_003"))
                        .build(),
                    CreateTasksResponse.TaskDetailItem.builder()
                        .assignmentNumber("0001")
                        .serialKey("0003")
                        .skuId(SkuId.of("STORER_KEY_2", "SKU_004"))
                        .build(),
                ))
                .issues(listOf())
                .build())
            .`when`(pickingClient).createMeasurementTasks(
                any()
            )

        testPostRequest(
            "/api/v1/order/multi",
            "controller/measurement-order-controller/create-multiple-by-skuIds-happy-pass/request.json",
            "controller/measurement-order-controller/create-multiple-by-skuIds-happy-pass/response.json",
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/initial-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/cancel-order-happy-path/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelOrderHappyPath() {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/order/5/cancel"))
            .andExpect(status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/cancel-orders-happy-path/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/cancel-orders-happy-path/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelOrdersHappyPath() {

        testPutRequest(
            "/api/v1/order/cancel",
            "controller/measurement-order-controller/cancel-orders-happy-path/request.json",
            null,
            status().isOk
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/cancel-orders-in-terminal-state/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/cancel-orders-in-terminal-state/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelOrdersInTerminalState() {
        testPutRequest(
            "/api/v1/order/cancel",
            "controller/measurement-order-controller/cancel-orders-in-terminal-state/request.json",
            "controller/measurement-order-controller/cancel-orders-in-terminal-state/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/cancel-order-in-terminal-state/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/cancel-order-in-terminal-state/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cancelOrderInTerminalState() {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/order/5/cancel"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/with-assigned-user-on-station/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/with-assigned-user-on-station/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun startOrderHappyPass() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC-B", "", MEASUREMENT_MOVE_TO_LOC, "7")
            )

        whenever(
            coreClient.getSkuInfoForMeasurement(
                SkuInfoForMeasurementRequest(listOf(SkuIdRequestItem("107", "0000000522")))
            )
        )
            .thenReturn(
                SkuInfoForMeasurementResponse(
                    listOf(
                        SkuInfoForMeasurementResponseItem(
                            SkuIdResponseItem("107", "0000000522"),
                            DimensionsResponseItem(
                                BigDecimal(6.18),
                                BigDecimal(5.48),
                                BigDecimal(4.27),
                                BigDecimal(2.5),
                                BigDecimal(4.0)
                            ),
                            "My awesome description"
                        )
                    )
                )
            )

        whenever(
            coreClient.getSerialInventoryBySerialNumber("1234567890")
        )
            .thenReturn(
                GetSerialInventoryResponse(
                    SerialInventoryDto(
                        "1234567890",
                        "storer",
                        "ROV0001",
                        "LOT01",
                        "LOC1",
                        "TM0001",
                        BigDecimal.ONE,
                        "TEST",
                        "TEST"
                    )
                )
            )

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/start"),
            "controller/measurement-order-controller/start-order/happy-pass/response.json",
            status().isOk
        )

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC-B", "", MEASUREMENT_MOVE_TO_LOC, "7")
        )
        verify(coreClient).getSkuInfoForMeasurement(
            SkuInfoForMeasurementRequest(listOf(SkuIdRequestItem("107", "0000000522")))
        )
        verify(coreClient).getSerialInventoryBySerialNumber("1234567890")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/with-assigned-user-on-station/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/with-assigned-user-on-station/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun startOrderRetrievingDimensionsFailed() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC-B", "", MEASUREMENT_MOVE_TO_LOC, "7")
            )

        whenever(
            coreClient.getSerialInventoryBySerialNumber("1234567890")
        )
            .thenReturn(
                GetSerialInventoryResponse(
                    SerialInventoryDto(
                        "1234567890",
                        "storer",
                        "ROV0001",
                        "LOT01",
                        "LOC1",
                        "TM0001",
                        BigDecimal.ONE,
                        "TEST",
                        "TEST"
                    )
                )
            )

        whenever(
            coreClient.getSkuInfoForMeasurement(
                SkuInfoForMeasurementRequest(listOf(SkuIdRequestItem("107", "0000000522")))
            )
        )
            .thenThrow(RuntimeException("Test exception"))

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/start"),
            "controller/measurement-order-controller/start-order/retrieving-dimensions-failed/response.json",
            status().isOk
        )

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC-B", "", MEASUREMENT_MOVE_TO_LOC, "7")
        )
        verify(coreClient).getSkuInfoForMeasurement(
            SkuInfoForMeasurementRequest(listOf(SkuIdRequestItem("107", "0000000522")))
        )
        verify(coreClient).getSerialInventoryBySerialNumber("1234567890")
        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun startOrderWithIncorrectStatus() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/5/start"),
            "controller/measurement-order-controller/start-order/try-start-with-incorrect-status/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/immutable.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun startOrderWhenNotExists() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/10/start"),
            "controller/measurement-order-controller/start-order/order-not-found/response.json",
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/balance-move-fail/final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun startOrderBalanceMoveFailed() {
        whenever(
            coreClient.getSerialInventoryBySerialNumber("1234567890")
        ).thenReturn(
            GetSerialInventoryResponse(null)
        )

        Mockito.`when`(
            coreClient
                .moveSerialNumbers(
                    MoveSerialNumbersRequest(
                        listOf("1234567890"),
                        "LOC-A",
                        "",
                        MEASUREMENT_MOVE_TO_LOC,
                        "7"
                    )
                )
        )
            .thenThrow(MoveBySerialNumbersException("Error while moving balances (itrnSourceKey=7)"))

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/start"),
            "controller/measurement-order-controller/start-order/balance-move-fail/response.json",
            status().isInternalServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/start-order/measure-station-is-not-found/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/start-order/measure-station-is-not-found/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun startOrderMeasureStationIsNotFound() {
        whenever(
            coreClient.getSerialInventoryBySerialNumber("1234567890")
        ).thenReturn(
            GetSerialInventoryResponse(null)
        )

        Mockito.`when`(
            coreClient
                .moveSerialNumbers(
                    MoveSerialNumbersRequest(
                        listOf("1234567890"),
                        "LOC-A",
                        "",
                        MEASUREMENT_MOVE_TO_LOC,
                        "7"
                    )
                )
        )
            .thenThrow(MeasureStationNotFoundByAssigneeException("Error while moving balances (itrnSourceKey=7)"))

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/start"),
            "controller/measurement-order-controller/start-order/measure-station-is-not-found/response.json",
            status().isInternalServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/happy-pass/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/happy-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderHappyPass() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
            )

        whenever(
            coreClient
                .saveSkuDimensions(
                    SaveDimensionsRequest(
                        listOf(
                            SkuDimensionsItem(
                                skuId = SkuId("107", "0000000522"),
                                manufacturerSku = null,
                                dimensions = DimensionItem(
                                    weight = BigDecimal("70.010"),
                                    length = BigDecimal("100.000"),
                                    height = BigDecimal("90.000"),
                                    width = BigDecimal("100.000")
                                )
                            )
                        ),
                        securityDataProvider.user,
                        SourceType.MEASUREMENT
                    )
                )
        )
            .thenReturn(null)
        doNothing().whenever(scanningOperationLog)
            .writeMeasurement(any(), any())

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/incorrect-status-for-finish/request.json"
                    )
                ),
            null,
            status().isOk
        )

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
        )
        verify(coreClient)
            .saveSkuDimensions(
                SaveDimensionsRequest(
                    listOf(
                        SkuDimensionsItem(
                            skuId = SkuId("107", "0000000522"),
                            manufacturerSku = null,
                            dimensions = DimensionItem(
                                weight = BigDecimal("70.010"),
                                length = BigDecimal("100.000"),
                                height = BigDecimal("90.000"),
                                width = BigDecimal("100.000")
                            )
                        )
                    ),
                    securityDataProvider.user,
                    SourceType.MEASUREMENT
                )
            )
        verify(scanningOperationLog).writeMeasurement(any(), any())
        verifyNoMoreInteractions(coreClient, scanningOperationLog)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/async-enabled-happy-pass/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/async-enabled-happy-pass/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun finishOrderWhenAsyncEnabledHappyPass() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
            )

        whenever(
            coreClient
                .saveSkuDimensions(
                    SaveDimensionsRequest(
                        listOf(
                            SkuDimensionsItem(
                                skuId = SkuId("107", "0000000522"),
                                manufacturerSku = null,
                                dimensions = DimensionItem(
                                    weight = BigDecimal("70.010"),
                                    length = BigDecimal("100.000"),
                                    height = BigDecimal("90.000"),
                                    width = BigDecimal("100.000")
                                )
                            )
                        ),
                        securityDataProvider.user,
                        SourceType.MEASUREMENT
                    )
                )
        )
            .thenReturn(null)
        doNothing().whenever(scanningOperationLog)
            .writeMeasurement(any(), any())

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/incorrect-status-for-finish/request.json"
                    )
                ),
            null,
            status().isOk
        )

        // Задержка, чтобы задание успело завершиться в асинхронном режиме и ExpectedDatabase выполнился.
        Thread.sleep(1000)

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
        )
        verify(coreClient)
            .saveSkuDimensions(
                SaveDimensionsRequest(
                    listOf(
                        SkuDimensionsItem(
                            skuId = SkuId("107", "0000000522"),
                            manufacturerSku = null,
                            dimensions = DimensionItem(
                                weight = BigDecimal("70.010"),
                                length = BigDecimal("100.000"),
                                height = BigDecimal("90.000"),
                                width = BigDecimal("100.000")
                            )
                        )
                    ),
                    securityDataProvider.user,
                    SourceType.MEASUREMENT
                )
            )
        verify(scanningOperationLog).writeMeasurement(any(), any())
        verifyNoMoreInteractions(coreClient, scanningOperationLog)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithFailedDimensionIsZeroValidation() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/dimension-is-zero/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/dimension-is-zero/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithFailedAtLeastOneDimensionIsBigValidation() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/dimension-is-big-validation/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/dimension-is-big-validation/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithFailedLowDensityValidation() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/low-density-validation/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/low-density-validation/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithFailedHighDensityValidation() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/high-density-validation/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/high-density-validation/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithFailedAllDimensionsSameValidation() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/dimensions-same-validation/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/dimensions-same-validation/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/forced-validation/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/forced-validation/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithNonCriticalValidationCanBeForced() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
            )

        whenever(
            coreClient
                .saveSkuDimensions(
                    SaveDimensionsRequest(
                        listOf(
                            SkuDimensionsItem(
                                skuId = SkuId("107", "0000000522"),
                                manufacturerSku = null,
                                dimensions = DimensionItem(
                                    weight = BigDecimal("70.010"),
                                    length = BigDecimal("100.000"),
                                    height = BigDecimal("100.000"),
                                    width = BigDecimal("100.000")
                                )
                            )
                        ),
                        securityDataProvider.user,
                        SourceType.MEASUREMENT
                    )
                )
        )
            .thenReturn(null)
        doNothing().whenever(scanningOperationLog)
            .writeMeasurement(any(), any())

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/forced-validation/request.json"
                    )
                ),
            null,
            status().isOk
        )

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
        )
        verify(coreClient)
            .saveSkuDimensions(
                SaveDimensionsRequest(
                    listOf(
                        SkuDimensionsItem(
                            skuId = SkuId("107", "0000000522"),
                            manufacturerSku = null,
                            dimensions = DimensionItem(
                                weight = BigDecimal("70.010"),
                                length = BigDecimal("100.000"),
                                height = BigDecimal("100.000"),
                                width = BigDecimal("100.000")
                            )
                        )
                    ),
                    securityDataProvider.user,
                    SourceType.MEASUREMENT
                )
            )
        verify(scanningOperationLog).writeMeasurement(any(), any())
        verifyNoMoreInteractions(coreClient, scanningOperationLog)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithCriticalValidationCannotBeForced() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/forcing-validation-failed/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/forcing-validation-failed/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWithIncorrectStatus() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/5/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/incorrect-status-for-finish/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/incorrect-status-for-finish/response.json",
            status().isBadRequest
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun finishOrderWhenNotExists() {
        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/10/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/order-not-found/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/order-not-found/response.json",
            status().isNotFound
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/finish-order/immutable-state.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/finish-order/immutable-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun finishOrderBalanceMoveFailed() {
        whenever(
            coreClient
                .moveSerialNumbers(
                    MoveSerialNumbersRequest(listOf("1234567890"), "LOC-A", "TBL-07", MEASUREMENT_MOVE_TO_ID, "7")
                )
        )
            .thenThrow(MoveBySerialNumbersException("Error while moving balances (itrnSourceKey=7)"))

        testRequest(
            MockMvcRequestBuilders.put("/api/v1/order/7/finish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    getFileContent(
                        "controller/measurement-order-controller/finish-order/move-balances-failed/request.json"
                    )
                ),
            "controller/measurement-order-controller/finish-order/move-balances-failed/response.json",
            status().isInternalServerError
        )
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/soft-cancel/mobile-station/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/soft-cancel/mobile-station/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun softCancelOrderMobileStationHappyPass() {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/order/7/soft-cancel"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(
                "controller/measurement-order-controller/soft-cancel/mobile-station/response.json"
            )))
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/soft-cancel/not-mobile-station/before.xml")
    @ExpectedDatabase(
        value = "/controller/measurement-order-controller/soft-cancel/not-mobile-station/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun softCancelOrderNotMobileStationHappyPass() {
        doNothing().whenever(coreClient)
            .moveSerialNumbers(
                MoveSerialNumbersRequest(listOf("1234567890"), "LOC1", "", MEASUREMENT_MOVE_TO_ID, "7")
            )

        whenever(
            coreClient
                .getIdInfo("TM00001")).thenReturn(IdInfoResponse(id="TM00001", loc="LOC1", fillingStatus = "EMPTY", type="TM"))

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/order/7/soft-cancel"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(
                "controller/measurement-order-controller/soft-cancel/not-mobile-station/response.json"
            )))

        verify(coreClient).moveSerialNumbers(
            MoveSerialNumbersRequest(listOf("1234567890"), "LOC1", "TM00001", MEASUREMENT_MOVE_TO_ID, "7")
        )

        verify(coreClient).getIdInfo("TM00001")

        verifyNoMoreInteractions(coreClient)
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/soft-cancel/completed/before.xml")
    fun softCancelOrderWhichIsCompleted() {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/order/7/soft-cancel"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(
                "controller/measurement-order-controller/soft-cancel/completed/response.json"
            )))
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/remaining-capacity/1/before.xml")
    fun remainingCapacityTestOk() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/order/remaining-capacity"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(
                "controller/measurement-order-controller/remaining-capacity/1/response.json"
            )))
    }

    @Test
    @DatabaseSetup("/controller/measurement-order-controller/remaining-capacity/2/before.xml")
    fun remainingCapacityTestNoActiveOrders() {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/order/remaining-capacity"))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent(
                "controller/measurement-order-controller/remaining-capacity/2/response.json"
            )))
    }

    private fun testPostRequest(path: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response), false))
        }
    }

    private fun testRequest(request: RequestBuilder, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(request)
            .andExpect(status)

        if (response != null) {
            val fileContent = getFileContent(response)
            result.andExpect(content().json(fileContent, false))
        }
    }

    private fun testPostRequest(path: String, request: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(request))
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response), false))
        }
    }

    private fun testPutRequest(path: String, request: String, response: String?, status: ResultMatcher) {
        val result = mockMvc
            .perform(
                MockMvcRequestBuilders.put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getFileContent(request))
            )
            .andExpect(status)

        if (response != null) {
            result.andExpect(content().json(getFileContent(response), false))
        }
    }
}
