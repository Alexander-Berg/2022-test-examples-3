package ru.yandex.market.abo.core.startrek.billing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class StartrekBillingTicketRepoTest @Autowired constructor(
    private val startrekBillingTicketRepo: StartrekBillingTicketRepo
) : EmptyTest() {

    @Test
    fun deleteAllByBillingResultId() {
        val startrekBillingTickets = listOf(
            StartrekBillingTicket().apply {
                billingResultId = 1
            },
            StartrekBillingTicket().apply {
                billingResultId = 2
            },
        )
        startrekBillingTicketRepo.saveAll(startrekBillingTickets)
        flushAndClear()
        startrekBillingTicketRepo.deleteAllByBillingResultId(2)
        flushAndClear()
        val dbStartrekBillingTickets = startrekBillingTicketRepo.findAll()
        assertEquals(1, dbStartrekBillingTickets.size)
        assertEquals(startrekBillingTickets[0].id, dbStartrekBillingTickets[0].id)
    }
}
