package ru.yandex.market.logistics.cte.dbqueue.producer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AddQualityAttributeIntoTankerPayload
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DbUnitConfiguration(databaseConnection = ["dbqueueDatabaseConnection"])
class AddQualityAttributeIntoTankerProducerTest(
    @Autowired private val addQualityAttributeIntoTankerProducer : AddQualityAttributeIntoTankerProducer,
    @Autowired private val jdbcTemplate: JdbcTemplate
) : IntegrationTest() {

    @Test
    @DatabaseSetup(
            value = ["classpath:dbqueue/producer/add-quality-attribute-into-tanker/before.xml"]
    )
    @ExpectedDatabase(
            value = "classpath:dbqueue/producer/add-quality-attribute-into-tanker/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            connection = "dbqueueDatabaseConnection"
    )
    fun enqueue() {
        val payload = AddQualityAttributeIntoTankerPayload("PACKAGE_JAMS",
                "Замятия (Свыше 5% площади стороны)")
        addQualityAttributeIntoTankerProducer.enqueue(EnqueueParams.create(payload))
    }
}
