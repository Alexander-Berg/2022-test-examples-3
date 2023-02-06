package ru.yandex.market.wms.taskrouter.task.service.taskmanagement.consolidation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.taskrouter.task.model.dto.ConsolidationTaskDto
import ru.yandex.market.wms.taskrouter.task.model.dto.ConsolidationWaveDto
import ru.yandex.market.wms.taskrouter.task.service.TaskManager
import java.math.BigDecimal
import java.time.Instant

internal class DefaultConsolidationAlgorithmTest {
    private val taskManager: TaskManager<ConsolidationTaskDto> = mock()

    private val algorithm = DefaultConsolidationAlgorithm(taskManager)

    private val user1 = "test_user_1"
    private val user2 = "test_user_2"
    private val user3 = "test_user_3"

    private val task1 = ConsolidationTaskDto(
        destination = "S01",
        assignedUser = user1,
        waves = listOf(
            ConsolidationWaveDto(
                percentageOfReadiness = 50,
                line = "S01_1",
                isSorting = false,
                qty = BigDecimal(10),
                cutoff = Instant.parse("2022-07-09T10:00:00.00Z")
            )
        ),
        availableCells = 10
    )
    private val task2 = ConsolidationTaskDto(
        destination = "S02",
        assignedUser = null,
        waves = listOf(
            ConsolidationWaveDto(
                percentageOfReadiness = 50,
                line = "S02_1",
                isSorting = false,
                qty = BigDecimal(10),
                cutoff = Instant.parse("2022-07-07T10:00:00.00Z")
            ),
            ConsolidationWaveDto(
                percentageOfReadiness = 100,
                line = "S02_2",
                isSorting = false,
                qty = BigDecimal(100),
                cutoff = Instant.parse("2022-07-05T10:00:00.00Z")
            )
        ),
        availableCells = 10
    )
    private val task3 = ConsolidationTaskDto(
        destination = "S03",
        assignedUser = null,
        waves = listOf(
            ConsolidationWaveDto(
                percentageOfReadiness = 50,
                line = "S03_1",
                isSorting = true,
                qty = BigDecimal(40),
                cutoff = Instant.parse("2022-07-05T10:00:00.00Z")
            ),
            ConsolidationWaveDto(
                percentageOfReadiness = 50,
                line = "S03_2",
                isSorting = false,
                qty = BigDecimal(40),
                cutoff = Instant.parse("2022-07-06T10:00:00.00Z")
            )
        ),
        availableCells = 10
    )

    @Test
    fun assignToTwoUsers() {
        //given
        whenever(taskManager.getTasks()).thenReturn(listOf(task1, task2, task3))
        //when
        val assign = algorithm.assignToUsers(listOf(user1, user2))
        //then
        assertTrue(assign.isNotEmpty())
        assertEquals(assign.size, 2)
        assertEquals(task1, assign.firstOrNull { it.assignedUser == user1 })
        assertEquals(task3.copy(assignedUser = user2), assign.firstOrNull { it.assignedUser == user2 })
    }

    @Test
    fun assignToAllUsersWithOneTask() {
        //given
        whenever(taskManager.getTasks()).thenReturn(listOf(task2))
        //when
        val assign = algorithm.assignToUsers(listOf(user1, user2, user3))
        //then
        assertTrue(assign.isNotEmpty())
        assertEquals(assign.size, 1)
        assertEquals(task2.copy(assignedUser = user1), assign.firstOrNull { it.assignedUser == user1 })
    }

    @Test
    fun assignToOneUser() {
        //given
        whenever(taskManager.getTasks()).thenReturn(listOf(task1, task2, task3))
        //when
        val assign = algorithm.assignToUsers(listOf(user2))
        //then
        assertTrue(assign.isNotEmpty())
        assertEquals(assign.size, 1)
        assertEquals(task3.copy(assignedUser = user2), assign.firstOrNull { it.assignedUser == user2 })
    }

    @Test
    fun assignToUserWithEmptyTasks() {
        //given
        whenever(taskManager.getTasks()).thenReturn(listOf())
        //when
        val assign = algorithm.assignToUsers(listOf(user1, user2, user3))
        //then
        assertTrue(assign.isEmpty())
    }

}
