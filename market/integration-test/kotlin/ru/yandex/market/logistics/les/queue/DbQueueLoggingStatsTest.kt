package ru.yandex.market.logistics.les.queue

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.service.queue.consumer.DbEventConsumer
import ru.yandex.market.logistics.les.service.queue.dto.EventDto
import ru.yandex.market.logistics.les.util.toInternal
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant

class DbQueueLoggingStatsTest : AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    lateinit var dbEventConsumer: DbEventConsumer

    @BeforeEach
    fun setUp() {
        clock.setFixed(INSTANT, MOSCOW_ZONE)
    }

    @Test
    @Disabled
    //TODO(DELIVERY-44262): Временно отключен из-за флапа в CI.
    fun logStatsFromTaskWithoutTimestamps() {
        dbEventConsumer.processPayload(EventDto(QUEUE, null, null, INTERNAL_EVENT))
        val statsLogLine = backLogCaptor.results.findLast { it.contains("Event 1 was sent to test_q") }

        checkCommonLog(statsLogLine)
    }

    @Test
    fun logStatsFromTaskWithLesReceivedTimestampOnly() {
        dbEventConsumer.processPayload(
            EventDto(
                QUEUE,
                null,
                INSTANT.minusMillis(5),
                INTERNAL_EVENT
            )
        )
        val statsLogLine = backLogCaptor.results.filter { it.contains("Event 1 was sent to test_q") }.getOrNull(0)

        checkCommonLog(statsLogLine)
        statsLogLine!!.contains("extra_keys=lesReceived-lesSentDuration\textra_values=5") shouldBe true
    }

    @Test
    fun logStatsFromTaskWithAllTimestamps() {
        dbEventConsumer.processPayload(
            EventDto(
                QUEUE,
                INSTANT.minusMillis(10),
                INSTANT.minusMillis(5),
                INTERNAL_EVENT
            )
        )
        val statsLogLine = backLogCaptor.results.filter { it.contains("Event 1 was sent to test_q") }.getOrNull(0)

        println(statsLogLine)
        checkCommonLog(statsLogLine)
        statsLogLine!!.contains(
            "extra_keys=producerSent-lesSentDuration,lesReceived-lesSentDuration\textra_values=10,5"
        ) shouldBe true
    }

    private fun checkCommonLog(statsLogLine: String?) {
        assertSoftly {
            statsLogLine shouldNotBe null
            statsLogLine!!.contains("entity_types=eventId,eventType,source") shouldBe true
            statsLogLine.contains("entity_values=eventId:1,eventType:event_type,source:source") shouldBe true
        }
    }

    companion object {
        private val INTERNAL_EVENT = Event(
            "source",
            "1",
            0,
            "event_type",
            OrderDamagedEvent("123"),
            "description"
        ).toInternal()

        private const val QUEUE = "test_q"

        private val INSTANT = Instant.parse("2022-05-07T16:19:04Z")
    }
}
