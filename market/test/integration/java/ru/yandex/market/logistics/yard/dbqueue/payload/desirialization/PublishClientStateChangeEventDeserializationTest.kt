package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventPayload

@Import(ConsumerConfig::class)
class PublishClientStateChangeEventDeserializationTest(
    @Autowired private val consumer: PublishClientStateChangeEventConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"clientId\":1,\"stateFromId\":1,\"stateToId\":2}"

    @Test
    fun testDeserializationWorks() {
        val payload: PublishClientStateChangeEventPayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.clientId).isEqualTo(1)
        softly.assertThat(payload.stateFromId).isEqualTo(1)
        softly.assertThat(payload.stateToId).isEqualTo(2)
    }

}
