package ru.yandex.market.abo.cpa.pinger.mass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.partner.info.PartnerInfoService
import ru.yandex.market.abo.cpa.MbiApiService
import ru.yandex.market.abo.cpa.pinger.PingerCutoffService
import java.time.LocalDateTime


internal class PingerMassErrorManagerTest {

    private val partnerInfoService: PartnerInfoService = mock()
    private val pingerCutoffService: PingerCutoffService = mock()
    private val cutoffManager: CutoffManager = mock()
    private val pingerMassErrorService: PingerMassErrorService = mock()
    private val mbiApiService: MbiApiService = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val transactionTemplate: TransactionTemplate = mock()

    private val pingerMassErrorManager = PingerMassErrorManager(
        partnerInfoService,
        pingerCutoffService,
        cutoffManager,
        pingerMassErrorService,
        mbiApiService,
        exceptionalShopsService,
        transactionTemplate
    )

    @Test
    fun buildTimeBordersAboInfo() {
        val time = LocalDateTime.of(2022, 3, 17, 23, 46)
        val xml = """
            <abo-info>
             <time-borders>
              <from-date>2022-03-16</from-date>
              <from-hour>23</from-hour>
              <from-minute>45</from-minute>
              <to-date>2022-03-18</to-date>
              <to-hour>0</to-hour>
              <to-minute>0</to-minute>
             </time-borders>
            </abo-info>

        """.trimIndent()
        assertEquals(xml, pingerMassErrorManager.buildTimeBordersAboInfo(time))
    }
}
