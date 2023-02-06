package ru.yandex.market.logistics.les.sqs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractLargeContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.service.sqs.EventService
import ru.yandex.market.logistics.les.util.toInternal

class ConsumerTest : AbstractLargeContextualTest() {

    @Value("\${sqs.queues.test}")
    lateinit var queueName: String

    @MockBean
    lateinit var eventService: EventService

    @BeforeEach
    fun setUp() {
        createQueues(client, lesSqsProperties)
    }

    @Test
    fun consumeEvent() {
        val event = Event(
            "lom",
            "event_id_3",
            0,
            "test_type",
            OrderDamagedEvent("123"),
            "Тест"
        )
        val internalEvent = event.toInternal()

        jmsTemplate.convertAndSend(queueName, event)
        verify(eventService, timeout(10000).atLeastOnce()).processEvent(anyOrNull(), any(), eq(internalEvent))
    }
}
