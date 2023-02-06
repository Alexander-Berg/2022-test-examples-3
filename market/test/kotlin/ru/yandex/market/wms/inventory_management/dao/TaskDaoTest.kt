package ru.yandex.market.wms.inventory_management.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.inventory.model.enums.TaskStatus
import ru.yandex.market.wms.inventory.model.enums.TaskType
import ru.yandex.market.wms.inventory_management.config.InventoryManagementIntegrationTest
import ru.yandex.market.wms.inventory_management.model.dto.TaskDto

class TaskDaoTest(
    @Autowired private val taskDao: TaskDao): InventoryManagementIntegrationTest() {

    companion object {
        private const val USER = "testUser"
    }

    @Test
    @DatabaseSetup("/dao/after-insert.xml")
    fun findTaskTest() {
        val expected = getTask()
        val tasks = taskDao.findTasksByGroupId(1).toList()
        Assertions.assertEquals(1, tasks.size)
        assertThat(tasks)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "editDate")
            .contains(expected)
    }

    @Test
    @DatabaseSetup("/dao/empty.xml")
    @ExpectedDatabase(value = "/dao/after-insert.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun insertTask() {
        taskDao.insertTasks(listOf(getTask()), USER)
    }

    @Test
    @DatabaseSetup("/dao/before-update.xml")
    @ExpectedDatabase(value = "/dao/after-update-external-task-id.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun updateTasksWithExternalTaskId() {
        taskDao.updateTasksWithExternalTaskId(mapOf(1L to 10L), USER)
    }

    @Test
    @DatabaseSetup("/dao/before-update.xml")
    @ExpectedDatabase(value = "/dao/after-update-task-status.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun updateTaskStatus() {
        val task = getTask(
            taskId = 1,
            executeWho = "inventorer",
            hasDiscrepancies = true,
            status = TaskStatus.FINISHED)
        taskDao.updateTasksWithExternalTaskDetails(listOf(task), USER)
    }

    private fun getTask(taskId: Long? = null,
                        externalTaskId: Long? = null,
                        executeWho: String? = null,
                        hasDiscrepancies: Boolean = false,
                        status: TaskStatus = TaskStatus.NEW,
                        user: String = USER,
    ): TaskDto
    {
        return TaskDto(
            id = taskId,
            status = status,
            type = TaskType.INITIAL,
            source = "TEST",
            putawayZone = "ZONE",
            loc = "LOC",
            locationType = "PICK",
            groupId = 1,
            externalTaskId = externalTaskId,
            executeWho = executeWho,
            hasDiscrepancies = hasDiscrepancies,
            addWho = user,
            editWho = user,
        )
    }
}


