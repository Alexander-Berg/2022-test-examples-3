package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.process_client_queue_by_state_to_id.ProcessClientQueueByStateToIdConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.process_client_queue_by_state_to_id.ProcessClientQueueByStateToIdPayload

@Import(ConsumerConfig::class)
class ProcessClientQueueByStateToIdDeserializationTest(@Autowired private val consumer: ProcessClientQueueByStateToIdConsumer) :
    AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"stateToId\":1}"

    @Test
    fun testDeserializationWorks() {
        val payload: ProcessClientQueueByStateToIdPayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.stateToId).isEqualTo(1)
    }

}
