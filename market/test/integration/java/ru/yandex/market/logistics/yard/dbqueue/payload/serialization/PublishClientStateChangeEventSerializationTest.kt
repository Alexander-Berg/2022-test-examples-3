package ru.yandex.market.logistics.yard.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventPayload
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventProducer

@Import(ConsumerConfig::class)
class PublishClientStateChangeEventSerializationTest(
    @Autowired private val producer: PublishClientStateChangeEventProducer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"clientId\":1,\"stateFromId\":1,\"stateToId\":2}"

    @Test
    fun testSerializeWorks() {
        val payload = PublishClientStateChangeEventPayload(1, 1, 2)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}
