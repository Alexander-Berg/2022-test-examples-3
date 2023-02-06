package ru.yandex.market.logistics.les.service.sqs

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.base.EventVersion
import ru.yandex.market.logistics.les.service.FlagService
import ru.yandex.market.logistics.les.service.RoutingService
import ru.yandex.market.logistics.les.service.YdbEventSavingService
import ru.yandex.market.logistics.les.util.EVENT_PROCESSING_CODE
import ru.yandex.market.logistics.les.util.parseTskRow
import ru.yandex.market.logistics.les.util.toInternal
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

class SqsEventConsumerTest : AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var routingService: RoutingService

    @Autowired
    private lateinit var ydbEventSavingService: YdbEventSavingService

    @Autowired
    private lateinit var flagService: FlagService

    private lateinit var service: EventServiceImpl

    @BeforeEach
    fun setupTest() {
        super.setup()
        service = EventServiceImpl(routingService, ydbEventSavingService, flagService)
    }

    @Test
    @Disabled
    //TODO(DELIVERY-44262): Временно отключен из-за флапа в CI.
    fun sendMetrics() {
        doReturn(setOf(TEST_QUEUE))
            .whenever(routingService)
            .getRouteQueues(
                sourceName = TEST_INTERNAL_EVENT.source,
                eventType = TEST_INTERNAL_EVENT.eventType,
            )
        doNothing()
            .whenever(ydbEventSavingService)
            .saveEventToYdbSync(TEST_INTERNAL_EVENT)
        service.processEvent(
            producerSentInstant = clock.instant(),
            lesReceivedInstant = clock.instant(),
            internalEvent = TEST_INTERNAL_EVENT
        )

        val statsLogLine = backLogCaptor.results
            .map { log -> parseTskRow(log) }
            .filter { log -> log.code == EVENT_PROCESSING_CODE }
        statsLogLine[0].extra shouldBe listOf(
            Pair("eventId", TEST_INTERNAL_EVENT.eventId),
            Pair("source", TEST_INTERNAL_EVENT.source),
            Pair("type", TEST_INTERNAL_EVENT.eventType),
            Pair("version", TEST_INTERNAL_EVENT.version.name)
        )
        statsLogLine[1].extra shouldBe listOf(
            Pair("eventId", TEST_INTERNAL_EVENT.eventId),
            Pair("source", TEST_INTERNAL_EVENT.source),
            Pair("type", TEST_INTERNAL_EVENT.eventType),
            Pair("version", TEST_INTERNAL_EVENT.version.name),
            Pair("queue", TEST_QUEUE),
        )
    }

    companion object {
        const val TEST_QUEUE = "TEST_QUEUE"
        val TEST_INTERNAL_EVENT = Event(
            "lom",
            "event_id_3",
            0,
            "test_type",
            OrderDamagedEvent("123"),
            "Тест",
        ).apply { version = EventVersion.VERSION_1 }
            .toInternal()
    }
}
