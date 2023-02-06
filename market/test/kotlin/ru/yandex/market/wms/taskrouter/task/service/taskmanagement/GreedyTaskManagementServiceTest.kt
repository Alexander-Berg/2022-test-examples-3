package ru.yandex.market.wms.taskrouter.task.service.taskmanagement

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DatabaseSetups
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean

class GreedyTaskManagementServiceTest : BaseTaskManagementIntegrationTest() {

    @Autowired
    lateinit var greedyTaskManagementService: GreedyTaskManagementService

    @SpyBean
    @Autowired
    lateinit var assignmentUpdaterService: AssignmentUpdaterService

    @BeforeEach
    fun resetMock() {
        Mockito.reset(assignmentUpdaterService)
    }

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
        greedyTaskManagementService.assignToUsers(listOf("User1"))
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun firstFloorPriorityOnStartTest() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        greedyTaskManagementService.assignToUsers(listOf("User1"))
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
        greedyTaskManagementService.assignToUsers(listOf("User1", "User2"))
        assertUserActivityByAssignmentWithExpectedUser("123123128", "User1" ,1 )
        assertUserActivityByAssignmentWithExpectedUser("123123127", "User2", 1)
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun whenUserComeFromOtherProcess() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123127", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123137", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123125", "MEZONIN_3")
        createTaskDetailAndUserActivity("123123128", "MEZONIN_3")
        createTaskDetailAndUserActivity("123123126", "MEZONIN_4")
        createTaskDetailAndUserActivity("123123129", "MEZONIN_5")
        greedyTaskManagementService.assignToUsers(listOf("User1"))
        assertUserActivityByAssignmentWithExpectedUser("123123124", "User1", 1)
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
        greedyTaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("1002", "User4")
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
        greedyTaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("5003", "User4", 1)
    }

    @Test
    @DatabaseSetup("/taskmanagement/db/common.xml")
    fun multipleAddUserToWaitersQueue() {
        createTaskDetailAndUserActivity("123123123", "MEZONIN_1")
        createTaskDetailAndUserActivity("123123124", "MEZONIN_2")
        createTaskDetailAndUserActivity("123123125", "MEZONIN_1")
        greedyTaskManagementService.assignToUsers(listOf("User1"))
        greedyTaskManagementService.assignToUsers(listOf("User1"))
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
        greedyTaskManagementService.assignToUsers(listOf("User4"))
        assertUserActivityByAssignmentWithExpectedUser("3002", "User4")

        Mockito.verify(assignmentUpdaterService, times(1))
            .updateTasks(eq("User4"), anyList(), eq("MEZONIN_3"), eq(GreedyTaskManagementService.ALGORITHM_VERSION))
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/assign_zone.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/user_skill.xml")
    )
    fun assignUseSkillWhenTaskTagNotContainSkill() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivityWithSkill("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "5001", 5)
        val countTaskWithoutUserBefore = countTaskWithoutUser()
        greedyTaskManagementService.assignToUsers(listOf("User4"))
        val countTaskWithoutUserAfter = countTaskWithoutUser()
        Assertions.assertEquals(countTaskWithoutUserBefore, countTaskWithoutUserAfter)
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/assign_zone.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/user_skill.xml")
    )
    fun assignUseSkillWhenTaskTagContainAllSkill() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivityWithSkill("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "5002", 5)
        greedyTaskManagementService.assignToUsers(listOf("User5"))
        assertUserActivityByAssignmentWithExpectedUser("5003", "User5")
    }

    @Test
    @DatabaseSetups(
        DatabaseSetup("/taskmanagement/db/common.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/assign_zone.xml"),
        DatabaseSetup("/taskmanagement/db/greedy_v1/user_skill.xml")
    )
    fun assignUseSkillWhenTaskTagNoContainAllSkill() {
        for (mez in 1..5) {
            for (task in 1..3) {
                createTaskDetailAndUserActivityWithSkill("${mez}00${task}", "MEZONIN_$mez", 5)
            }
        }

        addTagToTask(IntRange(1, 30).map { "500${it}" }, "POWER_LIFTER")
        assignAndFinishTaskWithUser("User1", "5001", 5, -20, -10)
        assignAndFinishTaskWithUser("User2", "4001", 5, -20, -10)
        assignAndFinishTaskWithUser("User3", "3001", 5, -20, 0)
        assignAndFinishTaskWithUser("User4", "5001", 5)
        val countTaskWithoutUserBefore = countTaskWithoutUser()
        greedyTaskManagementService.assignToUsers(listOf("User5"))
        val countTaskWithoutUserAfter = countTaskWithoutUser()
        Assertions.assertEquals(countTaskWithoutUserBefore, countTaskWithoutUserAfter)
    }
}
