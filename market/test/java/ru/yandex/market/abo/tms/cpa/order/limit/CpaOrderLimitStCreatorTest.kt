package ru.yandex.market.abo.tms.cpa.order.limit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.exception.ExceptionalShopReason
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.partner.info.PartnerInfoService
import ru.yandex.market.abo.core.partner.info.model.PartnerExtendedInfo
import ru.yandex.market.abo.core.partner.info.model.PartnerType
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual
import ru.yandex.market.abo.core.rating.partner.PartnerRatingService
import ru.yandex.market.abo.core.startrek.StartrekTicketManager
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService

class CpaOrderLimitStCreatorTest @Autowired constructor(
    val startrekTicketManager: StartrekTicketManager,
) : EmptyTest() {

    val partnerInfoService: PartnerInfoService = mock()
    val partnerRatingService: PartnerRatingService = mock()
    val cpaOrderLimitService: CpaOrderLimitService = mock()
    val exceptionalShopsService: ExceptionalShopsService = mock()

    val cpaOrderLimitStCreator = CpaOrderLimitStCreator(
        exceptionalShopsService,
        partnerRatingService,
        cpaOrderLimitService,
        startrekTicketManager,
        partnerInfoService
    )
    @Test
    fun `bad rating exclude shops`() {
        val partnerId = 1L
        whenever(exceptionalShopsService.loadShops(ExceptionalShopReason.DONT_CREATE_RATING_ORDER_LIMIT)).thenReturn(setOf(partnerId))

        val partnerRatingActual: PartnerRatingActual = mock()
        whenever(partnerRatingActual.total).thenReturn(0.949)
        whenever(partnerRatingActual.partnerId).thenReturn(partnerId)
        whenever(partnerRatingService.getActualRating(partnerId)).thenReturn(partnerRatingActual)

        whenever(cpaOrderLimitService.findManualLimits()).thenReturn(listOf())

        val partnerInfo: PartnerExtendedInfo = mock()
        whenever(partnerInfo.express).thenReturn(true)
        whenever(partnerInfoService.getPartnerType(partnerId)).thenReturn(PartnerType.SUPPLIER)
        whenever(partnerInfoService.loadPartnerExtendedInfo(anyLong())).thenReturn(partnerInfo)

        cpaOrderLimitStCreator.createTickets()
        assertEquals(1, startrekTicketManager.getTickets(StartrekTicketReason.LIMIT_EXCLUDE_EXPRESS).size)

        cpaOrderLimitStCreator.createTickets()
        assertEquals(1, startrekTicketManager.getTickets(StartrekTicketReason.LIMIT_EXCLUDE_EXPRESS).size)
    }

}
