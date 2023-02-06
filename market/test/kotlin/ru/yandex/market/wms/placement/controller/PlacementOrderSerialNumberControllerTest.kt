@file:Suppress("SameParameterValue")

package ru.yandex.market.wms.placement.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import mu.KotlinLogging
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.http.MediaType
import org.springframework.jms.core.JmsTemplate
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.model.enums.ItrnSourceType
import ru.yandex.market.wms.common.model.enums.LocationType
import ru.yandex.market.wms.common.spring.dao.entity.Loc
import ru.yandex.market.wms.common.spring.dao.entity.SerialInventory
import ru.yandex.market.wms.common.spring.dao.implementation.LotIdDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.LotLocIdDao
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.core.response.CheckByLocAndSkuResponse
import ru.yandex.market.wms.core.base.request.UpdateSusr7Request
import ru.yandex.market.wms.placement.config.PlacementIntegrationTest
import ru.yandex.market.wms.placement.service.impl.CoreServiceImpl
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants
import java.math.BigDecimal

internal class PlacementOrderSerialNumberControllerTest : PlacementIntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var lotIdDetailDao: LotIdDetailDao

    @MockBean
    @Autowired
    private lateinit var lotLocIdDao: LotLocIdDao

    @MockBean
    @Autowired
    private lateinit var coreService: CoreServiceImpl

    @MockBean
    @Autowired
    private lateinit var defaultJmsTemplate: JmsTemplate

    @BeforeEach
    fun cleanAndSetupDefaults() {
        reset(lotIdDetailDao, lotLocIdDao, coreService)

        whenever(constraintsClient.checkByLocAndSku(anyString(), anyList()))
            .thenReturn(CheckByLocAndSkuResponse(true, emptyList()))
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial check is success`() {
        val orderKey = "1"
        val checkedSerialNumber = "0000000111"
        setupSerialInventoryBySerialNumber(checkedSerialNumber, "LOC", "110")
        checkSuccessSerialBelonging(status().is2xxSuccessful, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial does not exist`() {
        val orderKey = "1"
        val checkedSerialNumber = "0000000113"
        checkSerialAvailability("not-found/serial", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, log record does not exist`() {
        val orderKey = "2"
        val checkedSerialNumber = "0000000214"
        checkSerialAvailability("not-found/log", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial does not belong to order`() {
        val orderKey = "2"
        val checkedSerialNumber = "0000000111"
        setupSerialInventoryBySerialNumber(checkedSerialNumber, "LOC", "110")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        checkSerialAvailability("not-belong", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial already placed`() {
        val orderKey = "2"
        val checkedSerialNumber = "0000000211"
        setupSerialInventoryBySerialNumber(checkedSerialNumber, "LOC", "110")
        checkSerialAvailability("bad-status/already-placed", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial has dropped status`() {
        val orderKey = "2"
        val checkedSerialNumber = "0000000212"
        checkSerialAvailability("bad-status/dropped", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial has lost status`() {
        val orderKey = "2"
        val checkedSerialNumber = "0000000213"
        checkSerialAvailability("bad-status/lost", status().is4xxClientError, orderKey, checkedSerialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial is picked`() {
        val orderKey = "2"
        val pickedSerial = "0000000333"
        setupSerialIsPicked(pickedSerial)
        checkSerialAvailability("picked", status().is4xxClientError, orderKey, pickedSerial)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial is picked twice`() {
        val orderKey = "2"
        val pickedSerial = "0000000333"
        setupErrorWhenPicked(pickedSerial)
        checkSerialAvailability("picked", status().is4xxClientError, orderKey, pickedSerial)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial is fake`() {
        val orderKey = "2"
        val serialNumber = "0000000111"
        setupSerialInventoryBySerialNumber(serialNumber, "LOC", "ID", isFake = true)
        checkSerialAvailability("fake-serial", status().isBadRequest, orderKey, serialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/check-availability/before.xml")
    fun `Check serials, serial in pick location`() {
        val orderKey = "2"
        val serialNumber = "0000000111"
        setupSerialInventoryBySerialNumber(serialNumber, "PICKLOC", "ID")
        setupLoc("PICKLOC", LocationType.PICK)
        checkSerialAvailability("pick-location", status().isBadRequest, orderKey, serialNumber)
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/ok/before.xml")
    @ExpectedDatabase(
        "/controller/placement-serial/add-serial/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add serial, successfully added new serial`() {
        setupSerialInventoryBySerialNumber("777", "LOC")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        addSerial("ok", expectedStatus = status().isOk, serial = "777")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/move-and-delete-id/before.xml")
    @ExpectedDatabase(
        "/controller/placement-serial/add-serial/move-and-delete-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add serial, successfully - moved from another order and deleted empty id`() {
        setupSerialInventoryBySerialNumber("102", "LOC")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        addSerial("move-and-delete-id", expectedStatus = status().isOk, serial = "102")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/move-and-place-id/before.xml")
    @ExpectedDatabase(
        "/controller/placement-serial/add-serial/move-and-place-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Add serial, successfully - moved from another order and set old id placed`() {
        setupSerialInventoryBySerialNumber("102", "LOC")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        addSerial("move-and-place-id", expectedStatus = status().isOk, serial = "102")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/serial-not-found/before.xml")
    fun `Add serial, error because serial not found`() {
        addSerial("serial-not-found", expectedStatus = status().isNotFound, serial = "777")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/pick-loc-type/before.xml")
    fun `Add serial, error because serial in PICK location type`() {
        setupSerialInventoryBySerialNumber("102", "LOC")
        setupLoc("LOC", LocationType.PICK)
        addSerial("pick-loc-type", expectedStatus = status().isBadRequest, serial = "102")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/already-added/before.xml")
    fun `Add serial, error because already added`() {
        setupSerialInventoryBySerialNumber("102", "LOC")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        addSerial("already-added", expectedStatus = status().isBadRequest, serial = "102")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/add-serial/added-to-optim-order/before.xml")
    fun `Add serial, bad request because it is added to optimization order`() {
        setupSerialInventoryBySerialNumber("001", "LOC")
        setupLoc("LOC", LocationType.RECEIPT_TABLE)
        addSerial("added-to-optim-order", status().isBadRequest, serial = "001")
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/place-serials/full-id/before.xml")
    fun `Place serials, successfully placed full id`() {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf("100", "101")))
            .thenReturn(buildSerialInventories(id = "123", serialNumbers = listOf("100", "101")))
        placeSerials("full-id", status().isOk)
        verify(defaultJmsTemplate, times(1))
            .convertAndSend(
                eq(QueueNameConstants.UPDATE_SUSR7),
                isA<UpdateSusr7Request>()
            )
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/place-serials/with-check/before.xml")
    fun `Place serials, successfully placed full id with checked source type`() {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf("100", "101")))
            .thenReturn(buildSerialInventories(id = "123", serialNumbers = listOf("100", "101")))
        placeSerials("with-check", status().isOk)
        verify(coreService, times(1))
            .moveSerialInventoriesToLoc(anyInt(), eq(ItrnSourceType.PLACEMENT_PLACE_ITEM_WITH_CHECK), anyList(),
                eq("PLACEMENT"), eq("1-01"))
    }

    @Test
    @DatabaseSetup("/controller/placement-serial/place-serials/mismatched/before.xml")
    fun `Place serials, error because serials mismatched with balances`() {
        whenever(coreService.getAllSerialInventoriesBySerialNumbers(setOf("100", "101")))
            .thenReturn(
                buildSerialInventories(id = "123", serialNumbers = listOf("100"))
                    + buildSerialInventories(id = "124", serialNumbers = listOf("101"))
            )
        placeSerials("mismatched", status().isBadRequest)
    }


    private fun checkSuccessSerialBelonging(expectedStatus: ResultMatcher, orderKey: String, serialNumber: String) {
        mockMvc.perform(
            post("/api/v1/order/$orderKey/uit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("uit" to serialNumber))
        )
            .andExpect(expectedStatus)
    }

    private fun checkSerialAvailability(
        testCase: String,
        expectedStatus: ResultMatcher,
        orderKey: String,
        serialNumber: String
    ) {
        mockMvc.perform(
            post("/api/v1/order/$orderKey/uit/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("uit" to serialNumber))
        )
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("check-availability", testCase)))
    }

    private fun addSerial(testCase: String, expectedStatus: ResultMatcher, serial: String) {
        val request = post("/api/v1/order/333/uit/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json("uit" to serial))

        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("add-serial", testCase), false))
    }


    private fun placeSerials(testCase: String, expectedStatus: ResultMatcher) {
        val request = post("/api/v1/order/10/uit/place")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json(FileContentUtils.getFileContent("controller/placement-serial/place-serials/common-request.json")))
        mockMvc.perform(request)
            .andExpect(expectedStatus)
            .andExpect(content().json(getJsonResponse("place-serials", testCase), true))
    }

    private fun setupSerialInventoryBySerialNumber(
        serialNumber: String, loc: String, id: String = "ID", isFake: Boolean = false
    ) {
        whenever(coreService.getSerialInventoryBySerialNumber(serialNumber))
            .thenReturn(SerialInventory.builder().serialNumber(serialNumber).loc(loc).id(id).isFake(isFake).build())
    }

    private fun setupLoc(loc: String, locationType: LocationType) {
        whenever(coreService.getLocOrNull(loc))
            .thenReturn(Loc.builder().loc(loc).locationType(locationType).build())
    }

    private fun setupSerialIsPicked(pickedSerial: String) {
        whenever(coreService.getOutboundSerialNumber(pickedSerial))
            .thenReturn(pickedSerial)
    }

    private fun setupErrorWhenPicked(pickedSerial: String) {
        ReflectionTestUtils.setField(coreService, "lotIdDetailDao", lotIdDetailDao)
        ReflectionTestUtils.setField(coreService, "lotLocIdDao", lotLocIdDao)
        ReflectionTestUtils.setField(coreService, "log", KotlinLogging.logger {})

        whenever(coreService.getOutboundSerialNumber(pickedSerial))
            .thenCallRealMethod()
        whenever(lotIdDetailDao.findActiveOutboundByUIT(anyString()))
            .thenThrow(IncorrectResultSizeDataAccessException::class.java)
    }

    private fun buildSerialInventories(
        lot: String = "", loc: String = "PLACEMENT", id: String = "", serialNumbers: List<String>
    ): List<SerialInventory> =
        serialNumbers.map {
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

    private fun getJsonResponse(testGroup: String, testCase: String) =
        FileContentUtils.getFileContent("controller/placement-serial/$testGroup/$testCase/response.json")

}
