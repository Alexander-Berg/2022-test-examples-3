package ru.yandex.market.logistics.calendaring.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.UpdateQuotaBookingIdConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.payload.UpdateQuotaBookingIdPayload

class UpdateQuotaBookingIdConsumerDeserializationTest(
    @Autowired private val consumer: UpdateQuotaBookingIdConsumer
) : AbstractContextualTest() {

    private val payload = "{\"oldBookingId\":1,\"newBookingId\":2}"

    @Test
    fun testDeserializationWorks() {
        val payload: UpdateQuotaBookingIdPayload? = consumer.payloadTransformer.toObject(payload)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.oldBookingId).isEqualTo(1)
        softly.assertThat(payload.newBookingId).isEqualTo(2)
    }
}
