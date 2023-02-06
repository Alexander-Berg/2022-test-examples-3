package ru.yandex.market.logistics.calendaring.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.dbqueue.consumer.SaveMetaInfoChangeEventConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.payload.SaveMetaInfoChangePayload

class SaveMetaInfoChangeEventConsumerDeserializationTest(
    @Autowired private val consumer: SaveMetaInfoChangeEventConsumer): AbstractContextualTest() {

    private val payload = "{\"payloads\":[\"TEST1\",\"TEST2\"]}"

    @Test
    fun testDeserializationWorks() {
        val payload: SaveMetaInfoChangePayload? = consumer.payloadTransformer.toObject(payload)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.payloads).isEqualTo(listOf("TEST1", "TEST2"))
    }
}
