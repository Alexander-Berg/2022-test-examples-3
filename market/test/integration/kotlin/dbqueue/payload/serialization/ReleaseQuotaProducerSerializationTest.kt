package ru.yandex.market.logistics.calendaring.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.ReleaseQuotaPayload
import ru.yandex.market.logistics.calendaring.dbqueue.producer.ReleaseQuotaProducer

open class ReleaseQuotaProducerSerializationTest(
    @Autowired private val producer: ReleaseQuotaProducer
): AbstractContextualTest() {

    private val payloadString: String = "{\"bookingIds\":[1,2]}"

    @Test
    fun testSerializeWorks() {
        val payload = ReleaseQuotaPayload(setOf(1,2))
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}
