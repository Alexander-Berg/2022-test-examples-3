package ru.yandex.market.abo.logbroker.checkouter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.listener.scheduled.ScheduledCpaEventListenerTask
import ru.yandex.market.abo.cpa.order.listener.scheduled.ScheduledCpaEventListenerType
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

/**
 * @author komarovns
 */
class ScheduledCpaEventListenersServiceTest @Autowired constructor(
    private val scheduledCpaEventListenersService: ScheduledCpaEventListenersService,
    private val scheduledCpaEventListenerTaskUpdater: PgBatchUpdater<ScheduledCpaEventListenerTask>,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @Test
    fun `batch updater`() {
        val initTask = ScheduledCpaEventListenerTask(null, ScheduledCpaEventListenerType.CROSSDOCK_MISSING_ITEM)
        scheduledCpaEventListenerTaskUpdater.insertOrUpdate(listOf(initTask))
        val tasks = scheduledCpaEventListenersService.loadTasks(2)
        assertEquals(1, tasks.size)

        val task = tasks[0]
        assertTrue(task.id ?: 0 > 0)
        assertNull(task.body)
        assertEquals(initTask.type, task.type)
        assertEquals(initTask.creationTime, task.creationTime)

        scheduledCpaEventListenerTaskUpdater.insertOrUpdate(listOf(task))
        flushAndClear()
        assertEquals(1, scheduledCpaEventListenersService.loadTasks(2).size)
    }

    @Test
    fun `batch updater json`() {
        scheduledCpaEventListenerTaskUpdater.insertOrUpdate(listOf(ScheduledCpaEventListenerTask(
            """{"key" : "value"}""", ScheduledCpaEventListenerType.ORDER_RETURN
        )))
        assertEquals("value", jdbcTemplate.queryForObject("""
            SELECT body ->> 'key'
            FROM cpa_event_listener_task
        """.trimIndent(), String::class.java))
    }
}
