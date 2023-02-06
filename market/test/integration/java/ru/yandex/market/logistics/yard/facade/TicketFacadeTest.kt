package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.domain.exception.NotFoundException
import ru.yandex.market.logistics.yard_v2.facade.TicketFacadeInterface
import ru.yandex.market.logistics.yard_v2.repository.mapper.TicketMapper

class TicketFacadeTest(
    @Autowired private val ticketFacade: TicketFacadeInterface,
    @Autowired private val ticketMapper: TicketMapper,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/queue/before.xml"])
    fun getTicketsQueue() {
        val queueBefore = ticketFacade.getWaitingTicketBySiteIds(listOf(100))
        assertions().assertThat(queueBefore.windows).hasSize(2)

        val windowDto = queueBefore.windows[1]
        assertions().assertThat(windowDto.windowId).isEqualTo("Windows 1")
        assertions().assertThat(windowDto.ticketCode).isEqualTo("Р001")
        assertions().assertThat(windowDto.audio).isEqualTo("test_audio_in_base64")

        val windowDto2 = queueBefore.windows[0]
        assertions().assertThat(windowDto2.windowId).isEqualTo("Windows 2")
        assertions().assertThat(windowDto2.ticketCode).isEqualTo("Р002")
        assertions().assertThat(windowDto2.audio).isEqualTo("test_audio_in_base64_2")
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/ticket-facade/update/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/ticket-facade/update/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUpdateOperatorWindowId() {
        val ticket = ticketMapper.getById(1L) ?: throw NotFoundException("no ticket")
        ticketFacade.updateOperatorWindowId(ticket, 6L, "test6")
        ticketFacade.updateInitialOperatorWindowId(ticket.id!!, 5L, "test5")
    }

}
