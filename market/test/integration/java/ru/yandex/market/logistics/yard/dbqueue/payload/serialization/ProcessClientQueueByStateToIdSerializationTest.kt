package ru.yandex.market.logistics.yard.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.process_client_queue_by_state_to_id.ProcessClientQueueByStateToIdPayload
import ru.yandex.market.logistics.yard_v2.dbqueue.process_client_queue_by_state_to_id.ProcessClientQueueByStateToIdProducer

class ProcessClientQueueByStateToIdSerializationTest(
    @Autowired private val producer: ProcessClientQueueByStateToIdProducer
) : AbstractSecurityMockedContextualTest() {
    private val payloadString = "{\"stateToId\":0}"

    @Test
    fun testSerializeWorks() {
        val payload = ProcessClientQueueByStateToIdPayload(0)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }
}
