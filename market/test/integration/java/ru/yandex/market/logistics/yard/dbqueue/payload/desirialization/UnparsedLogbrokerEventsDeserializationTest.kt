package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsPayload

@Import(ConsumerConfig::class)
class UnparsedLogbrokerEventsDeserializationTest(
    @Autowired private val consumer: UnparsedLogbrokerEventsConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"entity\": \"client_event\",\"line\":\"line\"}\n"

    @Test
    fun testDeserializationWorks() {
        val payload: UnparsedLogbrokerEventsPayload? = consumer.payloadTransformer.toObject(payloadString)

        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.line).isEqualTo("line")
        softly.assertThat(payload.entity).isEqualTo("client_event")
    }
}
