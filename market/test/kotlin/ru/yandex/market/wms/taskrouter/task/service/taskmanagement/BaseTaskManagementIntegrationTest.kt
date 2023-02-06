package ru.yandex.market.wms.taskrouter.task.service.taskmanagement

import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.wms.common.spring.dao.entity.AssignmentTag
import ru.yandex.market.wms.common.spring.dao.implementation.AssignmentTagsDao
import ru.yandex.market.wms.common.spring.dao.implementation.TaskDetailDao
import ru.yandex.market.wms.common.spring.dao.implementation.UserActivityDao
import ru.yandex.market.wms.taskrouter.config.BaseTest

open class BaseTaskManagementIntegrationTest : BaseTest() {

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var userActivityDao: UserActivityDao

    @Autowired
    lateinit var taskDetailDao: TaskDetailDao

    @Autowired
    lateinit var assignmentTagsDao: AssignmentTagsDao

    private var taskDetailKeyCounter: Int = 1

    fun createTaskDetailAndUserActivity(assignmentNumber: String, zone: String, count: Int = 1, taskPriority: Int = 2) {
        for (priority in 1..count) {
            val lenToPad = 10 - assignmentNumber.length;
            val assignmentNumberPad = assignmentNumber + priority.toString().padStart(lenToPad, '0')
            val taskDetailKey = String.format("TDK%07d", taskDetailKeyCounter)
            val taskDetailInsertQuery = """
                INSERT INTO wmwhse1.TASKDETAIL (ASSIGNMENTNUMBER, TASKTYPE, STATUS, QTY, PUTAWAYZONE,
                GROSSWGT, TASKDETAILKEY, ORDERKEY, WAVEKEY, PICKDETAILKEY, LOT, STORERKEY, SKU, USERKEY)
                VALUES (:assignmentNumber, 'PK', 0, 1, :zone, 1, :taskDetailKey, 'orderKey', 'W5',
                'PDK1', 'L5', '100', 'ROV0000000000000000005', '')
            """
            jdbcTemplate.update(
                taskDetailInsertQuery,
                mapOf(
                    "assignmentNumber" to assignmentNumberPad,
                    "zone" to zone,
                    "taskDetailKey" to taskDetailKey
                )
            )
            val userActivityInsertQuery = """
                INSERT INTO wmwhse1.USERACTIVITY (USERACTIVITYKEY, ASSIGNMENTNUMBER, PRIORITY, USERID,
                    TASKDETAILKEY, STATUS, TYPE)
                VALUES (:userActivityKey, :assignmentNumber, :priority, '', :taskDetailKey, 0, 'PK')
            """
            jdbcTemplate.update(
                userActivityInsertQuery,
                mapOf(
                    "assignmentNumber" to assignmentNumberPad,
                    "taskDetailKey" to taskDetailKey,
                    "priority" to taskPriority,
                    "userActivityKey" to "UA%07d".format(taskDetailKeyCounter)
                )
            )
            taskDetailKeyCounter++
        }
    }

    fun createTaskTag(assignmentNumber: String, count: Int, tags: Collection<String>) =
        tags.flatMap { tag ->
            (1..count).map {
                AssignmentTag.builder()
                    .assignmentNumber(assignmentNumber + it.toString().padStart((10 - assignmentNumber.length), '0'))
                    .tag(tag).build()
            }
        }.also { assignmentTagsDao.insert(it) }

    fun createTaskDetailAndUserActivityWithSkill(
        assignmentNumber: String,
        zone: String,
        count: Int = 1
    ) {
        val tags = listOf(zone)
        createTaskDetailAndUserActivity(assignmentNumber, zone, count)
        createTaskTag(assignmentNumber, count, tags)
    }

    fun assertUserActivityByAssignmentWithExpectedUser(assignmentNumber: String, userId: String, countTask: Long = 5) {
        val queryWithUser = """
            SELECT COUNT(*) FROM wmwhse1.USERACTIVITY WHERE ASSIGNMENTNUMBER like (:assignmentNumber) AND USERID = :userId
        """
        val countWithUser = jdbcTemplate.queryForObject(
            queryWithUser,
            mapOf("assignmentNumber" to "${assignmentNumber}%", "userId" to userId),
            Long::class.javaObjectType
        ) ?: 0

        assertEquals(countTask, countWithUser)
    }

    fun countTaskWithoutUser(): Long =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*) FROM wmwhse1.TASKDETAIL WHERE USERKEY = ''
                """,
            emptyMap<String, Any>(),
            Long::class.javaObjectType
        ) ?: 0

    fun addTagToTask(assignmentNumbers: Collection<String>, tag: String) {
        jdbcTemplate.batchUpdate(
            """
                INSERT INTO wmwhse1.ASSIGNMENT_TAGS(ASSIGNMENTNUMBER, TAG)
                VALUES (:assignmentNumber, :tag);
                """,
            assignmentNumbers.map { mapOf<String, Any?>("assignmentNumber" to it, "tag" to tag) }.toTypedArray()
        )
    }

    fun assignAndFinishTaskWithUser(
        user: String,
        assignmentNumber: String,
        count: Int = 1,
        firstFinishMinutes: Int = -10,
        lastFinishMinutes: Int = 0
    ) {
        userActivityDao.updateUserActivityByAssignmentNumber(user, listOf(assignmentNumber))
        taskDetailDao.setGroupId(listOf(assignmentNumber), user, "test")

        val assignmentNumbers = (1..count).map {
            assignmentNumber + it.toString().padStart((10 - assignmentNumber.length), '0')
        }

        val userActivitySetInProcessQuery = """
                UPDATE wmwhse1.USERACTIVITY SET STATUS = 1, EDITDATE = GETDATE() WHERE ASSIGNMENTNUMBER in (:assignmentNumbers)
            """
        jdbcTemplate.update(userActivitySetInProcessQuery, mapOf("assignmentNumbers" to assignmentNumbers))

        val taskDetailKeysQuery = """
            SELECT TOP :count TASKDETAILKEY FROM wmwhse1.TASKDETAIL
            WHERE ASSIGNMENTNUMBER IN (:assignmentNumbers)
            ORDER BY TASKDETAILKEY ASC
        """
        var counter = 1
        jdbcTemplate.query(
            taskDetailKeysQuery,
            mapOf(
                "assignmentNumbers" to assignmentNumbers,
                "count" to count
            )
        ) {
            val taskDetailKey = it.getString("TASKDETAILKEY")
            val endtime = (counter++) * (lastFinishMinutes - firstFinishMinutes) / count + firstFinishMinutes
            val userActivityUpdateQuery = """
                UPDATE wmwhse1.USERACTIVITY SET STATUS = 9, EDITDATE = dateadd(MINUTE, :endtime, GETDATE())
                WHERE ASSIGNMENTNUMBER In (:assignmentNumbers) AND TASKDETAILKEY = :taskDetailKey
            """
            jdbcTemplate.update(
                userActivityUpdateQuery,
                mapOf(
                    "assignmentNumbers" to assignmentNumbers,
                    "taskDetailKey" to taskDetailKey,
                    "endtime" to endtime
                )
            )
            val taskDetailUpdateQuery = """
                UPDATE wmwhse1.TASKDETAIL SET STATUS = 9, USERKEY = :userId,
                    ENDTIME = dateadd(MINUTE, :endtime, GETDATE()), EDITDATE = dateadd(MINUTE, :endtime, GETDATE())
                WHERE ASSIGNMENTNUMBER IN (:assignmentNumbers) AND TASKDETAILKEY = :taskDetailKey
            """
            jdbcTemplate.update(
                taskDetailUpdateQuery,
                mapOf(
                    "assignmentNumbers" to assignmentNumbers,
                    "userId" to user,
                    "taskDetailKey" to taskDetailKey,
                    "endtime" to endtime
                )
            )
        }
    }

    fun setPriorityForTask(assignmentNumber: String, priority: Int) {
        val userActivityUpdateQuery = """
                UPDATE wmwhse1.USERACTIVITY SET PRIORITY = :priority
                WHERE ASSIGNMENTNUMBER like :assignmentNumber
            """
        jdbcTemplate.update(
            userActivityUpdateQuery,
            mapOf(
                "assignmentNumber" to "${assignmentNumber}%",
                "priority" to priority
            )
        )
    }
}
