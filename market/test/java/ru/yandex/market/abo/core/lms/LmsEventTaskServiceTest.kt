package ru.yandex.market.abo.core.lms

import java.time.LocalDateTime
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.express.moderation.ExpressModerationInfo
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.PHONE
import ru.yandex.market.abo.core.express.moderation.warehouse.WarehouseChangeValue.SCHEDULE
import ru.yandex.market.abo.core.lms.LmsEventTaskService.Companion.TASK_EXPIRATION_PERIOD_HOURS
import ru.yandex.market.abo.core.lms.LmsEventTaskType.EXPRESS_WAREHOUSE_CHANGED
import ru.yandex.market.abo.core.lms.LmsEventTaskType.WAREHOUSE_CREATED
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB
import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto

/**
 * @author zilzilok
 */
class LmsEventTaskServiceTest @Autowired constructor(
    private val lmsEventTaskService: LmsEventTaskService
) : EmptyTest() {

    @Test
    fun `load unprocessed tasks`() {
        val task = getDefaultTask()
        lmsEventTaskService.saveTasks(listOf(task, task))
        flushAndClear()

        val tasksByPartner = lmsEventTaskService.loadUnprocessedTasks()
        assertEquals(1, tasksByPartner.size)

        val tasksFromDb = tasksByPartner[PARTNER_ID]!!
        assertEquals(2, tasksFromDb.size)

        val taskFromDb = tasksFromDb[0]
        assertEquals(task.partnerId, taskFromDb.partnerId)
        assertEquals(task.type, taskFromDb.type)
        assertNull(taskFromDb.processedTime)

        lmsEventTaskService.markTaskProcessed(taskFromDb)

        assertEquals(1, lmsEventTaskService.loadUnprocessedTasks()[PARTNER_ID]!!.size)
    }

    @Test
    fun `create and extract body`() {
        val snapshot = getSnapshot()
        val changes = listOf(PHONE, SCHEDULE)

        val body = lmsEventTaskService.createTaskBody(changes, snapshot)
        val task = LmsEventTask(1L, 2L, PARTNER_ID, DSBB, EXPRESS_WAREHOUSE_CHANGED, body)
        lmsEventTaskService.saveTasks(listOf(task))

        val taskFromDb = lmsEventTaskService.loadUnprocessedTasks()[PARTNER_ID]?.get(0)!!
        val bodyFromDb = lmsEventTaskService.extractTaskBody(taskFromDb.body)
        assertEquals(changes, bodyFromDb.changes)
        assertEquals(ExpressModerationInfo(snapshot), bodyFromDb.warehouseInfo)
    }

    @Test
    fun `mark old tasks expired test`() {
        val oldTask = LmsEventTask(
            eventId = 1,
            partnerId = PARTNER_ID,
            partnerModel = DSBB,
            creationTime = LocalDateTime.now().minusHours(TASK_EXPIRATION_PERIOD_HOURS + 1),
            type = WAREHOUSE_CREATED
        )
        val newTask = getDefaultTask()
        val tasks = listOf(oldTask, newTask)
        lmsEventTaskService.saveTasks(tasks)
        flushAndClear()
        lmsEventTaskService.updateExpiredTasks()
        flushAndClear()

        val taskAfterProcess = lmsEventTaskService.loadUnprocessedTasks()
        assertEquals(1, taskAfterProcess.size)
        assertThat(taskAfterProcess[PARTNER_ID]!![0])
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(newTask)
    }

    private fun getDefaultTask() = LmsEventTask(
        eventId = 2L,
        partnerId = PARTNER_ID,
        partnerModel = DSBB,
        creationTime = LocalDateTime.now(),
        type = WAREHOUSE_CREATED
    )

    private fun getSnapshot() =
        MAPPER.readValue(
            """
            {
              "phones": [
                {
                  "number": "+788888888",
                  "internalNumber": "1333",
                  "comment": "number-2",
                  "type": 0
                }
              ],
              "schedule": [
                {
                  "id": null,
                  "day": 2,
                  "timeFrom": "13:00:00",
                  "timeTo": "15:00:00",
                  "isMain": true
                }
              ]
            }
            """, BusinessWarehouseSnapshotDto::class.java
        )

    companion object {
        private const val PARTNER_ID = 123L
        private val MAPPER = ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModules(ParameterNamesModule(), JavaTimeModule())
    }
}
