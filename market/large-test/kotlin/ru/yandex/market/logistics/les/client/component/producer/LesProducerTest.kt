package ru.yandex.market.logistics.les.client.component.producer

import java.time.Instant
import com.amazonaws.services.sqs.model.SendMessageRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.boxbot.CodeEvent
import ru.yandex.market.logistics.les.client.AbstractContextualTest
import ru.yandex.market.logistics.les.client.component.sqs.SqsRequestTraceTskvLogger
import ru.yandex.market.logistics.les.client.component.sqs.TraceableMessagePostProcessor
import ru.yandex.market.logistics.les.client.configuration.properties.TraceProperties
import ru.yandex.market.logistics.les.client.producer.LesProducer
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils

class LesProducerTest : AbstractContextualTest() {
    @SpyBean
    lateinit var logger: SqsRequestTraceTskvLogger

    @MockBean
    lateinit var lesTraceProperties: TraceProperties

    @MockBean
    lateinit var processor: TraceableMessagePostProcessor

    @Value("\${sqs.queues.test}")
    lateinit var queueName: String

    private val messageBody = IntegrationTestUtils.extractFileContent("large/sqs/event_body.json").trimIndent()

    @BeforeEach
    fun before() {
        jmsTemplate.receiveTimeout = 10000
        client.createQueue(queueName)
    }

    @Test
    @DisplayName("Отсылаем с дефолтными настройками")
    fun send() {
        getProducer().send(
            Event(
                "source",
                "event_id",
                Instant.parse("2017-12-02T12:00:00Z").toEpochMilli(),
                "event_type",
                CodeEvent("externalOrderId", "code"),
                "description"
            ),
            queueName
        )

        val captor = ArgumentCaptor.forClass(SendMessageRequest::class.java)

        verify(client).sendMessage(captor.capture())

        JSONAssert.assertEquals(messageBody, captor.value.messageBody, false)
    }

    private fun getProducer(): LesProducer = LesProducer(jmsTemplate, lesTraceProperties, processor, logger)
}
