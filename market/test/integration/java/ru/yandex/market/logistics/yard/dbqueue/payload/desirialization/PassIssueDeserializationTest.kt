package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssueConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssuePayload

@Import(ConsumerConfig::class)
class PassIssueDeserializationTest(@Autowired
                                   private val consumer: PassIssueConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"serviceId\":100,\"serviceUUID\":null,\"passId\":1}\n"


    @Test
    fun testDeserializationWorks() {
        val payload: PassIssuePayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.serviceId).isEqualTo(100)
        softly.assertThat(payload.passId).isEqualTo(1)
        softly.assertThat(payload.serviceUUID).isNull()
    }

}
