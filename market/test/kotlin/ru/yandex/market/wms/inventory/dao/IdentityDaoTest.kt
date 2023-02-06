package ru.yandex.market.wms.inventory.dao

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration

import ru.yandex.market.wms.inventory.AbstractJdbcTest
import ru.yandex.market.wms.inventory.model.entity.IdentityEntity
import ru.yandex.market.wms.inventory.model.entity.ItemEntity
import ru.yandex.market.wms.inventory.model.entity.LocEntity
import ru.yandex.market.wms.inventory.model.entity.LogEntity
import ru.yandex.market.wms.inventory.model.entity.LogisticPointEntity
import ru.yandex.market.wms.inventory.model.entity.TaskEntity
import ru.yandex.market.wms.inventory.model.enums.IdentityType
import ru.yandex.market.wms.inventory.model.enums.LocType
import ru.yandex.market.wms.inventory.model.enums.LogisticPointType
import ru.yandex.market.wms.inventory.model.enums.TaskStatus
import ru.yandex.market.wms.inventory.model.enums.TaskType

@ContextConfiguration(
    classes = [
        LogisticPointDao::class,
        LocDao::class,
        TaskDao::class,
        LogDao::class,
        ItemDao::class,
        IdentityDao::class]
)
class IdentityDaoTest(
    @Autowired private val logisticPointDao: LogisticPointDao,
    @Autowired private val locDao: LocDao,
    @Autowired private val taskDao: TaskDao,
    @Autowired private val logDao: LogDao,
    @Autowired private val itemDao: ItemDao,
    @Autowired private val identityDao: IdentityDao,
) : AbstractJdbcTest() {

    private fun createLogisticPoint(): LogisticPointEntity {
        val logisticPoint = LogisticPointEntity(
            name = "RST",
            type = LogisticPointType.WMS,
        )
        logisticPointDao.insert(logisticPoint)
        return logisticPoint
    }

    private fun createLoc(logisticPointId: String): LocEntity {
        val loc =
            LocEntity(
                logisticPoint = logisticPointId,
                name = "A-A-41",
                type = LocType.CELL,
            )
        locDao.insert(loc)
        return loc
    }

    private fun createTask(logisticPoint: String, loc: String): TaskEntity {
        val task =
            TaskEntity(
                status = TaskStatus.NEW,
                type = TaskType.INITIAL,
                source = "katejud",
                loc = loc,
                logisticPoint = logisticPoint,
            )
        return taskDao.insert(task, USER)
    }

    private fun createLog(taskId: Long) {
        val log =
            LogEntity(
                logLine = LOG_LINE,
                taskId = taskId,
                sku = "ROV123",
                storer = "storer",
                lot = "lot",
                qtyExpected = 3,
                qtyInvented = 0,
                isInventedByQty = true,
                hasDiscrepancies = false,
                isMaster = true,
            )
        logDao.insert(listOf(log), taskId)
    }

    private fun createItem(taskId: Long): ItemEntity {
        val item =
            ItemEntity(
                taskId = taskId,
                logLine = LOG_LINE,
            )
        return itemDao.insert(item)
    }

    private fun createIdentity(itemId: Long) {
        val identity = IdentityEntity(
            itemId = itemId,
            value = IDENTITY_VALUE,
            type = IdentityType.EAN,
        )
        identityDao.insert(listOf(identity))
    }

    //Поиск item по identity
    @Test
    fun insertFullDataTest() {
        val logisticPoint = createLogisticPoint()
        val loc = createLoc(logisticPoint.name)
        val task = createTask(loc.logisticPoint, loc.name)
        task.id?.let { createLog(it) }
        val item = task.id?.let { createItem(it) }
        if (item != null) {
            item.id?.let { createIdentity(it) }
            val itemActualId = identityDao.findItemsByValue(IDENTITY_VALUE).get(0).itemId
            assertEquals(item.id, itemActualId)
        }
    }
}

private const val USER = "TEST"
private const val LOG_LINE = 1L
private const val IDENTITY_VALUE = "12345678"
