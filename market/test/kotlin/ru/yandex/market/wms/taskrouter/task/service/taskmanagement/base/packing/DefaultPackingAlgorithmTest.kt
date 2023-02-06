package ru.yandex.market.wms.taskrouter.task.service.taskmanagement.base.packing



import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.taskrouter.task.model.dto.PackingTaskDto
import ru.yandex.market.wms.taskrouter.task.model.dto.TaskDto
import ru.yandex.market.wms.taskrouter.task.service.TaskManager
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.BaseTaskManagementIntegrationTest
import ru.yandex.market.wms.taskrouter.task.service.taskmanagement.packing.DefaultPackingAlgorithm
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal class DefaultPackingAlgorithmTest : BaseTaskManagementIntegrationTest() {

    @Autowired
    var clock: Clock = Clock.fixed(Instant.parse("2022-06-01T12:00:00Z"), ZoneId.of("UTC"))

    @Autowired
    lateinit var packingAlgorithm: DefaultPackingAlgorithm

    @Autowired
    @MockBean
    lateinit var taskManager: TaskManager<PackingTaskDto>

    @Test
    fun `All users has non completable task`() {
        val users = setOf("someUserName")

        val allTask = listOf(
            PackingTaskDto(
                destination = "table_1", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            )
        )

        val result = listOf(
            PackingTaskDto(
                destination = "table_1", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            )
        )

        assignToUsers(users, allTask, result)
    }

    @Test
    fun `Any users has non completable task`() {

        val users = setOf("someUserName", "secondUserName")

        val allTask = listOf(
            PackingTaskDto(
                destination = "table_2", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 15, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_3", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_4", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 20, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_1", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            )
        )

        val result = listOf(
            PackingTaskDto(
                destination = "table_1", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_2", assignedUser = "secondUserName",
                sourceLocs = emptySet(), ticketCount = 15, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            )
        )

        assignToUsers(users, allTask, result)
    }

    @Test
    fun `With IDLE tasks`() {

        val users = setOf("someUserName", "secondUserName")

        val allTask = listOf(
            PackingTaskDto(
                destination = "table_2", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 15, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_3", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_4", assignedUser = null,
                sourceLocs = emptySet(), ticketCount = 20, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_1", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 0, cutoff = Instant.now(clock),
                ticketTypes = setOf(PackingTaskDto.Type.IDLE)
            )
        )

        val result = listOf(
            PackingTaskDto(
                destination = "table_2", assignedUser = "someUserName",
                sourceLocs = emptySet(), ticketCount = 15, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
            PackingTaskDto(
                destination = "table_3", assignedUser = "secondUserName",
                sourceLocs = emptySet(), ticketCount = 10, cutoff = Instant.now(clock).minus(1, ChronoUnit.HOURS),
                ticketTypes = setOf(PackingTaskDto.Type.SORTABLE)
            ),
        )

        assignToUsers(users, allTask, result)
    }

    @Test
    fun `no task`() {

        val users = setOf("someUserName", "secondUserName")

        val allTask = emptyList<PackingTaskDto>()

        val result = emptyList<PackingTaskDto>()

        assignToUsers(users, allTask, result)
    }

    private fun assignToUsers(
        userNames: Set<String>,
        allTask: List<PackingTaskDto>,
        result: List<TaskDto>
    ) {
        whenever(taskManager.getTasks()).thenReturn(allTask)
        val assignTask = packingAlgorithm.assignToUsers(userNames.toList())
        assertions.assertThat(assignTask).containsExactlyInAnyOrderElementsOf(result)
    }

}
