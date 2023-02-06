package ru.yandex.market.wms.replenishment.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.replenishment.dao.ReplenishmentTaskDao
import java.time.LocalDateTime
import java.time.ZoneOffset

class ReplenishmentTaskDaoTest : IntegrationTest() {

    @Autowired
    private val replenishmentTaskDao: ReplenishmentTaskDao? = null

    @Test
    @DatabaseSetup("/repository/unassign-tasks/before.xml")
    @ExpectedDatabase(
        value = "/repository/unassign-tasks/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun `unassign tasks test`() {
        val unassignBefore = LocalDateTime.of(2021, 2, 1, 0, 10, 0).toInstant(ZoneOffset.UTC)
        replenishmentTaskDao!!.unassignEditedBefore(unassignBefore, "TEST")
    }
}
