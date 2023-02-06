package ru.yandex.market.logistics.yard.dbqueue.payload.desirialization

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.handle_yard_client_state_change.HandleYardClientStateChangeConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.handle_yard_client_state_change.HandleYardClientStateChangePayload
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientStateHistoryEntity

@Import(ConsumerConfig::class)
class HandleYardClientStateChangeDeserializationTest(
    @Autowired private val consumer: HandleYardClientStateChangeConsumer
) : AbstractSecurityMockedContextualTest() {

    private val payloadString: String =
        "{\"clientId\":1,\"previousStateHistoryEntity\":{\"id\":1,\"yardClientId\":1,\"stateId\":1,\"createdAt\":null},\"nextStateHistoryEntity\":{\"id\":2,\"yardClientId\":1,\"stateId\":2,\"createdAt\":null}}"

    @Test
    fun testDeserializationWorks() {
        val payload: HandleYardClientStateChangePayload? = consumer.payloadTransformer.toObject(payloadString)

        softly.assertThat(payload).isNotNull
        softly.assertThat(payload!!.clientId).isEqualTo(1)
        softly.assertThat(payload.previousStateHistoryEntity).isEqualTo(YardClientStateHistoryEntity(1, 1, 1))
        softly.assertThat(payload.nextStateHistoryEntity).isEqualTo(YardClientStateHistoryEntity(2, 1, 2))
    }
}
