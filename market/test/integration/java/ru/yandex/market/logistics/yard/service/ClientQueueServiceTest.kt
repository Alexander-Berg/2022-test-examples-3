package ru.yandex.market.logistics.yard.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.service.ClientQueueService

class ClientQueueServiceTest(@Autowired private val clientQueueService: ClientQueueService) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_flow/4/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/yard_flow/4/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldNotMoveToNextStateIfCapacityIsBusyAndMovingBetweenDifferentCapacities() {
        clientQueueService.processClientQueue()
    }
}
