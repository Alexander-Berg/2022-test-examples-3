package ru.yandex.market.logistics.yard.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStatePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStateQueueProducer

class ProcessClientQueueSerializationTest(@Autowired private val moveClientToNextStateQueueProducer: MoveClientToNextStateQueueProducer) :
    AbstractSecurityMockedContextualTest() {
    private val payloadString = "{\"clientId\":0,\"currentEdgeId\":1}"

    @Test
    fun testSerializeWorks() {
        val payload = MoveClientToNextStatePayload(0, 1)
        val payloadString: String? = moveClientToNextStateQueueProducer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }
}
