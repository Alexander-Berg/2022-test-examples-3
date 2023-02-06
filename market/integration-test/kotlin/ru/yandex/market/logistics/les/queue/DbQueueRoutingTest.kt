package ru.yandex.market.logistics.les.queue

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.service.RoutingService
import ru.yandex.market.logistics.les.service.queue.consumer.DbEventConsumer
import ru.yandex.market.logistics.les.service.queue.dto.EventDto
import ru.yandex.market.logistics.les.util.toInternal
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId


class DbQueueRoutingTest : AbstractContextualTest() {
    @MockBean
    lateinit var routingService: RoutingService

    @Autowired
    lateinit var dbEventConsumer: DbEventConsumer

    private val queue = "test_queue"
    private val internalEvent = Event(
        "source",
        "1",
        0,
        "event_type",
        OrderDamagedEvent("123"),
        "description"
    ).toInternal()
    private lateinit var eventDto: EventDto

    @BeforeEach
    fun setUp() {
        internalEvent.timestamp = clock.instant().toEpochMilli()
        eventDto = EventDto(
            queue,
            clock.instant(),
            clock.instant(),
            internalEvent
        )
    }

    @Test
    @DatabaseSetup("/queue/before/fail_over_max.xml")
    @ExpectedDatabase(
        "/queue/after/fail_over_max.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun failOverMax() {
        whenever(routingService.routeSingleQueueEvent(eventDto)).thenThrow(RuntimeException("Test exception message"))
        dbEventConsumer.execute(createTask(5))
        verify(routingService).routeSingleQueueEvent(eventDto)
    }

    @Test
    @DatabaseSetup("/queue/before/enqueue.xml")
    @ExpectedDatabase(
        "/queue/after/first_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun failFirst() {
        whenever(routingService.routeSingleQueueEvent(eventDto)).thenThrow(RuntimeException("test"))
        dbEventConsumer.execute(createTask(0))
        verify(routingService).routeSingleQueueEvent(eventDto)
    }

    private fun createTask(attemptsCount: Long): Task<EventDto> {
        return Task.builder<EventDto>(QueueShardId("mainDbQueueShard"))
            .withAttemptsCount(attemptsCount)
            .withReenqueueAttemptsCount(0)
            .withTotalAttemptsCount(0)
            .withPayload(eventDto)
            .build()
    }
}
