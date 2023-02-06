package ru.yandex.market.markup3.core.services

import io.kotest.assertions.asClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.testutils.handlers.TestResult

class TaskResultServiceTest : CommonTaskTest() {
    @Test
    fun `test it stores and consumes results`() {
        val taskGroup = createTestTaskGroup()
        val taskId = createTestTask(taskGroup.id)
        taskResultService.storeResults(
            taskGroup.id, listOf(
                NewTaskResult(taskId, TestResult("hey")),
                NewTaskResult(taskId, TestResult("you"))
            )
        )

        val results = taskResultService.pollResults<TestResult>(taskGroup.id, 100)
        results shouldHaveSize 2
        results[0].asClue {
            it.taskId shouldBe taskId
            it.data.result shouldBe "hey"
        }

        taskResultService.consumeResults(taskGroup.id, listOf(results[0].taskResultId))

        val restResults = taskResultService.pollResults<TestResult>(taskGroup.id, 100)
        restResults shouldHaveSize 1
        restResults[0].asClue {
            it.data.result shouldBe "you"
        }
    }
}
