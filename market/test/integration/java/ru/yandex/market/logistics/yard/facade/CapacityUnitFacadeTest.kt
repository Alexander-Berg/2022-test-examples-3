package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.CapacityUnitFacadeInterface

class CapacityUnitFacadeTest(@Autowired private val capacityUnitFacade: CapacityUnitFacadeInterface) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/freeze/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/capacity_unit/freeze/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun freezeTest() {
        val capacityUnit = capacityUnitFacade.getByIdOrThrow(1L)
        val clientId = 1L
        capacityUnitFacade.freezeCapacityUnit(capacityUnit, clientId)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/unfreeze/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/capacity_unit/unfreeze/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun unfreezeTest() {
        val frozenCapacityUnit = 1L
        val clientId = 1L
        capacityUnitFacade.unfreezeCapacityUnit(frozenCapacityUnit, clientId)
    }
}
