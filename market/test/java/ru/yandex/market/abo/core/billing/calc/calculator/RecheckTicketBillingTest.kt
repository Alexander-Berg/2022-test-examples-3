package ru.yandex.market.abo.core.billing.calc.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.core.billing.calc.calculator.type.BillingReportCalculatorTest
import ru.yandex.market.abo.core.common_inbox.CommonInboxService
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicket
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketService
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketStatus
import ru.yandex.market.abo.cpa.quality.recheck.ticket.RecheckTicketType

class RecheckTicketBillingTest @Autowired constructor (
    val recheckTicketService: RecheckTicketService,
    val commonInboxService: CommonInboxService,
) : BillingReportCalculatorTest(5L) {

    val typeToIndex = mapOf(
        RecheckTicketType.LITE_CPC to 4,
        RecheckTicketType.PRIOR_CHECK to 5,
        RecheckTicketType.REGION_POST_MODERATION to 5,
        RecheckTicketType.PROMO_CPC_MODERATION to 6,
        RecheckTicketType.DISCOUNT_POST_MODERATION to 7,
        RecheckTicketType.MASS_SUSPECTED to 8,
        RecheckTicketType.MASS_FOUND to 9,
        RecheckTicketType.LITE_TICKET_COMMON to 10,
        RecheckTicketType.REGION_GROUP_MODERATION to 11,
        RecheckTicketType.BLUE_PREMODERATION to 12,
        RecheckTicketType.SUPPLIER_POSTMODERATION to 12,
        RecheckTicketType.RED_PREMODERATION to 13,
        RecheckTicketType.CUTOFF_APPROVE to 14,
        RecheckTicketType.OUTLET_LICENSE_CHECK to 19,
        RecheckTicketType.GUARANTEE_LETTER_CHECK to 20,
        RecheckTicketType.CUT_PRICE to 21,
        RecheckTicketType.CREDITS to 22,
        RecheckTicketType.OUTLET_LICENSE_CHECK_FMCG to 23,
        RecheckTicketType.GUARANTEE_LETTER_CHECK_FMCG to 24,
        RecheckTicketType.DELIVERY_PARTNER to 25,
        RecheckTicketType.MASS_SUSPECTED_PINGER to 26,
        RecheckTicketType.SUPPLIER_ASSORTMENT to 27,
        RecheckTicketType.LITE_DSBS to 28,
        RecheckTicketType.DSBS_PREMODERATION to 29,
        RecheckTicketType.FF_MODERATION to 30,
        RecheckTicketType.BUSINESS_LOGO to 31,
        RecheckTicketType.WAREHOUSE_MODERATION to 32,
//        RecheckTicketType.SUPPLIER_POSTMODERATION to 33,
        RecheckTicketType.SUPPLIER_ASSORTMENT_POSTMODERATION to 34,
        RecheckTicketType.LITE_FBS to 35,
        RecheckTicketType.LITE_FBY to 36,
    )

    override fun populateData() {
        assessorIds.forEach {
            val ticket = RecheckTicket().apply {
                userId = it
                type = typeToIndex.keys.random()
                status = RecheckTicketStatus.PASS
            }
            recheckTicketService.save(ticket)

            commonInboxService.putTicketToInbox(it, ticket.id, ticket.type.inboxType)
            commonInboxService.throwTicketFromInbox(ticket.type.inboxType, ticket.id)

            addItemsToUser(it, typeToIndex[ticket.type]!!, 1)
        }
    }

    @Test
    fun `test column number`() {
        assertEquals(33, billingReport.results.size)
    }
}
