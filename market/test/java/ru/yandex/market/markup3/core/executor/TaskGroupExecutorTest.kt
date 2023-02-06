package ru.yandex.market.markup3.core.executor

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.asClue
import io.kotest.assertions.fail
import io.kotest.assertions.forEachAsClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowMapper
import ru.yandex.market.markup3.core.dto.ProcessingStatus
import ru.yandex.market.markup3.core.dto.TaskEventStatus
import ru.yandex.market.markup3.core.dto.TaskGroup
import ru.yandex.market.markup3.core.events.ChildEvent
import ru.yandex.market.markup3.core.events.CoreEvent
import ru.yandex.market.markup3.core.executor.TaskGroupExecutor.StepResult
import ru.yandex.market.markup3.core.services.HandlerResult
import ru.yandex.market.markup3.core.services.SendEvent
import ru.yandex.market.markup3.core.services.TestEventObject
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.markup3.testutils.handlers.TestResult
import ru.yandex.market.markup3.testutils.handlers.TestState
import ru.yandex.market.markup3.utils.CommonObjectMapper
import java.time.Instant

class TaskGroupExecutorTest : CommonTaskTest() {
    @Autowired
    lateinit var executorProvider: TaskGroupExecutorProvider

    lateinit var executor: TaskGroupExecutor

    lateinit var taskGroup: TaskGroup

    @Before
    fun setup() {
        taskGroup = createTestTaskGroup()
        executor = executorProvider.createTaskGroupExecutor(taskGroup, sleepTimeMs = 0)
    }

    @After
    fun shutdown() {
        Mockito.reset(jdbcTemplate)
    }

    @Test
    fun `it should take and execute tasks`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        testHandler.handleNext { task, event ->
            task.id shouldBe taskId
            event shouldBe CoreEvent.Init
            HandlerResult()
        }

        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)
        val events = taskEventDbService.findByTaskId(taskId)
        events shouldHaveSize 1
        events[0].asClue {
            it.retries shouldBe 0
            it.status shouldBe TaskEventStatus.PROCESSED
        }
    }

    @Test
    fun `it should store logs`() {
        val taskId = createTestTask(taskGroup.id)

        val eventsBefore = taskEventDbService.findByTaskId(taskId)
        eventsBefore shouldHaveSize 1
        val logBefore = taskEventDbService.findEventLog(eventsBefore[0].id)
        logBefore.shouldBeEmpty()

        // Call next
        testHandler.handleNext { task, event ->
            task.id shouldBe taskId
            event shouldBe CoreEvent.Init

            val logWhileRunning = taskEventDbService.findEventLog(eventsBefore[0].id)
            logWhileRunning shouldHaveSize 1
            logWhileRunning[0].asClue {
                it.trace shouldNotBe null
                it.finished shouldBe null
            }

            HandlerResult(
                changeStage = "worked",
                changeProcessingStatus = ProcessingStatus.DONE,
                changeState = TestState("Worked")
            )
        }

        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)
        val events = taskEventDbService.findByTaskId(taskId)
        events shouldHaveSize 1
        events[0].asClue {
            it.retries shouldBe 0
            it.status shouldBe TaskEventStatus.PROCESSED
        }

        val logAfter = taskEventDbService.findEventLog(eventsBefore[0].id)
        logAfter shouldHaveSize 1
        logAfter[0].asClue {
            it.changeStage shouldBe "worked"
            it.changeState.toString() shouldBe """{"data":"Worked"}"""
            it.changeProcessingStatus shouldBe ProcessingStatus.DONE
            it.success shouldBe true
            it.finished shouldNotBe null
        }
    }

    @Test
    fun `state should be updated`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        testHandler.handleNext { task, event ->
            task.id shouldBe taskId
            event shouldBe CoreEvent.Init
            HandlerResult(changeState = TestState("Touched!"), changeStage = "Worked")
        }

        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)
        val events = taskEventDbService.findByTaskId(taskId)
        events shouldHaveSize 1
        events[0].asClue {
            it.retries shouldBe 0
            it.status shouldBe TaskEventStatus.PROCESSED
        }

        testHandler.handleNext { task, _ ->
            task.stage shouldBe "Worked"
            task.state.data shouldBe "Touched!"
            HandlerResult()
        }
        taskEventService.sendEvents(SendEvent(taskId, TestEventObject))
        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)

        val events2 = taskEventDbService.findByTaskId(taskId)
        events2 shouldHaveSize 2
        events2.forEachAsClue { it.status shouldBe TaskEventStatus.PROCESSED }
    }

    @Test
    fun `it should save results`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        testHandler.handleNext { task, event ->
            task.id shouldBe taskId
            event shouldBe CoreEvent.Init
            HandlerResult(results = listOf(TestResult("Yay!"), TestResult("Yoy!")))
        }

        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)
        val events = taskEventDbService.findByTaskId(taskId)
        events shouldHaveSize 1
        events[0].asClue {
            it.retries shouldBe 0
            it.status shouldBe TaskEventStatus.PROCESSED
        }

        val results = taskResultService.pollResults<TestResult>(taskGroup.id, 100)
        results shouldHaveSize 2
        results.map { it.data.result } shouldContainInOrder listOf("Yay!", "Yoy!")
    }

    @Test
    fun `it should reschedule failing tasks`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        testHandler.nextDelay = -5 // Schedule in past so we take it
        testHandler.handleNext { _, _ -> throw IllegalStateException("Destined to fail!") }

        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)

        val log = taskEventDbService.findEventLog(taskEventDbService.findByTaskId(taskId)[0].id)[0]
        log.asClue {
            it.finished shouldNotBe null
            it.success shouldBe false
            it.exception shouldContain "Destined to fail!"
        }

        val events = taskEventDbService.findByTaskId(taskId)
        events[0].asClue {
            it.retries shouldBe 1
            it.status shouldBe TaskEventStatus.ACTIVE
            it.nextRun shouldBeBefore Instant.now()
            it.lastMessage shouldContain "Destined to fail!"
        }
    }

    @Test
    fun `it should fail task after retries exhausted`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        testHandler.nextDelay = -1
        testHandler.handleNext { _, _ -> throw IllegalStateException("Should fail!") }
        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // First try

        testHandler.nextDelay = null // Don't retry (test policy)
        testHandler.handleNext { _, _ -> throw IllegalStateException("Should fail!") }
        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED // Had event (Init)

        val events = taskEventDbService.findByTaskId(taskId)
        events[0].asClue {
            it.retries shouldBe 1
            it.status shouldBe TaskEventStatus.FAILED
            it.lastMessage shouldContain "Should fail!"
        }
    }

    @Test
    fun `test system error`() {
        val taskId = createTestTask(taskGroup.id)

        // Call next
        doThrow(IllegalStateException("KILL DB!"))
            .whenever(jdbcTemplate)
            .query(any(), any<Map<String, Any>>(), any<RowMapper<*>>())

        testHandler.handleNext { _, _ -> fail("Shouldn't be called") }
        executor.processSingleEvent() shouldBe StepResult.SYSTEM_ERROR
        Mockito.reset(jdbcTemplate)

        val events = taskEventDbService.findByTaskId(taskId)
        events shouldHaveSize 1
        events[0].asClue {
            it.status shouldBe TaskEventStatus.ACTIVE
            it.retries shouldBe 0
            it.lastMessage shouldBe null
        }
    }

    @Test
    fun `test concurrent calls`() {
        // TODO довольно сложный тест для реализации
    }

    @Test
    fun `it should generate child events after processing child`() {
        testProcessingChildTaskWithStatus(ProcessingStatus.DONE)
    }

    @Test
    fun `it should generate cancelled child status events when child cancelled`() {
        testProcessingChildTaskWithStatus(ProcessingStatus.CANCELLED)
    }

    private fun testProcessingChildTaskWithStatus(childStatus: ProcessingStatus) {
        val parentTaskId = createTestTask(taskGroup.id)
        val taskId = createTestTask(taskGroup.id, parentTaskId = parentTaskId)

        // Call next
        testHandler.handleNext { task, event ->
            task.id shouldBe parentTaskId
            HandlerResult()
        }
        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED
        testHandler.handleNext { task, event ->
            task.id shouldBe taskId
            task.parentTaskId shouldBe parentTaskId
            event shouldBe CoreEvent.Init

            HandlerResult(
                changeStage = "worked",
                changeProcessingStatus = childStatus,
                changeState = TestState("Worked"),
                results = listOf(TestResult("Yay!"), TestResult("Yoy!"))
            )
        }
        executor.processSingleEvent() shouldBe StepResult.EVENT_PROCESSED

        val childEvents = taskEventDbService.findByTaskId(taskId)
        childEvents shouldHaveSize 1
        childEvents[0].asClue {
            it.retries shouldBe 0
            it.status shouldBe TaskEventStatus.PROCESSED
        }
        val parentEvents = taskEventDbService.findByTaskIdsWithData(listOf(parentTaskId))
        val childResultEvents = parentEvents.filter { it.event.eventType == "ChildResultEvent" }
        childResultEvents shouldHaveSize 2
        childResultEvents.forEach {
            it.event.retries shouldBe 0
            it.event.status shouldBe TaskEventStatus.ACTIVE
            val event = CommonObjectMapper.treeToValue(it.data.data, ChildEvent.ChildResultEvent::class.java)
            event.childTaskId shouldBe taskId
        }
        val childLifecycleEvents = parentEvents.filter { it.event.eventType == "ChildLifecycleEvent" }
        childLifecycleEvents shouldHaveSize 1
        childLifecycleEvents.forEach {
            it.event.retries shouldBe 0
            it.event.status shouldBe TaskEventStatus.ACTIVE
            val event = CommonObjectMapper.treeToValue(it.data.data, ChildEvent.ChildLifecycleEvent::class.java)
            event.childTaskId shouldBe taskId
            event.childStatus shouldBe childStatus
        }

        val results = taskResultService.pollResults<TestResult>(taskGroup.id, 100)
        results shouldHaveSize 2
        results.map { it.data.result } shouldContainInOrder listOf("Yay!", "Yoy!")
    }
}
