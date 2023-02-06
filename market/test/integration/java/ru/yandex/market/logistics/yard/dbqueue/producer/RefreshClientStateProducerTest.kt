package ru.yandex.market.logistics.yard.dbqueue.producer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStatePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateProducer
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DbUnitConfiguration(databaseConnection = ["dbqueueDatabaseConnection"])
class RefreshClientStateProducerTest(
    @Autowired val refreshClientStateProducer: RefreshClientStateProducer
) : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/producer/refresh-client-state/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/producer/refresh-client-state/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection")
    fun enqueue() {
        val payload = RefreshClientStatePayload(3)
        refreshClientStateProducer.enqueue(EnqueueParams.create(payload))
    }
}
