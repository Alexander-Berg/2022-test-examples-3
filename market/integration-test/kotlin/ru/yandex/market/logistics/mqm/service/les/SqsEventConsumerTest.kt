package ru.yandex.market.logistics.mqm.service.les

import com.amazon.sqs.javamessaging.SQSQueueDestination
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.ProcessLesEventProperties
import ru.yandex.market.logistics.mqm.queue.producer.ProcessLesEventProducer

class SqsEventConsumerTest: AbstractContextualTest() {

    @Autowired
    lateinit var processLesEventProducer: ProcessLesEventProducer

    @Test
    @ExpectedDatabase(
        "/service/les/consumer/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun success() {
        val destination = Mockito.mock(SQSQueueDestination::class.java)
        whenever(destination.queueName).thenReturn("testQueue")
        val sqsEventConsumer = SqsEventConsumer(processLesEventProducer, ProcessLesEventProperties(true))
        sqsEventConsumer.processEvent(
            destination, "123", Event(
                source = "testSource",
                eventId = "testEventId",
                timestamp = null,
                eventType = "testEventType",
                payload = null,
                description = "testDescription"
            )
        )
    }
}
