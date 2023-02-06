package ru.yandex.market.logistics.yard.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsPayload
import ru.yandex.market.logistics.yard_v2.dbqueue.unparsed_logbroker_events.UnparsedLogbrokerEventsProducer

@Import(ConsumerConfig::class)
class UnparsedLogbrokerEventsSerializationTest(
    @Autowired private val producer: UnparsedLogbrokerEventsProducer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"entity\":\"entity\",\"line\":\"line\"}"

    @Test
    fun testDeserializationWorks() {
        val unparsedLogbrokerEventsPayload = UnparsedLogbrokerEventsPayload("entity", "line")

        val payload = producer.payloadTransformer.fromObject(unparsedLogbrokerEventsPayload)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload).isEqualTo(payloadString)
    }
}
