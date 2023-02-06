package ru.yandex.market.wms.inventorization.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.enums.WmsErrorCode
import ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent
import ru.yandex.market.wms.inventorization.dto.EndPalletTaskRequest
import ru.yandex.market.wms.inventorization.dto.InventoryByPalletRequest
import ru.yandex.market.wms.inventorization.dto.InventoryByQtyRequest
import ru.yandex.market.wms.inventorization.dto.enums.EndPalletTaskResult
import ru.yandex.market.wms.inventorization.dto.enums.EndPalletTaskStatus
import ru.yandex.market.wms.inventorization.dto.enums.InventoryStatus
import ru.yandex.market.wms.inventorization.entity.InventorizationTask
import ru.yandex.market.wms.inventorization.entity.enums.SubTaskType

private const val GET_CURRENT_PALLET = "/task/pallet/current"
private const val PALLET_TASK_BY_LOC = "/task/pallet/by-loc/C4-10-0001"
private const val GET_CURRENT_PALLET01 = "/task/pallet/by-loc/1-01"
private const val END_TASK = "/task/0000000999/endTask"
private const val QTY_URL = "/task/0000000999/pallet/qty"
private const val PALLET_URL = "/task/0000000999/by-pallet"


class PalletTaskControllerTest : IntegrationTest() {
    private val mapper: ObjectMapper = ObjectMapper()

    @Test
    @DatabaseSetup(value = ["/get-current-pallet-task/good/before.xml"])
    fun getCurrentInventorizationTaskGoodTest(): Unit {
        mockMvc.perform(get(GET_CURRENT_PALLET))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("get-current-pallet-task/good/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/get-current-pallet-task/no-task/before.xml"])
    fun getCurrentInventorizationTaskNoTask1Test(): Unit {
        mockMvc.perform(get(GET_CURRENT_PALLET))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andReturn()
    }

    @Test
    fun getCurrentInventorizationTaskNoTask2Test(): Unit {
        mockMvc.perform(get(GET_CURRENT_PALLET))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/good/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/good/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getAndUpdateInventorizationTaskByLocGoodTest(): Unit {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("task-by-loc/good/response.json"), false))
            .andReturn();
        mockMvc.perform(put(PALLET_TASK_BY_LOC)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePalletTaskByLocRq(InventorizationTask(
                loc = "C4-10-0001",
                assignmentNumber = "1",
                taskdetailKey = "TDK0001",
                subTaskType = SubTaskType.PLT_COUNT,
                attempts = 0,
                inventoryzationId = "1",
                status = "S",
                user = "anonymousUser"
            ))))
            .andExpect(status().is2xxSuccessful)
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/good-when-allowed-zones-are-set/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/good-when-allowed-zones-are-set/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getAndUpdateInventorizationTaskByLocWhenAllowedZonesAreSetGoodTest() {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().is2xxSuccessful)
            .andExpect(content().json(getFileContent("task-by-loc/good-when-allowed-zones-are-set/response.json"), false))
            .andReturn();
        mockMvc.perform(put(PALLET_TASK_BY_LOC)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updatePalletTaskByLocRq(InventorizationTask(
                loc = "C4-10-0001",
                assignmentNumber = "1",
                taskdetailKey = "TDK0001",
                subTaskType = SubTaskType.PLT_COUNT,
                attempts = 0,
                inventoryzationId = "1",
                status = "S",
                user = "anonymousUser"
            ))))
            .andExpect(status().is2xxSuccessful)
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/bad-loc-type/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/bad-loc-type/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocBadLocTypeTest(): Unit {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/bad-loc-type/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/bad-loc-type-when-allowed-zones-are-set/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/bad-loc-type-when-allowed-zones-are-set/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocWhenAllowedZonesAreSetBadLocTypeTest() {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/bad-loc-type-when-allowed-zones-are-set/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/no-such-loc/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/no-such-loc/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocNoSuchLocTest(): Unit {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/no-such-loc/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/no-such-loc-when-allowed-zones-are-set/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/no-such-loc-when-allowed-zones-are-set/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocWhenAllowedZonesAreSetNoSuchLocTest() {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/no-such-loc-when-allowed-zones-are-set/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/not-allowed-area/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/not-allowed-area/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocNotAllwedAreaTest(): Unit {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/not-allowed-area/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/not-allowed-zones/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/not-allowed-zones/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocNotAllwedZonesTest() {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().`is`(400))
            .andExpect(content().json(getFileContent("task-by-loc/no-task/not-allowed-zones/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/old-inventorization-started/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/old-inventorization-started/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocOldInventorizationStartedTest(): Unit {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().isNotFound)
            .andExpect(content().json(getFileContent("task-by-loc/no-task/old-inventorization-started/response.json"), false))
            .andReturn()
    }

    @Test
    @DatabaseSetup(value = ["/task-by-loc/no-task/old-inventorization-started-when-allowed-zones-are-set/before.xml"])
    @ExpectedDatabase(value = "/task-by-loc/no-task/old-inventorization-started-when-allowed-zones-are-set/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun getInventorizationTaskByLocWhenAllowedZonesAreSetOldInventorizationStartedTest() {
        mockMvc.perform(get(PALLET_TASK_BY_LOC))
            .andExpect(status().isNotFound)
            .andExpect(content().json(getFileContent("task-by-loc/no-task/old-inventorization-started-when-allowed-zones-are-set/response.json"), false))
            .andReturn()
    }

    @Test
    fun updateInventorizationTaskByLocWithUitSubtask() {
        mockMvc.perform(put(PALLET_TASK_BY_LOC)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("task-by-loc/update-with-by-uit/request.json")))
            .andExpect(status().isBadRequest)
            .andExpect(content().json(getFileContent("task-by-loc/update-with-by-uit/response.json")))
    }


    @Test
    @DatabaseSetup(value = [
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml",
        "/by-pallet/serialinventory/serial.xml"
    ])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/status_by_uit_step1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test balance mismatch, no second SKU in serial, two SKU in LOTxLOCxID`() {
        val response = mockMvc.perform(
            put(GET_CURRENT_PALLET01)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePalletTaskByLocRq(InventorizationTask(
                    loc = "1-01",
                    assignmentNumber = "0000000002",
                    taskdetailKey = "0000000999",
                    subTaskType = SubTaskType.DEFAULT,
                    attempts = 1,
                    inventoryzationId = "0000000022",
                    status = "S",
                    user = "anonymousUser"
                ))))
            .andExpect(status().is5xxServerError)
            .andReturn()
            .response
            .contentAsString
        assertEquals(WmsErrorCode.BALANCE_EXCEPTION.entityName,
            ObjectMapper().readTree(response)["wmsErrorCode"].toString().replace("\"", ""))
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/lotxlocxid/lot.xml",
        "/by-pallet/serialinventory/first_item_wrong_pallet.xml"
    ])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/status_by_uit_step1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test balance mismatch, single SKU, different IDs`() {
        val response = mockMvc.perform(
            put(GET_CURRENT_PALLET01)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePalletTaskByLocRq(InventorizationTask(
                    loc = "1-01",
                    assignmentNumber = "0000000002",
                    taskdetailKey = "0000000999",
                    subTaskType = SubTaskType.DEFAULT,
                    attempts = 1,
                    inventoryzationId = "0000000022",
                    status = "S",
                    user = "anonymousUser"
                ))))
            .andExpect(status().is5xxServerError)
            .andReturn()
            .response
            .contentAsString
        assertEquals(WmsErrorCode.BALANCE_EXCEPTION.entityName,
            ObjectMapper().readTree(response)["wmsErrorCode"].toString().replace("\"", ""))
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/nsqlconfig/config.xml",
        "/task-by-loc/balance/task.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml",
        "/by-pallet/serialinventory/serial.xml", "/by-pallet/serialinventory/first_pallet_second_item.xml"
    ])
    fun `Test balance OK, two SKUs, single pallet`() {
        mockMvc.perform(
            put(GET_CURRENT_PALLET01)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePalletTaskByLocRq(InventorizationTask(
                    loc = "1-01",
                    assignmentNumber = "0000000002",
                    taskdetailKey = "0000000999",
                    subTaskType = SubTaskType.DEFAULT,
                    attempts = 1,
                    inventoryzationId = "0000000022",
                    status = "0",
                    user = "anonymousUser"
                ))))
            .andExpect(status().isOk)
            .andReturn()
    }


    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/faulty_tasks.xml", "/by-pallet/nsqlconfig/config.xml"])
    fun testValidationNoTaskDetail() {
        // no taskdetail
        mockMvc.perform(post("/task/0000000000/by-pallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(inventoryByPalletRq("1-01", "PLT123")))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/faulty_tasks.xml", "/by-pallet/nsqlconfig/config.xml"])
    fun testValidationNoStatus() {
        // no status
        mockMvc.perform(post("/task/0000000988/by-pallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(inventoryByPalletRq("1-01", "PLT123")))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/faulty_tasks.xml", "/by-pallet/nsqlconfig/config.xml"])
    fun testValidationNoUser() {
        // no user
        mockMvc.perform(post("/task/0000000989/by-pallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(inventoryByPalletRq("1-01", "PLT123")))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/faulty_tasks.xml", "/by-pallet/nsqlconfig/config.xml"])
    fun testValidationAttemptsExceeded() {
        // steps are exceeded
        mockMvc.perform(post("/task/0000000990/by-pallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(inventoryByPalletRq("1-01", "PLT123")))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/faulty_tasks.xml", "/by-pallet/nsqlconfig/config.xml"])
    fun testValidationWrongSubtask() {
        // subtask = BY_UIT
        mockMvc.perform(post("/task/0000000991/by-pallet")
            .contentType(MediaType.APPLICATION_JSON)
            .content(inventoryByPalletRq("1-01", "PLT123")))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/task.xml",  "/by-pallet/nsqlconfig/config.xml"])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/task_after_reset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test empty balance in pallet inventory`() {
        val response = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"), status().is2xxSuccessful)
        assertStatus(response, InventoryStatus.EMPTY)
        assertEmptyInventoryCountLog()
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/taskdetail/task.xml", "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/nsqlconfig/config.xml"])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/task_after_reset.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test balance gap in pallet inventory`() {
        val response = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"), status().is5xxServerError)
        assertEquals(WmsErrorCode.BALANCE_EXCEPTION.entityName,
            ObjectMapper().readTree(response)["wmsErrorCode"].toString().replace("\"", ""))
        assertEmptyInventoryCountLog()
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test successful pallet inventory`() {
        val response = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"))
        assertStatus(response, InventoryStatus.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/lotxlocxid/lot.xml",
        "/by-pallet/inventorycount/single_entry.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test pallet inventory with inventorycount rows already present`() {
        val response = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"))
        assertStatus(response, InventoryStatus.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/lotxlocxid/lot.xml",
        "/by-pallet/touched/touched_stored_pallet.xml"])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/status_plt_cnt.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test pallet inventory fail cause the pallet is touched`() {
        val response = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"))
        assertStatus(response, InventoryStatus.REQUIRED_COUNTING)
        assertEmptyInventoryCountLog()

        val responseItems = ObjectMapper().readTree(response)["items"]
        assertEquals("Single item is expected in response for multiple lots with same SKU",
            1, responseItems.size())
        assertEquals("Lots (поставки) mismatch",
            "\"0000000031, 0000000032\"", responseItems[0]["lot"].toString())
    }

    @Test
    @DatabaseSetup(value = ["/by-pallet/with-containers/common.xml", "/by-pallet/with-containers/ok/before.xml"])
    @ExpectedDatabase(
        value = "/by-pallet/with-containers/ok/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Test pallet with containers inventory, one empty, successful`() {
        mockMvc.perform(post(PALLET_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("by-pallet/with-containers/request.json")))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("by-pallet/with-containers/ok/response.json"), true))
    }


    @Test
    @DatabaseSetup("/by-pallet/with-containers/common.xml")
    @ExpectedDatabase(
        value = "/by-pallet/with-containers/all-empty/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Test pallet with containers inventory, all empty`() {
        mockMvc.perform(post(PALLET_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("by-pallet/with-containers/request.json")))
            .andExpect(status().isOk)
            .andExpect(content().json(
                getFileContent("by-pallet/with-containers/all-empty/response.json"), true))
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/with-containers/common.xml",
        "/by-pallet/with-containers/balance-gap/before.xml"
    ])
    @ExpectedDatabase(
        value = "/by-pallet/with-containers/balance-gap/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Test pallet with containers inventory, balances gap`() {
        mockMvc.perform(post(PALLET_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("by-pallet/with-containers/request.json")))
            .andExpect(status().isInternalServerError)
            .andExpect(content().json(
                getFileContent("by-pallet/with-containers/balance-gap/response.json"), true))
    }


    @Test
    @DatabaseSetup(value = ["/by-pallet/with-containers/common.xml", "/by-pallet/with-containers/touched/before.xml"])
    @ExpectedDatabase(
        value = "/by-pallet/with-containers/touched/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `Test pallet with containers inventory, touched`() {
        mockMvc.perform(post(PALLET_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("by-pallet/with-containers/request.json")))
            .andExpect(status().isBadRequest)
            .andExpect(content().json(
                getFileContent("by-pallet/with-containers/touched/response.json"), true))
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/by-pallet/taskdetail/finished.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun `Test successful qty inventory of single item`() {
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(response, InventoryStatus.OK)

        val finishRs = post(END_TASK, finishInventoryRequest())
        assertStatus(finishRs, EndPalletTaskResult.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/inventorycount/single_pallet_single_item.xml",
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test successful qty inventory, all the rows in inventorycount_log are already present`() {
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(response, InventoryStatus.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml", "/by-pallet/serialinventory/first_pallet_second_item.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test successful qty inventory first of two SKUs in same pallet`() {
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(response, InventoryStatus.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml", "/by-pallet/serialinventory/first_pallet_second_item.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_two_items.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test successful qty inventory, two SKUs in one pallet`() {
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(response, InventoryStatus.OK)

        val response2 = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244080", "456456456", 2))
        assertStatus(response2, InventoryStatus.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    @ExpectedDatabase(value = "/by-pallet/taskdetail/status_by_uit.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test fail qty inventory single item`() {
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", -3))
        assertStatus(response, InventoryStatus.REQUIRED_UIT_SCAN)
        assertEmptyInventoryCountLog()
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml", "/by-pallet/serialinventory/first_pallet_second_item.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/by-pallet/inventorycount/single_pallet_single_item.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/by-pallet/taskdetail/status_by_uit.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun `Test successful qty inventory fails, single pallet`() {
        // в /by-pallet/taskdetail/task.xml STEPNUMBER=1
        val response = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(response, InventoryStatus.OK)
        // STEPNUMBER=0 после успешной инвентаризации товара (sku)

        val response2 = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244080", "456456456", -2))
        assertStatus(response2, InventoryStatus.REQUIRED_COUNTING) // STEPNUMBER = 1
        val response3 = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244080", "456456456", 3))
        assertStatus(response3, InventoryStatus.REQUIRED_UIT_SCAN) // STEPNUMBER = 2

        // правильное кол-во, попытки исчерпаны
        mockMvc.perform(
            post(QTY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244080", "456456456", 2)))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml", "/by-pallet/serialinventory/first_pallet_second_item.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml", "/by-pallet/lotxlocxid/first_pallet_second_item.xml"])
    @ExpectedDatabases(
        ExpectedDatabase(value = "/by-pallet/inventorycount/lost_second_sku_first_ok.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED),
        ExpectedDatabase(value = "/by-pallet/taskdetail/finished.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    )
    fun `Test qty inventory first SKU success, second SKU lost`() {
        val qtySuccess = post(QTY_URL, inventoryByQtyRq("1-01", "PLT123", "ROV0000000001002244084", "123123123", 3))
        assertStatus(qtySuccess, InventoryStatus.OK)

        val finishForce = post(END_TASK, finishInventoryRequest(true))
        assertStatus(finishForce, EndPalletTaskResult.OK)
    }

    @Test
    fun `Test stop non existent pallet task`() {
        mockMvc.perform(post(END_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("end-task/stop-request.json")))
            .andExpect(status().isBadRequest)
            .andExpect(content().json(getFileContent("end-task/not-found/response.json"), true))
    }

    @Test
    @DatabaseSetup("/end-task/stop-ok/before.xml")
    @ExpectedDatabase(value = "/end-task/stop-ok/after.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test stop pallet task successfully`() {
        mockMvc.perform(post(END_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("end-task/stop-request.json")))
            .andExpect(status().isOk)
            .andExpect(content().json(getFileContent("end-task/ok-response.json"), true))
    }

    @Test
    @DatabaseSetup("/end-task/stop-wrong-status/before.xml")
    fun `Test stop pallet task with wrong status`() {
        mockMvc.perform(post(END_TASK)
            .contentType(MediaType.APPLICATION_JSON)
            .content(getFileContent("end-task/stop-request.json")))
            .andExpect(status().isBadRequest)
            .andExpect(content().json(getFileContent("end-task/stop-wrong-status/response.json"), true))
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    fun `Test cannot finish incomplete task without force flag`() {
        val response = post(END_TASK, finishInventoryRequest(false))
        assertStatus(response, EndPalletTaskResult.NEED_CONFIRMATION)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/lost_first_pallet.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test finish task with no items inventorized and all in lost`() {
        val response = post(END_TASK, finishInventoryRequest(true))
        assertStatus(response, EndPalletTaskResult.OK)
    }

    @Test
    @DatabaseSetup(value = [
        "/by-pallet/taskdetail/task.xml",
        "/by-pallet/nsqlconfig/config.xml",
        "/by-pallet/lotxlocxid/lot.xml",
        "/by-pallet/serialinventory/serial.xml",
        "/by-pallet/serialinventory/second_pallet.xml"])
    @ExpectedDatabase(value = "/by-pallet/inventorycount/lost_second_pallet_first_ok.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun `Test first pallet is inventorized but second one is lost`() {
        val byPalletRs = post(PALLET_URL, inventoryByPalletRq("1-01", "PLT123"))
        assertStatus(byPalletRs, InventoryStatus.OK)

        val finishResponseStr = post(END_TASK, finishInventoryRequest(true))
        assertStatus(finishResponseStr, EndPalletTaskResult.OK)
    }

    private fun assertEmptyInventoryCountLog() {
        val cnt = jdbc.query("select count(*) as cnt from inventorycount_log", ResultSetExtractor { rs ->
            if (rs.next()) rs.getInt("cnt") else throw AssertionError("Cannot count inventorycount_log")
        })
        assertEquals("table inventorycount_log is expected to be empty", 0, cnt)
    }

    // compares json field "status" with given value
    private fun <T : Enum<T>> assertStatus(json: String, status: Enum<T>) {
        val response = ObjectMapper().readTree(json)
        assertEquals(status.name, response["status"].toString().replace("\"", ""))
    }

    private fun post(url: String, content: String): String = post(url, content, status().isOk)
    private fun post(url: String, content: String, expectedStatus: ResultMatcher): String =
        mockMvc.perform(
            post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
            .andExpect(expectedStatus)
            .andReturn()
            .response
            .contentAsString

    private fun inventoryByPalletRq(loc: String, id: String): String =
            mapper.writeValueAsString(InventoryByPalletRequest(loc, id))
    private fun inventoryByQtyRq(loc: String, id: String, sku: String, storerKey: String, qty: Int): String =
            mapper.writeValueAsString(InventoryByQtyRequest(loc, id, sku, storerKey, qty.toBigDecimal()))
    private fun finishInventoryRequest(force: Boolean = false): String =
            mapper.writeValueAsString(EndPalletTaskRequest(EndPalletTaskStatus.FINISH, force))
    private fun updatePalletTaskByLocRq(task: InventorizationTask) = mapper.writeValueAsString(task)
}
