package ru.yandex.market.wms.replenishment.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest

class TurnoverReplenishmentServiceTest: IntegrationTest() {

    @Autowired
    private lateinit var replenishmentService: ReplenishmentService

    @Test
    @DatabaseSetup("/service/create-tasks/before.xml")
    @ExpectedDatabase(
        value = "/service/create-tasks/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create replenishment tasks`() {
        replenishmentService.createTurnoverReplenishmentTasks()
    }

    @Test
    @DatabaseSetup("/service/create-tasks-boxes/before.xml")
    @ExpectedDatabase(
        value = "/service/create-tasks-boxes/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `create replenishment tasks for boxes`() {
        replenishmentService.createTurnoverReplenishmentTasks()
    }

}
