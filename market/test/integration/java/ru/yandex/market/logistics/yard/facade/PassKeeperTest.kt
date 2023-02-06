package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.PassKeeper

class PassKeeperTest(
    @Autowired private val passKeeper: PassKeeper
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-keeper/before.xml"])
    fun getTicket() {
        val pass = passKeeper.getById(10)

        assertions().assertThat(pass.id).isEqualTo(10)
        assertions().assertThat(pass.yardClientId).isEqualTo(10)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-keeper/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/pass-keeper/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateTicket() {
        val pass = passKeeper.getById(10)

        passKeeper.update(pass.copy(externalId = "external"))
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/pass-keeper/before.xml"])
    fun searchByRequestId() {
        val passes = passKeeper.searchByRequestId(10)
        assertions().assertThat(passes).hasSize(1)

        val searchedPass = passes[0]
        assertions().assertThat(searchedPass.id).isEqualTo(10)
        assertions().assertThat(searchedPass.yardClientId).isEqualTo(10)
    }
}
