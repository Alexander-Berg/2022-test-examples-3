package ru.yandex.market.logistics.logistrator.queue.processor

import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import ru.yandex.market.logistics.dbqueue.QueueProcessor
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.logistrator.queue.payload.RequestIdPayload
import ru.yandex.market.logistics.logistrator.utils.REQUEST_ID_PAYLOAD
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult

abstract class AbstractQueueProcessorTest<P : QueueProcessor<RequestIdPayload>>(
    private val nextProcessorClass: Class<out QueueProcessor<RequestIdPayload>>? = null
) : AbstractContextualTest() {

    fun abstractTestExecute(
        processor: P,
        initializeMocks: () -> Unit = {},
        verifyExecution: () -> Unit
    ) {
        nextProcessorClass?.let {
            doNothing().whenever(dbQueueService).produceTask(eq(nextProcessorClass), eq(REQUEST_ID_PAYLOAD))
        }

        initializeMocks()

        assertSoftly {
            processor.execute(REQUEST_ID_PAYLOAD) shouldBe TaskExecutionResult.finish()
        }

        verifyExecution()

        nextProcessorClass?.let {
            verify(dbQueueService).produceTask(eq(nextProcessorClass), eq(REQUEST_ID_PAYLOAD))
        }

        verifyNoMoreInteractions(lmsClient)
        verifyNoMoreInteractions(dbQueueService)
    }
}
