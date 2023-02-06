package ru.yandex.market.wms.taskrouter.task.service.taskmanagement.base

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.BaseTaskManagementIntegrationTest

class DefaultTaskManagementTest : BaseTaskManagementIntegrationTest() {

    @Autowired
    lateinit var defaultTaskManagementService: DefaultTaskManagementService

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/base/1/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/base/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun oneUserAndOneTaskTest() {
        defaultTaskManagementService.assignToUsers(listOf("User1"))
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun firstFloorPriorityOnStartTest() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        defaultTaskManagementService.assignToUsers(listOf("User1"))
        assertUserActivityByAssignmentWithExpectedUser("123123123", "User1", 1)
        assertUserActivityByAssignmentWithExpectedUser("123123124", "", 1)
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun currentFloorPriorityTest() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123125", "MEZONIN_3")
        createTaskDetailAndUserActivity("123123126", "MEZONIN_4")
        createTaskDetailAndUserActivity("123123127", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123128", "MEZONIN_3")
        assignAndFinishTaskWithUser("User1", "123123125")
        assignAndFinishTaskWithUser("User2", "123123124")
        defaultTaskManagementService.assignToUsers(listOf("User1", "User2"))
        assertUserActivityByAssignmentWithExpectedUser("123123128", "User1", 1)
        assertUserActivityByAssignmentWithExpectedUser("123123127", "User2", 1)
    }
}
