package ru.yandex.market.wms.placement.controller

import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.dao.model.PlacementOrderStatus
import ru.yandex.market.wms.placement.model.dto.OrdersInfoResponse
import ru.yandex.market.wms.placement.model.dto.PlacementOrderInfo
import ru.yandex.market.wms.placement.service.CoreService
import ru.yandex.market.wms.shared.libs.querygenerator.EnumerationOrder
import java.math.BigDecimal

class PlacementAdminControllerTest : PlacementIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var coreService: CoreService

    @BeforeEach
    fun clean() {
        reset(coreService)
    }

    @Test
    fun `admin_v1_order No tasks exists`() {
        getInfo("controller/placement-order/info/empty.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/two_empty_orders.xml")
    fun `admin_v1_order Two empty orders`() {
        getInfo("controller/placement-order/info/two_empty_orders.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order Three orders`() {
        getInfo("controller/placement-order/info/three_orders.json")
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order pages test`() {
        val wholeContent = getInfoContent(3).content

        val partialContent = ArrayList<PlacementOrderInfo>(3)
        val response1 = getInfoContent(1)
        partialContent.addAll(response1.content)
        val response2 = getInfoContent(1, 1)
        partialContent.addAll(response2.content)
        val response3 = getInfoContent(1, 2)
        partialContent.addAll(response3.content)

        assertEquals(wholeContent, partialContent)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order filter status test`() {
        val finishedOrders = getInfoContent(100, filter = "status==FINISHED")
        assertEquals(1, finishedOrders.content.size)

        val inProgressOrders = getInfoContent(100, filter = "status==IN_PROGRESS")
        assertEquals(2, inProgressOrders.content.size)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order filter orderkey test`() {
        val finishedOrders = getInfoContent(100, filter = "orderKey==1")
        assertEquals(1, finishedOrders.content.size)

        val inProgressOrders = getInfoContent(100, filter = "orderKey==2")
        assertEquals(1, inProgressOrders.content.size)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order filter user test`() {
        val allOrders = getInfoContent(100, filter = "user==\"TEST\"")
        assertEquals(3, allOrders.content.size)

        val noneOrders = getInfoContent(100, filter = "user==\"TST\"")
        assertEquals(0, noneOrders.content.size)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order filter idCount test`() {
        getInfo(
            "controller/placement-order/info/filtered.json",
            "/admin/v1/order?filter=idCount==\"1 / 1\""
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order filter uitCount test`() {
        getInfo(
            "controller/placement-order/info/filtered.json",
            "/admin/v1/order?filter=uitCount==\"1 / 1\""
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order sorting test`() {
        val descStatus = getInfoContent(3, sort = "status", sortOrder = EnumerationOrder.DESC)
        // I > F => IN_PROGRESS > FINISHED
        assertEquals(PlacementOrderStatus.IN_PROGRESS, descStatus.content[0].status)

        val ascStatus = getInfoContent(3, sort = "status", sortOrder = EnumerationOrder.ASC)
        assertEquals(PlacementOrderStatus.FINISHED, ascStatus.content[0].status)
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order sort by createDate`() {
        val descCreateDate = getInfoContent(3, sort = "createDate", sortOrder = EnumerationOrder.ASC)
        for (i in 0..2) {
            assertEquals(i + 1, descCreateDate.content[i].orderKey)
        }
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order only with lost serials`() {
        getInfo(
            "controller/placement-order/info/only_with_lost.json",
            "/admin/v1/order?withLost=true"
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-order/info/three_orders.xml")
    fun `admin_v1_order get optimization orders`() {
        getInfo(
            "controller/placement-order/info/optimization_orders.json",
            "/admin/v1/order?orderType=OPTIMIZATION"
        )
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/id-details/common.xml")
    fun `admin_v1_order_{orderKey}_id All ids`() {
        testGetIdDetailList("all")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/id-details/common.xml")
    fun `admin_v1_order_{orderKey}_id Only partly placed`() {
        testGetIdDetailList("only-partly-placed", filter = "status==PARTLY_PLACED")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/id-details/common.xml")
    fun `admin_v1_order_{orderKey}_id Only specific id`() {
        testGetIdDetailList("only-specific-id", filter = "id==RCP124")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/id-details/common.xml")
    fun `admin_v1_order_{orderKey}_id Only id whose add date is less than`() {
        testGetIdDetailList("adddate-less-than", filter = "addDate<'2021-09-13 12:47:00'")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/id-details/common.xml")
    fun `admin_v1_order_{orderKey}_id All sort by add date desc`() {
        testGetIdDetailList("sort-by-adddate-desc", sortField = "addDate", order = EnumerationOrder.DESC)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/assign-user/ok/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/assign-user/ok/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_user Successfully assign user`() {
        setupUserExists("some-user")
        testAssignUser("ok", status().isOk, checkResponse = false)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/assign-user/user-not-found/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/assign-user/user-not-found/immutable.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_user Error because user not found`() {
        testAssignUser("user-not-found", status().isNotFound)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/assign-user/exists-active-order/immutable.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/assign-user/exists-active-order/immutable.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_user Error because user already has active order`() {
        setupUserExists("some-user")
        testAssignUser("exists-active-order", status().isBadRequest)
    }

    @Test
    fun `admin_v1_order_{orderKey}_id_{id} Empty response`() {
        testGetSerialDetailList("empty")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/serial-details/common.xml")
    fun `admin_v1_order_{orderKey}_id_{id} All`() {
        testGetSerialDetailList("all")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/serial-details/common.xml")
    fun `admin_v1_order_{orderKey}_id_{id} Filter by serial`() {
        testGetSerialDetailList("first-serial", filter = "serial==001")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/serial-details/common.xml")
    fun `admin_v1_order_{orderKey}_id_{id} Filter by placeDate`() {
        // UTC dates in DB
        // MSK dates in API
        testGetSerialDetailList("newest", filter = "placeDate>'2021-09-13 12:50:00'")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/serial-details/common.xml")
    fun `admin_v1_order_{orderKey}_id_{id} Order by serial desc`() {
        // UTC dates in DB
        // MSK dates in API
        testGetSerialDetailList("order-by", sortField = "serial", order = EnumerationOrder.DESC)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/preview/ok/before.xml")
    fun `admin_v1_order_{orderKey} Get order preview result is ok`() {
        testGetOrderPreview("ok")
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/synchronize/delete-placed/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/synchronize/delete-placed/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_synchronize delete placed serials`() {
        setupGetSerialInventoriesBySerialNumbers(
            mapOf("" to listOf("100", "101"), "RCP100" to listOf("102"))
        )
        testSynchronizeOrder("delete-placed", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/synchronize/delete-id/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/synchronize/delete-id/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_synchronize delete serials and id`() {
        setupGetSerialInventoriesBySerialNumbers(
            mapOf(
                "" to listOf("100"),
                "RCP999" to listOf("101", "200"),
                "RCP200" to listOf("201")
            )
        )
        testSynchronizeOrder("delete-id", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/synchronize/finish-task/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/synchronize/finish-task/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_synchronize delete serials and finish task`() {
        setupGetSerialInventoriesBySerialNumbers(mapOf("" to listOf("100", "101", "200")))
        testSynchronizeOrder("delete-id", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/synchronize/delete-not-found/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/synchronize/delete-not-found/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_synchronize delete serials not found in SerialInventory`() {
        setupGetSerialInventoriesBySerialNumbers(mapOf("RCP100" to listOf("101")))
        testSynchronizeOrder("delete-not-found", status().isOk)
    }

    @Test
    @DatabaseSetup("/controller/placement-admin/synchronize/delete-outsiders/before.xml")
    @ExpectedDatabase(
        value = "/controller/placement-admin/synchronize/delete-outsiders/after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    fun `admin_v1_order_{orderKey}_synchronize delete serials outside placement loc `() {
        val serialInventories = buildSerialInventories(id = "RCP100", serialNumbers = listOf("100", "101"))
        val outsideSerialInventories =
            buildSerialInventories(id = "RCP200", loc = "NOT_PLACEMENT_LOC", serialNumbers = listOf("200", "201"))

        whenever(coreService.getAllSerialInventoriesBySerialNumbers(anyCollection()))
            .thenReturn(serialInventories + outsideSerialInventories)

        testSynchronizeOrder("delete-outsiders", status().isOk)
    }

    private fun getInfo(responseJsonPath: String, url: String = "/admin/v1/order") =
        mockMvc.perform(get(url))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(FileContentUtils.getFileContent(responseJsonPath), false))
            .andReturn()

    private fun getInfoContent(
        limit: Int, offset: Int = 0, filter: String? = null,
        sort: String = "orderKey", sortOrder: EnumerationOrder = EnumerationOrder.ASC
    ): OrdersInfoResponse {
        val request = get("/admin/v1/order")
            .param("limit", limit.toString())
            .param("offset", offset.toString())
            .param("sort", sort)
            .param("order", sortOrder.order)
        filter?.let { request.param("filter", it) }

        return mapper.readValue(
            mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful)
                .andReturn().response.contentAsString,
            jacksonTypeRef()
        )
    }

    private fun testGetIdDetailList(
        testCase: String,
        filter: String = "",
        sortField: String = "",
        order: EnumerationOrder = EnumerationOrder.ASC
    ) {
        val request = get("/admin/v1/order/100/id")
            .param("filter", filter)
            .param("sort", sortField)
            .param("order", order.order)
        val responseJsonPath = "controller/placement-admin/id-details/$testCase/response.json"

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(
                content().json(FileContentUtils.getFileContent(responseJsonPath), true)
            )
    }

    private fun testAssignUser(testCase: String, expectedStatus: ResultMatcher, checkResponse: Boolean = true) {
        val request = post("/admin/v1/order/123/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("user" to "some-user"))

        val responseJsonPath = "controller/placement-admin/assign-user/$testCase/response.json"

        val result = mockMvc.perform(request).andExpect(expectedStatus)
        if (checkResponse) {
            result.andExpect(content().json(FileContentUtils.getFileContent(responseJsonPath)))
        }
    }

    private fun testGetSerialDetailList(
        testCase: String,
        filter: String = "",
        sortField: String = "",
        order: EnumerationOrder = EnumerationOrder.ASC
    ) {
        val request = get("/admin/v1/order/100/id/RCP123")
            .param("filter", filter)
            .param("sort", sortField)
            .param("order", order.order)
        val responseJsonPath = "controller/placement-admin/serial-details/$testCase/response.json"

        mockMvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(
                content().json(FileContentUtils.getFileContent(responseJsonPath), true)
            )
    }

    private fun testGetOrderPreview(testCase: String) {
        val responseJsonPath = "controller/placement-admin/preview/$testCase/response.json"
        mockMvc.perform(get("/admin/v1/order/1"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(FileContentUtils.getFileContent(responseJsonPath), true)
            )
    }

    private fun testSynchronizeOrder(testCase: String, expectedStatus: ResultMatcher, expectResponse: Boolean = false) {
        val resultActions = mockMvc.perform(post("/admin/v1/order/99/synchronize"))
            .andExpect(expectedStatus)

        if (expectResponse) {
            val responseJsonPath = "controller/placement-admin/synchronize/$testCase/response.json"
            resultActions.andExpect(content().json(FileContentUtils.getFileContent(responseJsonPath), true))
        }
    }

    private fun setupUserExists(user: String) {
        whenever(coreService.isUserExists(user)).thenReturn(true)
    }

    private fun setupGetSerialInventoriesBySerialNumbers(serialNumbersById: Map<String, List<String>>) {
        setupGetSerialInventoriesBySerialNumbers(
            serialNumbersById.flatMap { (id, serialNumbers) ->
                buildSerialInventories(id = id, serialNumbers = serialNumbers)
            }
        )
    }

    private fun setupGetSerialInventoriesBySerialNumbers(serialInventories: List<SerialInventory>) {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(anyCollection()))
            .thenReturn(serialInventories)
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>
    ): List<SerialInventory> {
        return serialNumbers.map {
            SerialInventory.builder()
                .serialNumber(it)
                .lot(lot)
                .loc(loc)
                .id(id)
                .quantity(BigDecimal.ONE)
                .build()
        }
    }
}
