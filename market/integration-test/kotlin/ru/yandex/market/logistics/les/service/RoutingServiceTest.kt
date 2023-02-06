package ru.yandex.market.logistics.les.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.service.queue.dto.EventDto
import ru.yandex.market.logistics.les.service.queue.dto.MultiQueueEventDto
import ru.yandex.market.logistics.les.service.queue.producer.DbEventProducer
import ru.yandex.market.logistics.les.service.sqs.InternalLesProducer
import ru.yandex.market.logistics.les.util.toInternal
import ru.yandex.money.common.dbqueue.api.EnqueueParams
import java.time.Instant

internal class RoutingServiceTest : AbstractContextualTest() {

    companion object {
        const val SOURCE = "test-source"
        const val EVENT_TYPE = "test-event-type"
        val INTERNAL_EVENT = Event(
            source = SOURCE,
            eventType = EVENT_TYPE,
            eventId = "event_id",
            timestamp = 0,
            payload = null,
            description = null
        ).toInternal()
            .let { it.copy(rawEvent = it.rawEvent.replaceFirst("\"VERSION_3\"", "\"VERSION_2\"")) }
        val RECEIVED_INSTANT = Instant.parse("2007-12-03T10:15:30.00Z")
        val EVENT_DTO = EventDto(
            queue = "test-queue-1",
            producerSentInstant = null,
            lesReceivedInstant = RECEIVED_INSTANT,
            internalEvent = INTERNAL_EVENT
        )
        val QUEUES = listOf("queue-1", "queue-2", "queue-3")
        val MULTI_EVENT_DTO = MultiQueueEventDto(
            queues = QUEUES.toSet(),
            producerSentInstant = null,
            lesReceivedInstant = RECEIVED_INSTANT,
            internalEvent = INTERNAL_EVENT
        )
    }

    @Autowired
    lateinit var cacheService: CacheService

    @Autowired
    lateinit var internalLesProducer: InternalLesProducer

    @Autowired
    lateinit var dbEventProducer: DbEventProducer

    @Autowired
    lateinit var routingService: RoutingService

    @BeforeEach
    internal fun setUp() {
        doNothing().whenever(internalLesProducer).send(any(), any())
        doReturn(1L).whenever(dbEventProducer).enqueue(any())
        cacheService.evictAll()
    }

    @Test
    @DatabaseSetup("/routing/before/route_queues.xml")
    fun getRouteQueues() {
        assertThat(
            routingService.getRouteQueues(SOURCE, EVENT_TYPE)
        )
            .containsExactlyInAnyOrder("queue-1", "queue-2")
    }

    @Test
    fun routeSingleQueueEvent() {
        routingService.routeSingleQueueEvent(EVENT_DTO)

        verify(internalLesProducer).send(eq(INTERNAL_EVENT), eq(EVENT_DTO.queue))
    }

    @Test
    fun routeMultiQueueEventThroughDbQueue() {
        routingService.routeMultiQueueEvent(MULTI_EVENT_DTO)

        QUEUES.forEach { queue ->
            verify(dbEventProducer).enqueue(
                eq(
                    EnqueueParams.create(getEventDto(queue))
                )
            )
        }
    }

    @Test
    @DatabaseSetup("/routing/before/route_events_in_flight.xml")
    fun routeMultiQueueEventInFlight() {
        routingService.routeMultiQueueEvent(MULTI_EVENT_DTO)

        QUEUES.forEach { queue ->
            verify(internalLesProducer).send(eq(INTERNAL_EVENT), eq(queue))
        }
    }

    @Test
    @DatabaseSetup("/routing/before/route_events_in_flight.xml")
    fun routeMultiQueueEventInFlightWithFallbackToDbQueue() {
        doThrow(RuntimeException::class).whenever(internalLesProducer).send(any(), eq(QUEUES[0]))

        routingService.routeMultiQueueEvent(MULTI_EVENT_DTO)

        verify(dbEventProducer).enqueue(eq(EnqueueParams.create(getEventDto(QUEUES[0]))))

        QUEUES.forEach { queue ->
            verify(internalLesProducer).send(eq(INTERNAL_EVENT), eq(queue))
        }
    }

    @Test
    @DatabaseSetup("/routing/before/route_events_in_flight_no_fallback_to_db_queue.xml")
    fun routeMultiQueueEventInFlightNoFallbackToDbQueue() {
        doThrow(RuntimeException::class).whenever(internalLesProducer).send(any(), eq(QUEUES[0]))

        assertThrows<RuntimeException> { routingService.routeMultiQueueEvent(MULTI_EVENT_DTO) }

        QUEUES.forEach { queue ->
            verify(internalLesProducer).send(eq(INTERNAL_EVENT), eq(queue))
        }
    }

    @AfterEach
    internal fun tearDown() {
        verifyNoMoreInteractions(dbEventProducer, internalLesProducer)
    }

    private fun getEventDto(queue: String) = EventDto(
        queue = queue,
        producerSentInstant = null,
        lesReceivedInstant = RECEIVED_INSTANT,
        internalEvent = INTERNAL_EVENT
    )
}
