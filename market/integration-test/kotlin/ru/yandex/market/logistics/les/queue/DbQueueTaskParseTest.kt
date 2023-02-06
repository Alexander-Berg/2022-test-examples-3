package ru.yandex.market.logistics.les.queue

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.OrderDamagedEvent
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.service.queue.consumer.BaseQueueConsumer
import ru.yandex.market.logistics.les.service.queue.dto.EventDto
import ru.yandex.market.logistics.les.util.toInternal
import java.time.Instant

class DbQueueTaskParseTest : AbstractContextualTest() {

    @Autowired
    lateinit var queueConsumer: BaseQueueConsumer<EventDto>

    @Test
    fun parseTaskWithTimestamps() {
        val eventDto = queueConsumer.payloadTransformer.toObject(
            """
                {
                    $STRING_EVENT,
                    "queue":"test_queue"
                }
            """.trimIndent()
        )

        eventDto shouldBe EventDto("test_queue", null, null, INTERNAL_EVENT)
    }

    @Test
    fun parseTaskWithOneTimestamp() {
        val eventDto = queueConsumer.payloadTransformer.toObject(
            """
                {
                    $STRING_EVENT,
                    "queue":"test_queue",
                    "lesReceivedInstant":"2022-04-07T16:14:13Z"  
                }
            """.trimIndent()
        )

        eventDto shouldBe EventDto(
            "test_queue",
            null,
            Instant.parse("2022-04-07T16:14:13Z"),
            INTERNAL_EVENT
        )
    }

    @Test
    fun parseTaskWithAllTimestamps() {
        val eventDto = queueConsumer.payloadTransformer.toObject(
            """
                {
                    $STRING_EVENT,
                    "queue":"test_queue",
                    "lesReceivedInstant":"2022-04-07T16:14:13Z",
                    "producerSentInstant":"2022-04-07T16:10:13Z"
                }
            """.trimIndent()
        )

        eventDto shouldBe EventDto(
            "test_queue",
            Instant.parse("2022-04-07T16:10:13Z"),
            Instant.parse("2022-04-07T16:14:13Z"),
            INTERNAL_EVENT
        )
    }


    companion object {
        private val INTERNAL_EVENT = Event(
            "source",
            "1",
            1625130000000,
            "event_type",
            OrderDamagedEvent("123"),
            "description"
        ).toInternal()

        private val STRING_EVENT = """
            "internalEvent": {
                "rawEvent": "{\"source\":\"source\",\"timestamp\":1625130000000,\"payload\":{\"_type\":\"ru.yandex.market.logistics.les.OrderDamagedEvent\",\"externalOrderId\":\"123\"},\"description\":\"description\",\"features\":{},\"version\":\"VERSION_3\",\"eventId\":\"1\",\"eventType\":\"event_type\",\"entityKeys\":[{\"entityId\":\"123\",\"entityType\":\"ORDER\"}]}",
                "source": "source",
                "eventId": 1,
                "timestamp": 1625130000000,
                "eventType": "event_type",
                "version": "VERSION_3",
                "sensitive": false,
                "description": "description",
                "entityKeys": [{"entityId": "123", "entityType": "ORDER"}]
            }
        """.trimIndent()
    }
}
