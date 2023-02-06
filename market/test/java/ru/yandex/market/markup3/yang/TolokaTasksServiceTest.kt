package ru.yandex.market.markup3.yang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.core.TolokaSource
import ru.yandex.market.markup3.core.dto.TaskRow
import ru.yandex.market.markup3.core.exceptions.NeedToRecoverException
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.utils.CommonObjectMapper
import ru.yandex.market.markup3.yang.repositories.TolokaRecoverQueueRepository
import ru.yandex.market.markup3.yang.services.CreateTaskSuiteRequest
import ru.yandex.market.markup3.yang.services.GetOrCreatePoolRequest
import ru.yandex.market.markup3.yang.services.TaskInTaskSuite
import ru.yandex.market.markup3.yang.services.TolokaTasksService
import ru.yandex.toloka.client.v1.pool.Pool
import java.math.BigDecimal
import java.util.Date

class TolokaTasksServiceTest : CommonTaskTest() {
    @Autowired
    lateinit var tolokaTasksService: TolokaTasksService
    @Autowired
    lateinit var tolokaClientMock: TolokaClientMock
    @Autowired
    lateinit var tolokaRecoverQueueRepository: TolokaRecoverQueueRepository

    @Test
    fun `get or create pool works`() {
        val pool = createBasePool()

        val req = GetOrCreatePoolRequest(
            poolGroupId = "PG",
            basePoolId = pool.id,
            poolCustomizer = null,
            source = TolokaSource.YANG,
            forbidDownload = false,
        )

        val poolInfo = tolokaTasksService.getOrCreatePool(req)
        poolInfo.active shouldBe true
        poolInfo.poolGroupId shouldBe "PG"
        poolInfo.externalPoolId shouldNotBe null
        poolInfo.id shouldNotBe null
        tolokaClientMock.getPool(poolInfo.externalPoolId) shouldNotBe null

        //same request - same pool
        val poolInfo2 = tolokaTasksService.getOrCreatePool(req)
        poolInfo2.id shouldBe poolInfo.id

        tolokaPoolInfoRepository.findAll() shouldHaveSize 1
    }

    @Test
    fun `create task suite`() {
        val pool = createBasePool()

        val req = GetOrCreatePoolRequest(
            poolGroupId = "PG",
            basePoolId = pool.id,
            poolCustomizer = null,
            source = TolokaSource.YANG,
            forbidDownload = false,
        )

        val poolInfo = tolokaTasksService.getOrCreatePool(req)

        val task = createFakeMarkupTask()

        val createTasksReq = CreateTaskSuiteRequest(
            tolokaPoolInfo = poolInfo,
            taskGroup = createTestTaskGroup(),
            taskId = task.id,
            tasks = listOf(TaskInTaskSuite("key", emptyMap<String, Any>())),
            overlap = 1,
        )

        val result = tolokaTasksService.createTaskSuite(createTasksReq)

        result shouldHaveSize 1
    }

    @Test
    fun `should add to recover queue`() {
        tolokaClientMock.setShouldThrowOnce(true)
        val pool = createBasePool()

        val req = GetOrCreatePoolRequest(
            poolGroupId = "PG",
            basePoolId = pool.id,
            poolCustomizer = null,
            source = TolokaSource.YANG,
            forbidDownload = false,
        )

        val poolInfo = tolokaTasksService.getOrCreatePool(req)

        val task = createFakeMarkupTask()

        val createTasksReq = CreateTaskSuiteRequest(
            tolokaPoolInfo = poolInfo,
            taskGroup = createTestTaskGroup(),
            taskId = task.id,
            tasks = listOf(TaskInTaskSuite("key", emptyMap<String, Any?>())),
            overlap = 1,
        )

        tolokaRecoverQueueRepository.findAll() shouldHaveSize 0

        // toloka handler throw
        shouldThrow<RuntimeException> {
            tolokaTasksService.createTaskSuite(createTasksReq)
        }

        // waiting for recover
        shouldThrow<NeedToRecoverException> {
            tolokaTasksService.createTaskSuite(createTasksReq)
        }

        val recoverQueue = tolokaRecoverQueueRepository.findAll()
        recoverQueue shouldHaveSize 1
        val recoverRow = recoverQueue[0]
        recoverRow.poolId shouldBe poolInfo.id
        recoverRow.taskId shouldBe task.id
        recoverRow.taskKey shouldBe "key"

        val taskInfo = tolokaTaskInfoRepository.findByTaskId(task.id)[0]
        taskInfo.recovered = true
        tolokaTaskInfoRepository.update(taskInfo)

        val result = tolokaTasksService.createTaskSuite(createTasksReq)
        result shouldHaveSize 1
    }

    private fun createFakeMarkupTask(): TaskRow {
        return taskDbService.insertTasks(
            listOf(
                TaskRow(
                    taskGroupId = 1,
                    externalKey = "createTask.externalKey",
                    stage = "init",
                    state = CommonObjectMapper.valueToTree("q"),
                )
            )
        )[0]
    }

    private fun createBasePool(): Pool = tolokaClientMock.createPool(
        Pool(
            "prj",
            "pr_name",
            true,
            Date(),
            BigDecimal.ONE,
            1,
            true,
            null
        )
    ).result
}
