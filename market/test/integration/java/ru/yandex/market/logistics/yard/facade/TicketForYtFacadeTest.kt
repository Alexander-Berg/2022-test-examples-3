package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.entity.TicketEntity
import ru.yandex.market.logistics.yard_v2.facade.TicketForYtFacade

class TicketForYtFacadeTest(@Autowired private val ticketForYtFacade: TicketForYtFacade) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-for-yt-facade/1/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/facade/ticket-for-yt-facade/1/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun persist() {
        val entity = TicketEntity(1, "2", 3, 4, null, null, "6", "7", 8, 1, 1, null)
        ticketForYtFacade.persist(entity)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-for-yt-facade/2/before.xml"])
    fun findByIdGreater() {
        softly.assertThat(ticketForYtFacade.findByIdGreaterThan(1, 2).size).isEqualTo(1)
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-for-yt-facade/3/before.xml"])
    @ExpectedDatabase(value = "classpath:fixtures/facade/ticket-for-yt-facade/3/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun deleteByIdLess() {
        ticketForYtFacade.deleteByIdLessThanEqual(2)
    }

}
