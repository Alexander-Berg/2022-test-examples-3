package ru.yandex.market.logistics.les.sqs

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import ru.yandex.market.logistics.les.AbstractLargeContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.base.EventPayload
import ru.yandex.market.logistics.les.base.EventVersion
import ru.yandex.market.logistics.les.configuration.CacheConfiguration.Companion.FLAG_CACHE
import ru.yandex.market.logistics.les.service.CacheService

class RoutingTest : AbstractLargeContextualTest() {
    @Value("\${sqs.queues.test}")
    lateinit var queueName: String

    @Autowired
    lateinit var cacheService: CacheService

    private var outQueueName1 = "test_queue_1"
    private var outQueueName2 = "test_queue_2"
    private var source = "test_source"
    private var eventType = "test_event_type"

    @BeforeEach
    fun setUp() {
        jmsTemplate.receiveTimeout = 10000
        client.createQueue(outQueueName1)
        client.createQueue(outQueueName2)
        cacheService.evict(FLAG_CACHE)
        createQueues(client, lesSqsProperties)
    }

    @Test
    @DatabaseSetup("/sqs/before/route.xml")
    fun routeEvent() {
        val event = Event(
            source,
            "event_id_3",
            0,
            eventType,
            OrderDamagedEvent("123"),
            null
        )

        jmsTemplate.convertAndSend(queueName, event)

        val resEvent1 = jmsTemplate.receiveAndConvert(outQueueName1) as Event
        assertEquals(resEvent1.version, EventVersion.VERSION_2)
        assertEquals(event, resEvent1.copy(version = EventVersion.VERSION_3))

        val resEvent2 = jmsTemplate.receiveAndConvert(outQueueName2) as Event
        assertEquals(resEvent2.version, EventVersion.VERSION_2)
        assertEquals(event, resEvent2.copy(version = EventVersion.VERSION_3))
    }


    @Test
    @DatabaseSetup("/sqs/before/route.xml")
    fun routeEventWithoutEntity() {
        val event = Event(
            source,
            "event_id_3",
            0,
            eventType,
            EntitylessEventPayload("123"),
            null
        )

        jmsTemplate.convertAndSend(queueName, event)

        val resEvent1 = jmsTemplate.receiveAndConvert(outQueueName1) as Event
        assertEquals(resEvent1.version, EventVersion.VERSION_2)
        assertEquals(event, resEvent1.copy(version = EventVersion.VERSION_3))

        val resEvent2 = jmsTemplate.receiveAndConvert(outQueueName2) as Event
        assertEquals(resEvent2.version, EventVersion.VERSION_2)
        assertEquals(event, resEvent2.copy(version = EventVersion.VERSION_3))

        verify(ydbEventRepository).create(any())
        verifyNoMoreInteractions(ydbEntityRepository)
    }

    data class EntitylessEventPayload(val field: String? = null) : EventPayload {
        override fun getEntityKeys(): List<EntityKey> = emptyList()
    }
}
