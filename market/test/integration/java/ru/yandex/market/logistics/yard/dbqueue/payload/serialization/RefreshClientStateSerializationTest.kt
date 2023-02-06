import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStatePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateProducer

class RefreshClientStateSerializationTest(@Autowired private val producer: RefreshClientStateProducer) : AbstractSecurityMockedContextualTest() {
    private val payloadString = "{\"clientId\":1}"

    @Test
    fun testSerializeWorks() {
        val payload = RefreshClientStatePayload(1)
        val payloadString: String? = producer.payloadTransformer.fromObject(payload)
        softly.assertThat(payloadString).isEqualTo(this.payloadString)
    }

}


