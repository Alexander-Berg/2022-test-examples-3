package ru.yandex.market.logistics.les.sqs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualIntegrationTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.producer.LesProducer

class UsePostProcessorFlagTest : AbstractContextualIntegrationTest() {

    @Autowired
    lateinit var producer: LesProducer

    @Autowired
    lateinit var sqsTraceProperties: TraceProperties

    @Autowired
    lateinit var traceableMessagePostProcessor: TraceableMessagePostProcessor

    @Test
    fun flagEnabled() {
        whenever(sqsTraceProperties.usePostProcessor).thenReturn(true)

        producer.send(EVENT, QUEUE)

        verify(jmsTemplate).convertAndSend(QUEUE, EVENT, traceableMessagePostProcessor)
        verifyNoMoreInteractions(jmsTemplate)
    }

    @Test
    fun flagDisabled() {
        whenever(sqsTraceProperties.usePostProcessor).thenReturn(false)

        producer.send(EVENT, QUEUE)

        verify(jmsTemplate).convertAndSend(QUEUE, EVENT)
        verifyNoMoreInteractions(jmsTemplate)
    }

    companion object {
        private val EVENT = Event(
            "source",
            "1",
            0,
            "event_type",
            OrderDamagedEvent("123"),
            "description"
        )

        private const val QUEUE = "queueueue"
    }
}
