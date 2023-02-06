package ru.yandex.market.abo.cpa.pinger.mass

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.cutoff.history.AboCutoffHistoryService
import ru.yandex.market.abo.core.exception.ExceptionalShopReason
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.cpa.pinger.PingerCutoffService
import ru.yandex.market.abo.cpa.pinger.mass.model.PingerMassError
import ru.yandex.market.core.abo.AboCutoff
import java.time.LocalDateTime


internal class PingerMassErrorManagerFunctionalTest @Autowired constructor(
    private val pingerMassErrorManager: PingerMassErrorManager,
    private val pingerCutoffService: PingerCutoffService,
    private val pingerMassErrorService: PingerMassErrorService,
    private val exceptionalShopsService: ExceptionalShopsService,
    private val aboCutoffHistoryService: AboCutoffHistoryService
) : EmptyTest() {

    private val PARTNER_ID = 1L

    @Test
    fun `not enough pinger cutoffs opened`() {
        openPingerCutoffs(2)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isEmpty())
    }

    @Test
    fun `shop is exceptional`() {
        openPingerCutoffs(3)
        exceptionalShopsService.addException(
            PARTNER_ID, ExceptionalShopReason.DONT_OPEN_PINGER_MASS_ERRORS_CUTOFF, 1, ""
        )
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isEmpty())
    }

    @Test
    fun `no pinger mass error was opened yet`() {
        openPingerCutoffs(3)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isNotEmpty())
    }

    @Test
    fun `pinger mass error was opened today`() {
        openPingerCutoffs(3)
        createMassErrors(0)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isNotEmpty())
        assertTrue(pingerMassErrorService.findInactiveMassErrors().isEmpty())
        assertTrue(getOpenedCutoffs().isEmpty())
    }

    @Test
    fun `pinger mass error was opened yesterday`() {
        openPingerCutoffs(3)
        createMassErrors(1)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isNotEmpty())
        assertTrue(pingerMassErrorService.findInactiveMassErrors().isEmpty())
        assertTrue(getOpenedCutoffs().isEmpty())
    }

    @Test
    fun `pinger mass error was opened 2 days ago`() {
        openPingerCutoffs(3)
        createMassErrors(2)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isEmpty())
        assertTrue(pingerMassErrorService.findInactiveMassErrors().isNotEmpty())
        assertTrue(getOpenedCutoffs().isNotEmpty())
    }

    @Test
    fun `pinger mass error was opened 8 days ago`() {
        openPingerCutoffs(3)
        createMassErrors(8)
        executeLogic()
        assertTrue(pingerMassErrorService.findActiveMassErrors().isNotEmpty())
        assertTrue(pingerMassErrorService.findInactiveMassErrors().isNotEmpty())
        assertTrue(getOpenedCutoffs().isEmpty())
    }

    private fun createMassErrors(massErrorDaysAgo: Long) {
        val pingerMassError = PingerMassError().apply {
            this.partnerId = PARTNER_ID
            this.creationTime = LocalDateTime.now().minusDays(massErrorDaysAgo)
        }
        pingerMassErrorService.saveMassError(pingerMassError)

    }

    private fun openPingerCutoffs(count: Int) {
        repeat(count) { pingerCutoffService.openCutoff(PARTNER_ID) }
    }

    private fun executeLogic() {
        pingerMassErrorManager.deleteExpiredMassErrors()
        pingerMassErrorManager.processPingerMassErrors()
    }

    private fun getOpenedCutoffs() =
        aboCutoffHistoryService.load(
            PARTNER_ID, setOf(AboCutoff.PINGER_MASS_ERRORS), Pageable.ofSize(1)
        ).content
}
