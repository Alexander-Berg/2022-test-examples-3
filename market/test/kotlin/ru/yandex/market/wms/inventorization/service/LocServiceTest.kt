package ru.yandex.market.wms.inventorization.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.lang.Nullable
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inventorization.core.model.ShortInventoryTaskRequest
import ru.yandex.market.wms.inventorization.dao.InventoryTaskDao
import java.sql.ResultSet
import java.time.Instant

class LocServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var inventoryTaskDao: InventoryTaskDao

    @Autowired
    private lateinit var locService: LocService

    private data class InventoryTaskDetail(
        val taskDetailKey: String,
        val taskType: String,
        val fromLoc: String,
        val logicalFromLoc: String,
        val status: String,
        val sourceType: String,
        val sourceKey: String,
        val addWho: String,
        val addDate: String,
        val editWho: String,
        val editDate: String
    ) {

        fun assertTask(queryId: String?) {
            assertEquals("PHY", taskType);
            assertEquals(queryId, sourceKey);
            assertEquals("C4-10-0001", fromLoc);
            assertEquals("C4-10-0001", logicalFromLoc);
            assertEquals("anonymousUser", addWho);
            assertEquals("2020-04-01 12:34:56.789", addDate);
            assertEquals("anonymousUser", editWho);
            assertEquals("2020-04-01 12:34:56.789", editDate);
        }
    }

    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/inventory.xml"],
        connection = "wmwhseConnection"
    )
    @ExpectedDatabase(
        value = "/inventory-for-shorts/happy/after_inventory.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testInventoryTask() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        val taskDetails: List<InventoryTaskDetail> = inventoryTaskDetailsFromPicking()
        val inventoryTaskKeys: List<String> = pickToInventoriesKeys()
        assertAll(
            { assertEquals(1, taskDetails.size) },
            { assertEquals("0", taskDetails[0].status) },
            { taskDetails[0].assertTask(inventoryTaskDao.getInventoryQueryIdByReason()) },
            { assertEquals(1, inventoryTaskKeys.size) },
            { inventoryTaskKeys.forEach { assertEquals(taskDetails[0].taskDetailKey, it) } }
        )
    }

    /**
     * Не создается задание на инвентаризацию, но приложение не падает.
     */
    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/no-area/before.xml", "/inventory-for-shorts/no-area/inventory.xml"],
        connection = "wmwhseConnection"
    )
    fun testNoAreaDetail() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        val taskDetails: List<InventoryTaskDetail> = inventoryTaskDetailsFromPicking()
        val inventoryTaskKeys: List<String> = pickToInventoriesKeys()
        assertAll(
            { assertTrue(taskDetails.isEmpty()) },
            { assertTrue(inventoryTaskKeys.isNotEmpty()) },
            { assertNull(inventoryTaskKeys.iterator().next()) },
        )
    }

    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/inventory_disabled.xml"],
        connection = "wmwhseConnection"
    )
    fun testDisableInventory() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        Assertions.assertTrue(inventoryTaskDetailsFromPicking().isEmpty())
        assertEquals(1, pickToInventoriesKeys().size)
        Assertions.assertNull(pickToInventoriesKeys()[0])
    }

    /**
     * INVENTORYCOUNT_QUERY не должен создаться, если уже существует.
     */
    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/inventory.xml", "/inventory-for-shorts/happy/inventory_query.xml"],
        connection = "wmwhseConnection"
    )
    @ExpectedDatabase(
        value = "/inventory-for-shorts/happy/after_inventory.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testInventoryTaskQueryExists() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        val taskDetails: List<InventoryTaskDetail> = inventoryTaskDetailsFromPicking()
        val inventoryTaskKeys: List<String> = pickToInventoriesKeys()
        assertAll(
            { assertEquals(1, taskDetails.size) },
            { assertEquals("0", taskDetails[0].status) },
            { taskDetails[0].assertTask(inventoryTaskDao.getInventoryQueryIdByReason()) },
            { assertEquals(1, inventoryTaskKeys.size) },
            { inventoryTaskKeys.forEach { assertEquals(taskDetails[0].taskDetailKey, it) } }
        )
    }

    /**
     * Старые задания отменены, новое создано.
     */
    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/old_inventories.xml"],
        connection = "wmwhseConnection"
    )
    fun testCancelOldInventories() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        val taskDetails: List<InventoryTaskDetail> = inventoryTaskDetailsFromPicking() // новые задания инв-ии
        val allInvTasks: List<InventoryTaskDetail> = inventoryTaskDetails(null, "PHY")
        val inventoryTaskKeys: List<String> = pickToInventoriesKeys()
        assertAll(
            { assertEquals(1, taskDetails.size) },
            { assertEquals("0", taskDetails[0].status) },
            { taskDetails[0].assertTask(inventoryTaskDao.getInventoryQueryIdByReason()) },
            { assertEquals(1, inventoryTaskKeys.size) },
            { inventoryTaskKeys.forEach { assertEquals(taskDetails[0].taskDetailKey, it) } },
            { assertEquals("X", allInvTasks.find { "PHY0001" == it.taskDetailKey }?.status) },
            { assertEquals("X", allInvTasks.find { "PHY0002" == it.taskDetailKey }?.status) }
        )
    }

    /**
     * Дважды вызываем
     * [PickingUpdateLostService.processUpdateLostItem]
     * Должно быть одно задание на инвентаризацию, один INVENTORYCOUNT_QUERY
     * и две записи в PICK_TO_INVENTORY_TASK с одним заданием.
     * Задание на инвентаризацию не отменится, т.к. оно недавнее.
     */
    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/inventory.xml"],
        connection = "wmwhseConnection"
    )
    @ExpectedDatabase(
        value = "/inventory-for-shorts/happy/after_inventory_2picks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testTwiceLost() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0001",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000004",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
        val taskDetails: List<InventoryTaskDetail> = inventoryTaskDetailsFromPicking()
        val inventoryTaskKeys: List<String> = pickToInventoriesKeys()
        assertAll(
            { assertEquals(1, taskDetails.size) },
            {
                val maybeNew = taskDetails.find { "0" == it.status }
                Assertions.assertNotNull(maybeNew)
                maybeNew?.assertTask(inventoryTaskDao.getInventoryQueryIdByReason())
            },
            { assertEquals(2, inventoryTaskKeys.size) },
            { inventoryTaskKeys.forEach { assertEquals(taskDetails[0].taskDetailKey, it) } }
        )
    }

    @Test
    @DatabaseSetup(
        value = ["/inventory-for-shorts/happy/before.xml", "/inventory-for-shorts/happy/inventory.xml", "/inventory-for-shorts/happy/pick2inventory.xml"],
        connection = "wmwhseConnection"
    )
    @ExpectedDatabase(
        value = "/inventory-for-shorts/happy/pick2inventory_updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "wmwhseConnection"
    )
    fun testLostPick2InventoryExists() {
        locService.createInventoryTask(
            ShortInventoryTaskRequest(
                taskDetailKey = "TDK0003",
                loc = "C4-10-0001",
                sku = "ROV0000000000000000002",
                lot = "L5",
                qty = 1,
                user = "anonymousUser",
                timeShorted = Instant.now()
            )
        )
    }

    private fun inventoryTaskDetailsFromPicking(): List<InventoryTaskDetail> {
        return inventoryTaskDetails("PickingInventorizaion", null)
    }

    private fun inventoryTaskDetails(
        @Nullable sourceType: String?,
        @Nullable taskType: String?
    ): List<InventoryTaskDetail> {
        var sql = "select taskDetailKey, taskType, fromLoc, logicalFromLoc, status, " +
            "sourceType, sourceKey, addWho, addDate, editWho, editDate " +
            "from taskDetail " +
            "where 1=1 "
        if (sourceType != null) {
            sql += "AND sourceType = '$sourceType' "
        }
        if (taskType != null) {
            sql += "AND taskType = '$taskType' "
        }
        return jdbc.query(sql)
        { rs: ResultSet, _: Int ->
            InventoryTaskDetail(
                taskDetailKey = rs.getString("taskDetailKey"),
                taskType = rs.getString("taskType"),
                fromLoc = rs.getString("fromLoc"),
                logicalFromLoc = rs.getString("logicalFromLoc"),
                status = rs.getString("status"),
                sourceType = rs.getString("sourceType"),
                sourceKey = rs.getString("sourceKey"),
                addWho = rs.getString("addWho"),
                addDate = rs.getString("addDate"),
                editWho = rs.getString("editWho"),
                editDate = rs.getString("editDate")
            )
        }
    }

    private fun pickToInventoriesKeys(): List<String> {
        return jdbc.queryForList("select INVENTORYTASKKEY from PICK_TO_INVENTORY_TASK", String::class.java)
    }
}
