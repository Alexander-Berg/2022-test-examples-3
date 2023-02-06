package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStatePayload

@Import(ConsumerConfig::class)
class ProcessClientQueueDeserializationTest(
    @Autowired private val consumer: MoveClientToNextStateConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"currentEdgeId\":1}"

    @Test
    fun testDeserializationWorks() {
        val payload: MoveClientToNextStatePayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.currentEdgeId).isEqualTo(1)
    }

}

