package ru.yandex.market.logistics.calendaring.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.ReleaseQuotaConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.payload.ReleaseQuotaPayload

class ReleaseQuotaConsumerDeserializationTest(
    @Autowired private val consumer: ReleaseQuotaConsumer
): AbstractContextualTest() {

    private val payloadString: String = "{\"bookingIds\":[1,2]}"

    @Test
    fun testDeserializationWorks() {
        val payload: ReleaseQuotaPayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.bookingIds).isEqualTo(setOf(1L,2L))
    }

}
