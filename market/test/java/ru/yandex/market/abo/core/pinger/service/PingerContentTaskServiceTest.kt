package ru.yandex.market.abo.core.pinger.service

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pinger.model.PingerContentTask

/**
 * @author komarovns
 * @date 20.01.2021
 */
class PingerContentTaskServiceTest @Autowired constructor(
    private val pingerContentTaskService: PingerContentTaskService
) : EmptyTest() {

    @Test
    fun deleteAllByConsumedTimeBeforeTest() {
        val now = LocalDateTime.now()
        val tasks = listOf(
            task(now),
            task(now.minusDays(2)).apply {
                consumedTime = now.minusDays(2)
            }
        )
        pingerContentTaskService.saveAll(tasks)
        flushAndClear()
        pingerContentTaskService.deleteTasksBefore(now.minusDays(1))
        flushAndClear()
        val dbTasks = pingerContentTaskService.loadAll()
        assertEquals(1, dbTasks.size)
        assertEquals(tasks[0].id, dbTasks[0].id)
    }

    @Test
    fun loadNewProcessedTasksByGeneratorTypeTest() {
        val oldTask = task(
            time = LocalDateTime.now().minusHours(2),
        ).apply {
            result = true
            consumedTime = LocalDateTime.now().minusHours(2)
        }
        val newTask = task(
            time = LocalDateTime.now(),
        ).apply {
            result = true
            consumedTime = LocalDateTime.now()
        }

        val tasks = listOf(oldTask, newTask)
        pingerContentTaskService.saveAll(tasks)
        flushAndClear()

        val dbTasks = pingerContentTaskService.loadNewProcessedTasks(
            LocalDateTime.now().minusHours(1), 10
        ).toList()
        assertEquals(1, dbTasks.size)
        assertEquals(newTask.id, dbTasks[0].id)
    }

    private fun task(time: LocalDateTime) =
        PingerContentTask.builder()
            .genId(1)
            .creationTime(time)
            .build()
}
