package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStatePayload

@Import(ConsumerConfig::class)
class RefreshClientStateDeserializationTest(
    @Autowired private val consumer: RefreshClientStateConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String = "{\"clientId\":1}"

    @Test
    fun testDeserializationWorks() {
        val payload: RefreshClientStatePayload? = consumer.payloadTransformer.toObject(payloadString)
        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.clientId).isEqualTo(1)
    }

}

