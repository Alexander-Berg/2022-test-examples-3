package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.LoginFacade

class LoginFacadeTest(@Autowired private val loginFacade: LoginFacade) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/login/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/capacity_unit/login/after-logout.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun logoutCapacityUnitsFromLastShiftChange() {
        loginFacade.logoutCapacityUnitsFromLastShiftChange()
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/login/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/capacity_unit/login/after-final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun tryToLogoutInFinalState() {
        loginFacade.tryToLogout(2L)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/capacity_unit/login/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/repository/capacity_unit/login/after-not-final-state.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun tryToLogoutInNotFinalState() {
        loginFacade.tryToLogout(1L)
    }
}
