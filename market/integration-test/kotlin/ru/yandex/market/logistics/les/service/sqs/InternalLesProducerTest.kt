package ru.yandex.market.logistics.les.service.sqs

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.core.MessageCreator
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.util.toInternal
import javax.jms.Session

internal class InternalLesProducerTest : AbstractContextualTest() {

    companion object {
        val INTERNAL_EVENT = Event(
            source = "src",
            eventId = "id",
            timestamp = 0,
            eventType = "type",
            payload = null,
            description = null
        ).toInternal()

        const val QUEUE = "queue-159"
    }

    @Autowired
    lateinit var lesTraceProperties: TraceProperties

    @Autowired
    lateinit var processor: TraceableMessagePostProcessor

    @Mock
    lateinit var session: Session

    @Mock
    lateinit var sqsTextMessage: SQSTextMessage

    @Autowired
    lateinit var internalLesProducer: InternalLesProducer

    @Test
    fun sendWithPostProcess() {
        lesTraceProperties.usePostProcessor = true

        internalLesProducer.send(INTERNAL_EVENT, QUEUE)
        imitateSendMessageAndVerify()

        verify(sqsTextMessage).setObjectProperty(eq("meta_requestId"), any())
        verify(sqsTextMessage).setObjectProperty(eq("meta_timestamp"), any())
    }

    @Test
    fun sendNoPostProcess() {
        lesTraceProperties.usePostProcessor = false

        internalLesProducer.send(INTERNAL_EVENT, QUEUE)
        imitateSendMessageAndVerify()
    }

    private fun imitateSendMessageAndVerify(): SQSTextMessage {
        val messageCreatorCaptor = argumentCaptor<MessageCreator>()
        verify(jmsTemplate).send(eq(QUEUE), messageCreatorCaptor.capture())
        val messageCreator = messageCreatorCaptor.firstValue
        doReturn(sqsTextMessage).whenever(session).createTextMessage(eq(INTERNAL_EVENT.rawEvent))

        assertThat(messageCreator.createMessage(session)).isEqualTo(sqsTextMessage)
        verify(sqsTextMessage).setObjectProperty(eq("_type"), eq("ru.yandex.market.logistics.les.base.Event"))
        return sqsTextMessage
    }
}
