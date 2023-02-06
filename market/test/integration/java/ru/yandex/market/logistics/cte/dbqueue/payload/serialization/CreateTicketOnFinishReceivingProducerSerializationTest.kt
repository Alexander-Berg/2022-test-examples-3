package ru.yandex.market.logistics.cte.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.CreateTicketOnFinishReceivingPayload
import ru.yandex.market.logistics.cte.dbqueue.producer.CreateTicketOnFinishReceivingProducer
import ru.yandex.market.logistics.cte.client.enums.RegistryType

class CreateTicketOnFinishReceivingProducerSerializationTest(
    @Autowired private val producer: CreateTicketOnFinishReceivingProducer
) : IntegrationTest() {
    private val payloadString = "{\"itemId\":1,\"registryType\":\"REFUND\"}"

    @Test
    fun testSerializeWorks() {
        val payload = CreateTicketOnFinishReceivingPayload(1, RegistryType.REFUND)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        assertions.assertThat(payloadString).isEqualTo(this.payloadString)
    }
}

