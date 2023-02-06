package ru.yandex.market.logistics.calendaring.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.PutBookedSlotToFFWFPayload
import ru.yandex.market.logistics.calendaring.dbqueue.producer.PutBookedSlotToFFWFProducer

class PutBookedSlotToFFWFProducerSerializationTest(
    @Autowired private val producer: PutBookedSlotToFFWFProducer
) : AbstractContextualTest() {
    private val payloadString = "{\"bookingId\":1}"

    @Test
    fun testSerializeWorks() {
        val payload = PutBookedSlotToFFWFPayload(1)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}

