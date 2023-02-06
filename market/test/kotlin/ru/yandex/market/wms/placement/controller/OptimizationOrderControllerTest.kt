@file:Suppress("SameParameterValue")

package ru.yandex.market.wms.placement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.dao.entity.LotLocId
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.constraints.core.response.CheckByLocAndSkuResponse
import ru.yandex.market.wms.core.base.response.GetChildContainersResponse
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.placement.service.transportation.TransportOrderService
import java.math.BigDecimal

class OptimizationOrderControllerTest : PlacementIntegrationTest() {

    @MockBean
    private lateinit var transportOrderService: TransportOrderService

    @MockBean
    @Autowired
    private lateinit var coreService: CoreService

    @BeforeEach
    fun cleanAndSetupDefaults() {
        reset(coreService)

        whenever(coreClient.getChildContainers(anyString()))
            .thenReturn(GetChildContainersResponse(emptyList()))
        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(CheckByLocAndSkuResponse(true, emptyList()))
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/get-order/empty-content/before.xml")
    fun `getCurrentOrder - return empty content`() {
        getCurrentOrder(
            expectedContent = content().string(""),
            expectedStatus = status().isNoContent
        )
    }


    @Test
    @DatabaseSetup("/controller/optimization-order/get-order/current-order/before.xml")
    fun `getCurrentOrder - return active optimization order`() {
        val path = "controller/optimization-order/get-order/current-order/response.json"
        getCurrentOrder(
            expectedContent = content().json(getFileContent(path), true),
            expectedStatus = status().isOk
        )
    }

    @Test
    fun `getIdState - bad request, id not added`() {
        getOptimizationOrderIdState("id-not-added", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/get-id-state/ok/before.xml")
    fun `getIdState - ok`() {
        getOptimizationOrderIdState("ok", status().isOk)
    }

    @Test
    fun `addEmptyId - not found, because id doesn't exist`() {
        setupIdExists("RCP1", false)

        addEmptyId(testCase = "id-not-found", expectedStatus = status().isNotFound)
    }

    @Test
    fun `addEmptyId - bad request, because id is not empty`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf(SerialInventory.builder().build()))

        addEmptyId(testCase = "id-not-empty", expectedStatus = status().isBadRequest)
    }

    @Test
    fun `addEmptyId - bad request, because id has nested ids`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf())
        setupNestedIds("RCP1", listOf("RCP2", "RCP3"))

        addEmptyId(testCase = "id-has-nested-ids", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-empty-id/id-in-another-order/before.xml")
    fun `addEmptyId - bad request, because id in another order`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf())

        addEmptyId(testCase = "id-in-another-order", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-empty-id/create-order-and-add-id/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/add-empty-id/create-order-and-add-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `addEmptyId - successfully create new order and add id`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf())

        addEmptyId(testCase = "create-order-and-add-id", expectedStatus = status().isOk, strictJson = false)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-empty-id/add-and-set-active/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/add-empty-id/add-and-set-active/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `addEmptyId - successfully add id and set active`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf())

        addEmptyId(testCase = "add-and-set-active", expectedStatus = status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-id/id-not-found/before.xml")
    fun `addId - not found id`() {
        addId("id-not-found", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-id/empty-id/before.xml")
    fun `addId - bad request because id is empty`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", listOf())

        addId("empty-id", status().isBadRequest)
    }

    @Test
    fun `addId - bad request, because id has nested ids`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById("RCP1", serialNumbers = listOf("001", "002"))
        setupNestedIds("RCP1", listOf("RCP2", "RCP3"))

        addEmptyId(testCase = "id-has-nested-ids", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-id/added-to-another-order/before.xml")
    fun `addId - bad request because id added to another order`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById(id = "RCP1", serialNumbers = listOf("001", "002", "003"))

        addId("added-to-another-order", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-id/picked-reserved/before.xml")
    fun `addId - bad request id is picked & reserved`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById(id = "RCP1", serialNumbers = listOf("001", "002", "003"))

        setupLocLotIdById("RCP1", buildLotLocId(id = "RCP1", qtyPicked = BigDecimal.ONE))

        addId("picked-reserved", status().isBadRequest)

        setupLocLotIdById("RCP1", buildLotLocId(id = "RCP1", qtyAllocated = BigDecimal.ONE))

        addId("picked-reserved", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-id/ok-new-order/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/add-id/ok-new-order/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `addId - successfully with creating new order`() {
        setupIdExists("RCP1", true)
        setupSerialInventoriesById(id = "RCP1", loc = "1-01", serialNumbers = listOf("001", "002", "003"))
        setupLocLotIdById("RCP1", buildLotLocId(id = "RCP1", loc = "1-01"))

        addId("ok-new-order", status().isOk, strictJson = false)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/place-id/ok/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/place-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `placeId - successfully`() {
        setupSerialInventoriesById(loc = "PLACEMENT", id = "123", serialNumbers = listOf("0000000100", "0000000101"))
        placeId(testCase = "ok", expectedStatus = status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/place-id/already-placed/before.xml")
    fun `placeId - bad request because id not added to order`() {
        setupSerialInventoriesById(loc = "1-01", id = "123", serialNumbers = listOf("0000000100", "0000000101"))
        placeId(testCase = "already-placed", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/activate-id/id-not-added/before.xml")
    fun `activateId - bad request because id not added to order`() {
        activateId("id-not-added", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/activate-id/already-activated/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/activate-id/already-activated/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `activateId - successfully activated already activated id`() {
        activateId("already-activated", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/activate-id/not-activated/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/activate-id/not-activated/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `activateId - successfully activated not activated id`() {
        activateId("not-activated", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/delete-empty-id/ok/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/delete-empty-id/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteEmptyId - success`() {
        deleteEmptyId("ok", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/delete-empty-id/id-not-empty/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/delete-empty-id/id-not-empty/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteEmptyId - bad request, because id is not empty`() {
        deleteEmptyId("id-not-empty", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/delete-empty-id/id-is-active/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/delete-empty-id/id-is-active/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `deleteEmptyId - bad request, because id is active`() {
        deleteEmptyId("id-is-active", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-serial/not-found-serial/before.xml")
    fun `addSerial - not found serial`() {
        addSerial("not-found-serial", expectedStatus = status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-serial/not-found-active-id/before.xml")
    fun `addSerial - not found active id`() {
        setupSerialInventoryBySerialNumber("102", buildSerialInventory("102", "1-01"))
        addSerial("not-found-active-id", expectedStatus = status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-serial/already-added/before.xml")
    fun `addSerial - bad request because already added`() {
        setupSerialInventoryBySerialNumber("102", buildSerialInventory("102", "1-01"))
        addSerial("already-added", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-serial/added-to-another/before.xml")
    fun `addSerial - bad request because added to another order`() {
        setupSerialInventoryBySerialNumber("102", buildSerialInventory("102", "1-01"))
        addSerial("added-to-another", expectedStatus = status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/add-serial/ok/before.xml")
    @ExpectedDatabase(
        "/controller/optimization-order/add-serial/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `addSerial - successfully`() {
        setupSerialInventoryBySerialNumber("102", buildSerialInventory("102", "1-01"))
        addSerial("ok", expectedStatus = status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/place-serials/from-multiple-ids/before.xml")
    fun `placeSerials - successfully placed from multiple ids`() {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf("100", "101")))
            .thenReturn(
                buildSerialInventories(id = "123", serialNumbers = listOf("100"))
                    + buildSerialInventories(id = "124", serialNumbers = listOf("101"))
            )

        placeSerials("from-multiple-ids", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/place-serials/already-placed/before.xml")
    fun `placeSerials - error because serials already placed`() {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf("100", "101")))
            .thenReturn(buildSerialInventories(loc = "1-01", id = "123", serialNumbers = listOf("100", "101")))

        placeSerials("already-placed", status().isBadRequest)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/start-order/order-not-found/before.xml")
    fun `startOptimizationOrder - not found optimization order`() {
        startOptimizationOrder("order-not-found", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/start-order/empty-order/before.xml")
    fun `startOptimizationOrder - bad request because empty order`() {
        startOptimizationOrder("empty-order", status().isConflict)
    }

    @Test
    @DatabaseSetup("/controller/optimization-order/start-order/ok/before.xml")
    fun `startOptimizationOrder - successfully start and delete empty ids`() {
        startOptimizationOrder("ok", status().isOk)
    }

    private fun getCurrentOrder(expectedContent: ResultMatcher, expectedStatus: ResultMatcher) =
        mockMvc.perform(get("/api/v1/optimization/order"))
            .andExpect(expectedStatus)
            .andExpect(expectedContent)

    private fun getOptimizationOrderIdState(testCase: String, expectedStatus: ResultMatcher) =
        mockMvc.perform(get("/api/v1/optimization/order/1/id/RCP1"))
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("get-id-state", testCase), true))

    private fun addEmptyId(testCase: String, expectedStatus: ResultMatcher, strictJson: Boolean = true) {
        val request = post("/api/v1/optimization/order/empty-id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to "RCP1"))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("add-empty-id", testCase), strictJson))
    }

    private fun addId(testCase: String, expectedStatus: ResultMatcher, strictJson: Boolean = true) {
        val request = post("/api/v1/optimization/order/id")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to "RCP1"))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("add-id", testCase), strictJson))
    }

    private fun placeId(testCase: String, expectedStatus: ResultMatcher) {
        val request = post("/api/v1/optimization/order/1/id/123/place")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("loc" to "1-01"))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("place-id", testCase), true))
    }

    private fun activateId(testCase: String, expectedStatus: ResultMatcher) {
        val request = post("/api/v1/optimization/order/2/id/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("id" to "RCP1"))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("activate-id", testCase), true))
    }

    private fun deleteEmptyId(testCase: String, expectedStatus: ResultMatcher, orderKey: Int = 2, id: String = "RCP1") {
        mockMvc.perform(delete("/api/v1/optimization/order/$orderKey/empty-id/$id"))
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("delete-empty-id", testCase), true))
    }

    private fun addSerial(testCase: String, expectedStatus: ResultMatcher) {
        val request = post("/api/v1/optimization/order/2/uit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("uit" to "102"))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("add-serial", testCase), true))
    }

    private fun placeSerials(testCase: String, expectedStatus: ResultMatcher) {
        val request = post("/api/v1/optimization/order/10/uit/place")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(getJsonRequest("place-serials", testCase)))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("place-serials", testCase), true))
    }

    private fun startOptimizationOrder(testCase: String, expectedStatus: ResultMatcher) {
        mockMvc.perform(post("/api/v1/optimization/order/2/start"))
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("start-order", testCase), true))
    }

    private fun setupIdExists(id: String, value: Boolean) {
        whenever(coreService.isIdExists(id)).thenReturn(value)
    }

    private fun setupSerialInventoriesById(id: String, inventories: List<SerialInventory>) {
        whenever(coreService.getSerialInventoriesById(id)).thenReturn(inventories)
    }

    private fun setupSerialInventoriesById(
        lot: String = "", loc: String = "", id: String = "", serialNumbers: List<String>
    ) {
        whenever(coreService.getSerialInventoriesById(id))
            .thenReturn(buildSerialInventories(lot, loc, id, serialNumbers))
    }


    private fun setupSerialInventoryBySerialNumber(serialNumber: String, serialInventory: SerialInventory?) {
        whenever(coreService.getSerialInventoryBySerialNumber(serialNumber)).thenReturn(serialInventory)
    }

    private fun setupLocLotIdById(id: String, lotLocId: LotLocId) {
        whenever(coreService.getLotLocIdById(id)).thenReturn(listOf(lotLocId))
    }

    private fun setupNestedIds(id: String, nestedIds: List<String>) {
        whenever(coreClient.getChildContainers(id)).thenReturn(GetChildContainersResponse(nestedIds))
    }

    private fun buildSerialInventory(serialNumber: String, loc: String): SerialInventory {
        return SerialInventory.builder().serialNumber(serialNumber).loc(loc).build()
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>
    ): List<SerialInventory> {
        return serialNumbers.map {
            SerialInventory.builder()
                .sku("ROV123")
                .storerKey("465852")
                .serialNumber(it)
                .lot(lot)
                .loc(loc)
                .id(id)
                .quantity(BigDecimal.ONE)
                .build()
        }
    }

    private fun buildLotLocId(
        lot: String = "",
        loc: String = "",
        id: String = "",
        qtyPicked: BigDecimal = BigDecimal.ZERO,
        qtyAllocated: BigDecimal = BigDecimal.ZERO
    ): LotLocId {
        return LotLocId.builder().lot(lot).loc(loc).id(id).qtyAllocated(qtyAllocated).qtyPicked(qtyPicked).build()
    }

    private fun getJsonRequest(testGroup: String, testCase: String): String =
        getFileContent("controller/optimization-order/$testGroup/$testCase/request.json")

    private fun getJsonResponse(testGroup: String, testCase: String): String =
        getFileContent("controller/optimization-order/$testGroup/$testCase/response.json")
}
