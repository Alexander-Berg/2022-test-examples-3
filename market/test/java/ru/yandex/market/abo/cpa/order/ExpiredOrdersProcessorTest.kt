package ru.yandex.market.abo.cpa.order

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType
import ru.yandex.market.abo.core.checkorder.CheckOrderPartnerManager
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.cpa.pinger.PingerCutoffService
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus

class ExpiredOrdersProcessorTest @Autowired constructor(
    val checkOrderPartnerManager: CheckOrderPartnerManager,
    transactionTemplate: TransactionTemplate,

) : EmptyTest() {

    val cutoffManager: CutoffManager = mock()
    val pingerCutoffService: PingerCutoffService = mock()

    val expiredOrdersProcessor = ExpiredOrdersProcessor(
        pingerCutoffService,
        checkOrderPartnerManager,
        cutoffManager,
        transactionTemplate
    )

    @Test
    fun `duplicate switch off`() {
        val partnerId = 1L
        val orderId = 2L
        whenever(pingerCutoffService.openCutoffForAcceptProblems(partnerId, orderId)).thenReturn(CutoffActionStatus.OK)

        assertTrue(checkOrderPartnerManager.findCheckOrders(partnerId).isEmpty())

        expiredOrdersProcessor.switchOffPartner(partnerId, orderId, Color.WHITE, true);
        assertTrue(checkOrderPartnerManager.hasInProgressScenario(partnerId, CheckOrderScenarioType.EXPIRED_API_ORDER_DSBS))

        expiredOrdersProcessor.switchOffPartner(partnerId, orderId, Color.WHITE, true);
        assertTrue(checkOrderPartnerManager.hasInProgressScenario(partnerId, CheckOrderScenarioType.EXPIRED_API_ORDER_DSBS))
    }

    @Test
    fun `cutoff White PI partner`() {
        expiredOrdersProcessor.switchOffPartner(1L, 2L, Color.WHITE, false)
        verify(cutoffManager).openAboCutoff(any(), any(), anyOrNull())
    }
}
