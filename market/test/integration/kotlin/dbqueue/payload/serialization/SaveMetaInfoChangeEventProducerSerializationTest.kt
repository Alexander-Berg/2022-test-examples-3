package ru.yandex.market.logistics.calendaring.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.payload.SaveMetaInfoChangePayload
import ru.yandex.market.logistics.calendaring.dbqueue.producer.SaveMetaInfoChangeEventProducer

class SaveMetaInfoChangeEventProducerSerializationTest(
    @Autowired private val producer: SaveMetaInfoChangeEventProducer): AbstractContextualTest() {

    private val payloadString: String = "{\"payloads\":[\"TEST1\",\"TEST2\"]}"

    @Test
    fun testSerializeWorks() {
        val payload = SaveMetaInfoChangePayload(listOf("TEST1", "TEST2"))
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}
