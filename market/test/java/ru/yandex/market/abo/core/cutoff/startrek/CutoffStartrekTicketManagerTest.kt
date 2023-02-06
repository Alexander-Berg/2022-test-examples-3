package ru.yandex.market.abo.core.cutoff.startrek

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.cutoff.CutoffDbService
import ru.yandex.market.abo.core.cutoff.startrek.CutoffStartrekTicketTestHelper.generateNewCutoffStartrekTicket
import ru.yandex.market.abo.core.cutoff.startrek.CutoffStartrekTicketTestHelper.generateOldCutoffStartrekTicket

class CutoffStartrekTicketManagerTest {

    private val cutoffStartrekTicketService: CutoffStartrekTicketService = mock()

    private val cutoffStartrekTicketCreator: CutoffStartrekTicketCreator = mock()

    private val cutoffDbService: CutoffDbService = mock()

    private val cutoffStartrekTicketManager = CutoffStartrekTicketManager(
        cutoffStartrekTicketService = cutoffStartrekTicketService,
        cutoffStartrekTicketCreator = cutoffStartrekTicketCreator,
        cutoffDbService = cutoffDbService,
    )

    @Test
    fun `process immediate ticket`() {
        val ticket = generateNewCutoffStartrekTicket()
        whenever(cutoffStartrekTicketService.getUnprocessedImmediateTickets(any()))
            .thenReturn(listOf(ticket))

        cutoffStartrekTicketManager.process()

        verify(cutoffStartrekTicketCreator).createStartrekTicket(ticket)
        verify(cutoffStartrekTicketService).setCreatedTrue(ticket.id)
    }

    @Test
    fun `process actual delayed ticket`() {
        val ticket = generateOldCutoffStartrekTicket()
        whenever(cutoffStartrekTicketService.getUnprocessedDelayedTickets(any(), any()))
            .thenReturn(listOf(ticket))

        cutoffStartrekTicketManager.process()

        verify(cutoffStartrekTicketCreator).createStartrekTicket(ticket)
        verify(cutoffStartrekTicketService).setCreatedTrue(ticket.id)
    }

    @Test
    fun `process closed delayed ticket`() {
        val ticket = generateOldCutoffStartrekTicket()
        whenever(cutoffStartrekTicketService.getUnprocessedDelayedTickets(any(), any()))
            .thenReturn(listOf(ticket))
        whenever(cutoffDbService.isCutoffOpened(any(), any()))
            .thenReturn(true)

        cutoffStartrekTicketManager.process()

        verify(cutoffStartrekTicketCreator, never()).createStartrekTicket(any())
        verify(cutoffStartrekTicketService).setCreatedTrue(ticket.id)
    }
}
