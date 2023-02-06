package ru.yandex.market.logistics.calendaring.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.ProcessMetaInfoChangeEventPayload
import ru.yandex.market.logistics.calendaring.dbqueue.producer.ProcessMetaInfoChangeEventProducer

open class ProcessMetaInfoChangeProducerSerializationTest(
    @Autowired private val producer: ProcessMetaInfoChangeEventProducer
): AbstractContextualTest() {

    private val payloadString: String = "{\"externalId\":\"555\",\"source\":\"FFWF\",\"bookingId\":1,\"uuid\":\"123\"}"

    @Test
    fun testSerializeWorks() {
        val payload = ProcessMetaInfoChangeEventPayload("555","FFWF",1L, "123")
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}
