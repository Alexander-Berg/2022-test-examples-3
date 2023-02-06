package ru.yandex.market.logistics.cte.dbqueue.payload.serialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AddQualityAttributeIntoTankerPayload
import ru.yandex.market.logistics.cte.dbqueue.producer.AddQualityAttributeIntoTankerProducer

class AddQualityAttributeIntoTankerPayloadSerializationTest(
        @Autowired private val producer: AddQualityAttributeIntoTankerProducer
) : IntegrationTest() {
    private val payloadString = "{\"name\":\"PACKAGE_JAMS\",\"description\":\"Замятия (Свыше 5% площади стороны)\"}"

    @Test
    fun testSerializeWorks() {
        val payload = AddQualityAttributeIntoTankerPayload("PACKAGE_JAMS",
                "Замятия (Свыше 5% площади стороны)")
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        assertions.assertThat(payloadString).isEqualTo(this.payloadString)
    }
}
