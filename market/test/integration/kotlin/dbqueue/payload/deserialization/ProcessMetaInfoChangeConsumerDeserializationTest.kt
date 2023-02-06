package ru.yandex.market.logistics.calendaring.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.ProcessMetaInfoChangeEventConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.payload.ProcessMetaInfoChangeEventPayload

class ProcessMetaInfoChangeConsumerDeserializationTest(
    @Autowired private val consumer: ProcessMetaInfoChangeEventConsumer
): AbstractContextualTest() {

    private val payloadString: String = "{\"externalId\":\"453\", \"source\": \"FFWF\",\"uuid\":\"123\"}"

    @Test
    fun testDeserializationWorks() {
        val payload: ProcessMetaInfoChangeEventPayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.externalId).isEqualTo("453")
        softly.assertThat(payload.source).isEqualTo("FFWF")
        softly.assertThat(payload.uuid).isEqualTo("123")
    }
}
