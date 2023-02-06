package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityUnitStatusLogEntity
import ru.yandex.market.logistics.yard_v2.facade.CapacityUnitStatusLogFacade
import java.time.LocalDateTime

class CapacityUnitStatusLogFacadeTest(@Autowired val capacityUnitStatusLogFacade: CapacityUnitStatusLogFacade) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/capacity-unit-status-log-facade/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/capacity-unit-status-log-facade/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveTest() {
        val result = capacityUnitStatusLogFacade.logStatusChanging(1L, "TEST_STATUS")
        assertions().assertThat(result.capacityUnitId).isEqualTo(1L)
        assertions().assertThat(result.status).isEqualTo("TEST_STATUS")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/capacity-unit-status-log-facade/before.xml"])
    fun getLastStatusUpdateTest() {
        val result = capacityUnitStatusLogFacade.getLastStatusChanging(1L)

        assertions().assertThat(result).isEqualTo(
            CapacityUnitStatusLogEntity(2, 1, "TEST_STATUS_NEW", LocalDateTime.of(2021, 5, 1, 11, 0, 0))
        )
    }
}
