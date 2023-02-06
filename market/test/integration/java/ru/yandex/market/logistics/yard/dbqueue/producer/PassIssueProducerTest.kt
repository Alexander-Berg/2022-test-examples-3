package ru.yandex.market.logistics.yard.dbqueue.producer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssuePayload
import ru.yandex.market.logistics.yard_v2.dbqueue.issue_pass.PassIssueProducer
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@DbUnitConfiguration(databaseConnection = ["dbqueueDatabaseConnection"])
class PassIssueProducerTest(@Autowired val passIssueProducer: PassIssueProducer)
    : AbstractSecurityMockedContextualTest() {
    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/producer/pass-issue/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/dbqueue/producer/pass-issue/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        connection = "dbqueueDatabaseConnection")
    fun enqueue() {
        val payload = PassIssuePayload(100, null, 1)
        passIssueProducer.enqueue(EnqueueParams.create(payload))
    }
}
