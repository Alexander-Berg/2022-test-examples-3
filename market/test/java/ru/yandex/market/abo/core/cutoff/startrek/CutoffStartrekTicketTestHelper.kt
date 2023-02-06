package ru.yandex.market.abo.core.cutoff.startrek

import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason
import ru.yandex.market.core.abo.AboCutoff
import java.time.LocalDateTime

object CutoffStartrekTicketTestHelper {

    fun generateNewCutoffStartrekTicket() = CutoffStartrekTicket(
        shopId = 1L,
        startrekTicketReason = StartrekTicketReason.SERVICED_SHOP_CUTOFF,
        cutoff = AboCutoff.CART_DIFF,
        creationTime = LocalDateTime.now()
    )

    fun generateOldCutoffStartrekTicket() = CutoffStartrekTicket(
        shopId = 1L,
        startrekTicketReason = StartrekTicketReason.SERVICED_SHOP_CUTOFF,
        cutoff = AboCutoff.CART_DIFF,
        creationTime = LocalDateTime.now().minusDays(2)
    )

}
