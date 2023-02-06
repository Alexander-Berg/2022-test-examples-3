package ru.yandex.market.logistics.yard.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssuePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssueProducer

@Import(ConsumerConfig::class)
class PassIssueSerializationTest(@Autowired
                                 private val producer: PassIssueProducer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"serviceId\":100,\"serviceUUID\":null,\"passId\":1}"


    @Test
    fun testDeserializationWorks() {
        val passIssuePayload = PassIssuePayload(100, null, 1)

        val payload = producer.payloadTransformer.fromObject(passIssuePayload)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload).isEqualTo(payloadString)
    }

}
