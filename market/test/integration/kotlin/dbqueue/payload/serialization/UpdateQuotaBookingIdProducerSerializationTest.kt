package ru.yandex.market.logistics.calendaring.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.UpdateQuotaBookingIdPayload
import ru.yandex.market.logistics.calendaring.dbqueue.producer.UpdateQuotaBookingIdProducer

class UpdateQuotaBookingIdProducerSerializationTest(
    @Autowired
    private val producer: UpdateQuotaBookingIdProducer
) : AbstractContextualTest() {

    private val payloadString = "{\"oldBookingId\":1,\"newBookingId\":2}"

    @Test
    fun testSerializeWorks() {
        val payload = UpdateQuotaBookingIdPayload(1, 2)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }
}
