package ru.yandex.market.markup3.testutils.handlers

import ru.yandex.market.markup3.core.dto.Task
import ru.yandex.market.markup3.core.dto.TaskEventRow
import ru.yandex.market.markup3.core.dto.TaskTypeHandle
import ru.yandex.market.markup3.core.events.Event
import ru.yandex.market.markup3.core.services.HandlerResult
import ru.yandex.market.markup3.core.services.RetryPolicy
import ru.yandex.market.markup3.core.services.TaskHandler
import ru.yandex.market.markup3.core.services.TaskHandlerRegistry
import ru.yandex.market.markup3.tasks.TaskType
import java.util.LinkedList

data class TestInput(val n: Int = 0)
data class TestResult(val result: String)
data class TestState(val data: String)

typealias CallHandler = (Task<TestInput, TestState>, Event) -> HandlerResult<TestState, TestResult>

class TestHandler(handlerRegistry: TaskHandlerRegistry) : TaskHandler<TestInput, TestState, TestResult> {
    override val handle = TaskTypeHandle(TestHandler::class.java, TaskType.DICE)

    init {
        handlerRegistry.replace(this)
    }


    var nextDelay: Long? = 0

    override val retryPolicy: RetryPolicy
        get() = object : RetryPolicy {
            override fun shouldRetry(event: TaskEventRow): Long? = nextDelay
        }

    private val handlers = LinkedList<CallHandler>()

    fun reset() {
        nextDelay = 0
        handlers.clear()
    }


    override fun initialState(input: TestInput) = TestState(data = "Hello")

    override fun handleEvent(task: Task<TestInput, TestState>, event: Event): HandlerResult<TestState, TestResult> {
        return handlers.poll()?.invoke(task, event)
            ?: throw IllegalStateException("no handlers: call .handleNext { .. }")
    }

    fun handleNext(handler: CallHandler) = handlers.add(handler)
}
