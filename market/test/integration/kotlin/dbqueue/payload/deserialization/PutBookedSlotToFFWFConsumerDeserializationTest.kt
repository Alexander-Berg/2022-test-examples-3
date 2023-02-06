package ru.yandex.market.logistics.calendaring.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.PutBookedSlotToFFWFConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.payload.PutBookedSlotToFFWFPayload

class PutBookedSlotToFFWFConsumerDeserializationTest(
    @Autowired private val consumer: PutBookedSlotToFFWFConsumer
) : AbstractContextualTest() {

    private val payload = "{\"bookingId\":1}"

    @Test
    fun testDeserializationWorks() {
        val payload: PutBookedSlotToFFWFPayload? = consumer.payloadTransformer.toObject(payload)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.bookingId).isEqualTo(1)
    }

}
