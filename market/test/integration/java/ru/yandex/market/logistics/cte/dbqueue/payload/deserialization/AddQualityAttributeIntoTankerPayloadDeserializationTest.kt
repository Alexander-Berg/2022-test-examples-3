package ru.yandex.market.logistics.cte.dbqueue.payload.deserialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.config.ConsumerConfig
import ru.yandex.market.logistics.cte.dbqueue.consumer.AddQualityAttributeIntoTankerConsumer
import ru.yandex.market.logistics.cte.dbqueue.payload.AddQualityAttributeIntoTankerPayload

@Import(ConsumerConfig::class)
class AddQualityAttributeIntoTankerPayloadDeserializationTest(
    @Autowired private val consumer: AddQualityAttributeIntoTankerConsumer
) : IntegrationTest() {
    private val payloadString = "{\"name\":\"PACKAGE_JAMS\",\"description\":\"Замятия (Свыше 5% площади стороны)\"}"

    @Test
    fun testDeserializationWorks() {
        val payload: AddQualityAttributeIntoTankerPayload? = consumer.payloadTransformer.toObject(payloadString)
        assertions.assertThat(payload).isNotNull
        assertions.assertThat(payload!!.name).isEqualTo("PACKAGE_JAMS")
        assertions.assertThat(payload!!.description).isEqualTo("Замятия (Свыше 5% площади стороны)")
    }

}