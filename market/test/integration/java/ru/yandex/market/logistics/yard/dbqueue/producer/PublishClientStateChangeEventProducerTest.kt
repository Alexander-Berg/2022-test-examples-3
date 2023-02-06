package ru.yandex.market.logistics.yard.dbqueue.producer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventPayload
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventProducer
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DbUnitConfiguration(databaseConnection = ["dbqueueDatabaseConnection"])
class PublishClientStateChangeEventProducerTest(
    @Autowired val publishClientStateChangeEventProducer: PublishClientStateChangeEventProducer
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/producer/publish-client-state-change-event/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/dbqueue/producer/publish-client-state-change-event/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun enqueue() {
        val payload = PublishClientStateChangeEventPayload(3, 1, 2)
        publishClientStateChangeEventProducer.enqueue(EnqueueParams.create(payload))
    }

}
