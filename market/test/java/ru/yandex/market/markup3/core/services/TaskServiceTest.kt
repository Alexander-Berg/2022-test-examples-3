package ru.yandex.market.markup3.core.services

import io.kotest.assertions.asClue
import io.kotest.assertions.forEachAsClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.testutils.handlers.TestInput
import ru.yandex.market.markup3.utils.CommonObjectMapper

class TaskServiceTest : CommonTaskTest() {
    @Test
    fun `it should create something`() {
        val response = taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(CreateTask(TestInput()))
            )
        )

        response.results.forEachAsClue { it.result shouldBe CreateTaskResult.OK }

        taskDbService.findAll() shouldHaveSize 1
    }

    @Test
    fun `it should create with unique keys`() {
        val response = taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(CreateTask(TestInput(), uniqueKeys = listOf("Hey", "you!"), externalKey = "Yay!")),
            )
        )

        response.results.forEachAsClue { it.result shouldBe CreateTaskResult.OK }

        val tasks = taskDbService.findAll()
        tasks shouldHaveSize 1
        tasks[0].asClue {
            it.externalKey shouldBe "Yay!"
            it.stage shouldBe "init"
            CommonObjectMapper.writeValueAsString(it.state) shouldBe """{"data":"Hello"}"""
        }
    }

    @Test
    fun `test multiple tasks and unique keys error`() {
        taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(
                    CreateTask(TestInput(), uniqueKeys = listOf("Hey", "you!"), externalKey = "Yay!")
                ),
            )
        )

        var tasks = taskDbService.findAll()
        tasks shouldHaveSize 1

        val response = taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(
                    CreateTask(TestInput(), uniqueKeys = listOf("2.1", "2.2"), externalKey = "2"),
                    CreateTask(TestInput(), uniqueKeys = listOf("Hey", "you2!"), externalKey = "Yay2!"),
                    CreateTask(TestInput(), uniqueKeys = listOf(), externalKey = "3"),
                    CreateTask(TestInput(), uniqueKeys = listOf(), externalKey = "4")
                ),
            )
        )

        response.results[0].result shouldBe CreateTaskResult.OK
        response.results[1].result shouldBe CreateTaskResult.FAIL_UNIQUE_KEY
        response.results[1].failedUniqueKeys shouldBe mapOf("Hey" to tasks[0].id)
        response.results[2].result shouldBe CreateTaskResult.OK
        response.results[3].result shouldBe CreateTaskResult.OK

        tasks = taskDbService.findAll()
        tasks shouldHaveSize 4
    }

    @Test
    fun `test duplicated unique key and external key`() {
        taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(
                    CreateTask(TestInput(), uniqueKeys = listOf("Hey", "you!"), externalKey = "Yay!")
                ),
            )
        )

        var tasks = taskDbService.findAll()
        tasks shouldHaveSize 1

        val response = taskService.createTasks(
            CreateTaskRequest(
                taskGroupId = createTestTaskGroup().id,
                tasks = listOf(
                    CreateTask(TestInput(), uniqueKeys = listOf("2.1", "2.2"), externalKey = "2"),
                    CreateTask(TestInput(), uniqueKeys = listOf("Hey", "you!"), externalKey = "Yay!"),
                ),
            )
        )

        response.results[0].result shouldBe CreateTaskResult.OK
        response.results[1].result shouldBe CreateTaskResult.FAIL_EXTERNAL_KEY
        response.results[1].taskId shouldBe tasks[0].id
        response.results[1].failedUniqueKeys shouldBe mapOf("Hey" to tasks[0].id, "you!" to tasks[0].id)
    }

    @Test
    fun `it should fail on externalKey conflict`() {

    }

    @Test
    fun `it should fail on uniqueKey conflict`() {

    }

    @Test
    fun `when failed it should return other requests failed`() {

    }

    @Test
    fun `it should be able to recover from concurrent conflict failure`() {

    }
}
