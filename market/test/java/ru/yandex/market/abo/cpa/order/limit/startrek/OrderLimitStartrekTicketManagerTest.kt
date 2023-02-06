package ru.yandex.market.abo.cpa.order.limit.startrek

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService
import ru.yandex.market.abo.cpa.order.limit.model.CpaOrderLimit
import java.util.Optional

class OrderLimitStartrekTicketManagerTest {

    private val orderLimitStartrekTicketService: OrderLimitStartrekTicketService = mock()
    private val orderLimitStartrekTicketCreator: OrderLimitStartrekTicketCreator = mock()
    private val cpaOrderLimitService: CpaOrderLimitService = mock()

    private val orderLimitStartrekTicketManager = OrderLimitStartrekTicketManager(
        orderLimitStartrekTicketService = orderLimitStartrekTicketService,
        orderLimitStartrekTicketCreator = orderLimitStartrekTicketCreator,
        cpaOrderLimitService = cpaOrderLimitService,
    )

    @Test
    fun `process new ticket`() {
        whenever(orderLimitStartrekTicketService.getNewOrderLimits()).thenReturn(listOf(
            OrderLimitStartrekTicket(
                id = 0L,
                orderLimitId = 1L,
                shopId = 2L,
            )
        ))
        whenever(cpaOrderLimitService.findOne(any())).thenReturn(Optional.of(CpaOrderLimit()))

        orderLimitStartrekTicketManager.createTickets()

        verify(orderLimitStartrekTicketCreator).createTicket(any())
        verify(orderLimitStartrekTicketService).setCreatedTrue(0L)
    }
}
