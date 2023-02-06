package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.logging.MonitoringEventTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.SendTelegramMessageEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor

class SendTelegramMessageEventConsumerTest : AbstractContextualTest() {

    @Autowired
    private lateinit var consumer: SendTelegramMessageEventConsumer

    @Autowired
    private lateinit var pechkinHttpClient: PechkinHttpClient

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(MonitoringEventTskvLogger.getLoggerName())

    @Test
    fun sendTelegramMessageTest() {
        consumer.processPayload(
            SendTelegramMessagePayload(
                "-1434325324",
                "Thing happened",
                "me"
            ),
            null
        )

        val captor = ArgumentCaptor.forClass(MessageDto::class.java)
        verify(pechkinHttpClient).sendMessage(captor.capture())

        val message = captor.value

        assertSoftly {
            message.channel shouldBe "-1434325324"
            message.message shouldBe "Thing happened"
            message.sender shouldBe "me"
        }

        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=SEND_TELEGRAM_MESSAGE\t" +
                "eventPayload=SendTelegramMessagePayload(channel=-1434325324, message=Thing happened, sender=me)\t" +
                "message=Telegram message was sent\t" +
                "extraKeys=sender,channel,message\t" +
                "extraValues=me,-1434325324,Thing happened"
        }
    }

    @Test
    fun sendTelegramMessageWithDefaultSenderTest() {
        consumer.processPayload(
            SendTelegramMessagePayload(
                "-1434325324",
                "Thing happened"
            ),
            null
        )

        val captor = ArgumentCaptor.forClass(MessageDto::class.java)
        verify(pechkinHttpClient).sendMessage(captor.capture())

        val message = captor.value

        assertSoftly {
            message.channel shouldBe "-1434325324"
            message.message shouldBe "Thing happened"
            message.sender shouldBe "MQM Monitoring"
        }
    }
}
