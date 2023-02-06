package ru.yandex.market.abo.core.cutoff.startrek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.cutoff.startrek.CutoffStartrekTicketTestHelper.generateNewCutoffStartrekTicket
import ru.yandex.market.abo.core.cutoff.startrek.CutoffStartrekTicketTestHelper.generateOldCutoffStartrekTicket
import ru.yandex.market.core.abo.AboCutoff
import java.time.LocalDateTime

class CutoffStartrekTicketServiceTest @Autowired constructor(
    private val cutoffStartrekTicketService: CutoffStartrekTicketService,
) : EmptyTest() {

    @Test
    fun `load immediate tickets`() {
        val cutoffStartrekTicket = generateNewCutoffStartrekTicket()
        cutoffStartrekTicketService.saveTicket(cutoffStartrekTicket)
        flushAndClear()

        val loadedTicket = cutoffStartrekTicketService
            .getUnprocessedImmediateTickets(
                delayedProblems = listOf(AboCutoff.COMMON_OTHER)
            )
            .first()

        assertThat(loadedTicket)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(cutoffStartrekTicket)
    }

    @Test
    fun `load delayed tickets`() {
        val oldTicket = generateOldCutoffStartrekTicket()
        val newTicket = generateNewCutoffStartrekTicket()

        cutoffStartrekTicketService.saveTickets(oldTicket, newTicket)

        val loadedTickets = cutoffStartrekTicketService
            .getUnprocessedDelayedTickets(
                delayedProblems = listOf(AboCutoff.CART_DIFF),
                createdBefore = LocalDateTime.now().minusDays(1),
            )

        assertThat(loadedTickets)
            .singleElement()
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(oldTicket)
    }

    @Test
    fun `set created true`() {
        val ticket = generateNewCutoffStartrekTicket()
        val savedTicket = cutoffStartrekTicketService.saveTicket(ticket)
        flushAndClear()
        cutoffStartrekTicketService.setCreatedTrue(savedTicket.id)
        flushAndClear()
        val loadedTicket = cutoffStartrekTicketService.getTicket(savedTicket.id)
        assertThat(loadedTicket?.created).isTrue
    }
}
