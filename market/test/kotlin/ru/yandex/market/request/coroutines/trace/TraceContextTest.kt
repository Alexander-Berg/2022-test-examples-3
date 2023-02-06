package ru.yandex.market.request.coroutines.trace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import ru.yandex.market.request.context.impl.MDCContext.REQUEST_ID
import ru.yandex.market.request.trace.RequestContext
import ru.yandex.market.request.trace.RequestContextHolder

/**
 * Тесты для [TraceContext].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class TraceContextTest {

    @Test
    fun `request_id will be passed into coroutine's context`() {
        val request = RequestContextHolder.createNewContext()
        val testThreadId = Thread.currentThread().id

        // Стартуем корутину на новом треде, тк на текущем трассировка уже сохранена
        runBlocking(Dispatchers.Default + MDCContext() + TraceContext()) {
            Assertions.assertNotEquals(testThreadId, Thread.currentThread().id)

            val contextInCoroutine = RequestContextHolder.getContext()
            Assertions.assertEquals(request.requestId, contextInCoroutine.requestId)
        }
    }

    @Test
    fun `test alias for context creation`() {
        val request = RequestContextHolder.createNewContext()
        val testThreadId = Thread.currentThread().id

        // Стартуем корутину на новом треде, тк на текущем трассировка уже сохранена
        runBlocking(Dispatchers.Default + traceContext()) {
            Assertions.assertNotEquals(testThreadId, Thread.currentThread().id)

            val contextInCoroutine = RequestContextHolder.getContext()
            Assertions.assertEquals(request.requestId, contextInCoroutine.requestId)
            Assertions.assertEquals(request.requestId, MDC.get(REQUEST_ID))
        }
    }

    @Test
    fun `change trace context`() {
        val request = RequestContextHolder.createNewContext()

        runBlocking(traceContext()) {
            Assertions.assertEquals(request.requestId, RequestContextHolder.getContext().requestId)

            val nextRequest = RequestContext(request.nextSubReqId)
            withContext(traceContext(nextRequest)) {
                Assertions.assertEquals(nextRequest.requestId, RequestContextHolder.getContext().requestId)
            }

            Assertions.assertEquals(request.requestId, RequestContextHolder.getContext().requestId)
        }
    }
}
