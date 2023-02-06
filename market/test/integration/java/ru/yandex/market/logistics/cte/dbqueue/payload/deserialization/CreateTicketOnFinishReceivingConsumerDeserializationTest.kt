package ru.yandex.market.logistics.cte.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.config.ConsumerConfig
import ru.yandex.market.logistics.cte.dbqueue.consumer.CreateTicketOnFinishReceivingConsumer
import ru.yandex.market.logistics.cte.dbqueue.payload.CreateTicketOnFinishReceivingPayload

@Import(ConsumerConfig::class)
class CreateTicketOnFinishReceivingConsumerDeserializationTest(
    @Autowired private val consumer: CreateTicketOnFinishReceivingConsumer
) : IntegrationTest() {

    private val payloadString: String = "{\"itemId\":1,\"registryType\":\"REFUND\"}"

    @Test
    fun testDeserializationWorks() {
        val payload: CreateTicketOnFinishReceivingPayload? = consumer.payloadTransformer.toObject(payloadString)
        assertions.assertThat(payload).isNotNull
        assertions.assertThat(payload!!.itemId).isEqualTo(1)
    }

}
