package ru.yandex.market.logistics.les.sqs

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import java.time.Instant
import javax.jms.Queue
import javax.jms.Session
import javax.jms.Topic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.jms.support.converter.MessageConverter
import ru.yandex.market.logistics.les.AbstractContextualIntegrationTest
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.boxbot.CodeEvent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent

class ConverterTest : AbstractContextualIntegrationTest() {
    @SpyBean
    lateinit var converter: MessageConverter

    private val messageBody = extractFileContent("integration/sqs/event_body.json").trimIndent()

    @Test
    fun toMessage() {
        val mes = converter.toMessage(event(), sessionMock()) as SQSTextMessage
        JSONAssert.assertEquals(/* expectedStr = */ messageBody, /* actualStr = */ mes.text, false)
    }

    @Test
    fun fromMessage() {
        val sqsMessage = SQSTextMessage(messageBody)
        sqsMessage.setStringProperty("_type", Event::class.java.name)
        val event = converter.fromMessage(sqsMessage) as Event
        assertEquals(/* expected = */ event(), /* actual = */ event)
    }

    private fun event(): Event = Event(
        source = "source",
        eventId = "event_id",
        timestamp = Instant.parse("2017-12-02T12:00:00Z").toEpochMilli(),
        eventType = "event_type",
        payload = CodeEvent("externalOrderId", "code"),
        description = "description",
        entityKeys = null,
        features = mapOf(),
        version = null
    )

    private fun sessionMock(): Session {
        val ses: Session = mock(Session::class.java)
        whenever(ses.createTopic(anyString())).thenReturn(mock(Topic::class.java))
        whenever(ses.createQueue(anyString())).thenReturn(mock(Queue::class.java))
        whenever(ses.createTextMessage(anyString())).thenAnswer {
            SQSTextMessage(it.arguments[0] as String?)
        }
        return ses
    }
}
