package ru.yandex.market.logistics.cte.dbqueue.producer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.CreateTicketOnFinishReceivingPayload
import ru.yandex.market.logistics.cte.client.enums.RegistryType
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DbUnitConfiguration(databaseConnection = ["dbqueueDatabaseConnection"])
class CreateTicketOnFinishReceivingProducerTest(
    @Autowired private val createTicketOnFinishReceivingProducer: CreateTicketOnFinishReceivingProducer,
    @Autowired private val jdbcTemplate: JdbcTemplate
): IntegrationTest() {

    @Test
    @DatabaseSetup(
        value = ["classpath:dbqueue/producer/create-ticket-on-finish-receiving/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/producer/create-ticket-on-finish-receiving/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection"
    )
    fun enqueue() {
//        jdbcTemplate.update("insert into dbqueue.queue_log (queue_name, event, payload, entity_id, task_id) " +
//            "values ('111', '111', '{}', 1, 1)")
        val payload = CreateTicketOnFinishReceivingPayload(3, RegistryType.REFUND)
        createTicketOnFinishReceivingProducer.enqueue(EnqueueParams.create(payload))
    }
}
