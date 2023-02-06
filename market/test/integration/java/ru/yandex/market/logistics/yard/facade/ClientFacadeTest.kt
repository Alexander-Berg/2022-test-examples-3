package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade

class ClientFacadeTest(@Autowired private val clientFacade: ClientFacade) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/signing/before.xml"])
    fun getTicketForSignSuccess() {

        val lastByPhoneNumber = clientFacade.getLastByPhoneNumber("123123123")
        assertions().assertThat(lastByPhoneNumber?.meta).isNotEmpty
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/yard_client/before.xml"])
    fun findAllNotInQueue() {
        val all = clientFacade.findAllNotInQueue()
        val ids = all.map { it.id }.toSet()

        assertions().assertThat(all.size).isEqualTo(1)
        assertions().assertThat(ids).contains(2)
    }
}
