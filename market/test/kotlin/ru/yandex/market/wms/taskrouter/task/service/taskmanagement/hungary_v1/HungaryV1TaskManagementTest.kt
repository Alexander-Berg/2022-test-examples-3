package ru.yandex.market.wms.taskrouter.task.service.taskmanagement.hungary_v1

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.BaseTaskManagementIntegrationTest

class HungaryV1TaskManagementTest : BaseTaskManagementIntegrationTest() {

    @Autowired
    lateinit var hungaryV1TaskManagementService: HungaryV1TaskManagementService

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/hungary_v1/1/before.xml")
    )
    @ExpectedDatabase(
        value = "/taskmanagement/db/hungary_v1/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun simpleTest() {
        hungaryV1TaskManagementService.assignToUsers(listOf("User1"))
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun firstFloorPriorityOnStartTest() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        hungaryV1TaskManagementService.assignToUsers(listOf("User1"))
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
        createTaskDetailAndUserActivity("123123129", "MEZONIN_5")
        assignAndFinishTaskWithUser("User1", "123123125")
        assignAndFinishTaskWithUser("User2", "123123124")
        hungaryV1TaskManagementService.assignToUsers(listOf("User1", "User2"))
        assertUserActivityByAssignmentWithExpectedUser("123123128", "User1" , 1)
        assertUserActivityByAssignmentWithExpectedUser("123123127", "User2", 1)
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun currentFloorPriorityWithWaitersTest() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivity("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "2001", 5)
        assignAndFinishTaskWithUser("User4", "2002", 5)
        assignAndFinishTaskWithUser("User4", "2003", 5)
        assignAndFinishTaskWithUser("User5", "1001", 5, -20, -10)
        hungaryV1TaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("3002", "User4")
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun currentFloorPriorityWithWaitersAndPriorityTaskTest() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivity("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "2001", 5)
        assignAndFinishTaskWithUser("User4", "2002", 5)
        assignAndFinishTaskWithUser("User4", "2003", 5)
        assignAndFinishTaskWithUser("User5", "1001", 5, -20, -10)
        setPriorityForTask("5003", 0)
        hungaryV1TaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("5003", "User4", 1)
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun multipleAddUserToWaitersQueue() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123125", "MEZONIN_1")
        hungaryV1TaskManagementService.assignToUsers(listOf("User1"))
        hungaryV1TaskManagementService.assignToUsers(listOf("User1"))
        assertUserActivityByAssignmentWithExpectedUser("123123123", "User1", 1)
        assertUserActivityByAssignmentWithExpectedUser("123123124", "", 1)
        assertUserActivityByAssignmentWithExpectedUser("123123125", "User1", 1)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/assign_zone.xml")
    )
    fun assignWhenExistAssignZone() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivity("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "5001", 5)
        hungaryV1TaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("3002", "User4")
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/assign_zone_multi.xml")
    )
    fun assignWhenExistManyAssignZone() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivity("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "5001", 5)
        hungaryV1TaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("3002", "User4")
    }
}
