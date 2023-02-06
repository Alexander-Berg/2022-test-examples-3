package ru.yandex.market.abo.core.antifraud.v2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.clch.CheckerManager
import ru.yandex.market.abo.clch.ClchService
import ru.yandex.market.abo.clch.ClchSessionSource.ANTI_FRAUD
import ru.yandex.market.abo.core.CoreConfig.ANTI_FRAUD_CLONE_TICKETS_LIMIT_PER_DAY
import ru.yandex.market.abo.core.antifraud.service.AntiFraudClonesStTicketService
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudClchResult
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.PASSPORT_SAME_CONTACTS
import ru.yandex.market.abo.core.antifraud.v2.yt.daily.YtAntiFraudDailyClonesReportManager
import ru.yandex.market.abo.core.exception.ExceptionalShopReason.IGNORE_ANTI_FRAUD_CLONE_CHECK
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.util.db.ConfigurationService
import java.util.Optional

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru), zilzilok
 */
class AntiFraudClonesDailyMonitoringTest {
    private val coreConfigService: ConfigurationService = mock()
    private val ytAntiFraudDailyClonesReportManager: YtAntiFraudDailyClonesReportManager = mock()
    private val antiFraudClonesStTicketService: AntiFraudClonesStTicketService = mock()
    private val checkerManager: CheckerManager = mock()
    private val clchService: ClchService = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val antiFraudClchResult: AntiFraudClchResult = mock {
        on { shopId } doReturn SHOP_ID
        on { cloneShopId } doReturn CLONE_SHOP_ID
        on { distance } doReturn DISTANCE
        on { cloneFeatures } doReturn CLONE_FEATURES
    }

    private var antiFraudClonesDailyMonitoring = AntiFraudClonesDailyMonitoring(
        mock(),
        coreConfigService,
        ytAntiFraudDailyClonesReportManager,
        antiFraudClonesStTicketService,
        clchService,
        checkerManager,
        exceptionalShopsService
    )


    @BeforeEach
    fun init() {
        whenever(antiFraudClonesStTicketService.createdTodayTicketsCountForPotentialClone).thenReturn(0)
        whenever(ytAntiFraudDailyClonesReportManager.loadLastReportGeneration()).thenReturn(LAST_ANTI_FRAUD_YT_TABLE)
        whenever(ytAntiFraudDailyClonesReportManager.loadReport(LAST_ANTI_FRAUD_YT_TABLE))
            .thenReturn(listOf(antiFraudClchResult))
        whenever(coreConfigService.getValueAsInt(ANTI_FRAUD_CLONE_TICKETS_LIMIT_PER_DAY.id)).thenReturn(TICKETS_LIMIT)
        whenever(exceptionalShopsService.loadShops(IGNORE_ANTI_FRAUD_CLONE_CHECK)).thenReturn(SHOPS_IGNORED_FOR_CHECK)
    }

    @Test
    fun `monitor clones when not exist unchecked clones`() {
        whenever(antiFraudClonesStTicketService.noNewTicketsForPotentialClone(antiFraudClchResult))
            .thenReturn(false)

        antiFraudClonesDailyMonitoring.monitor()
        verify(antiFraudClonesStTicketService, never()).createStTicketForPotentialClone(anyLong(), any())
    }

    @Test
    fun `monitor clones when exist unchecked clones`() {
        whenever(antiFraudClonesStTicketService.noNewTicketsForPotentialClone(antiFraudClchResult))
            .thenReturn(true)
        whenever(checkerManager.createDelayedSession(eq(ANTI_FRAUD), eq(SHOP_SET), any())).thenReturn(SESSION_ID)
        whenever(clchService.getActiveShopIds(SHOP_SET)).thenReturn(SHOP_SET)

        antiFraudClonesDailyMonitoring.monitor()
        verify(checkerManager).createDelayedSession(eq(ANTI_FRAUD), eq(SHOP_SET), any())
        verify(antiFraudClonesStTicketService).createStTicketForPotentialClone(SESSION_ID, antiFraudClchResult)
    }

    @Test
    fun `monitor clones when both shops are ignored`() {
        whenever(checkerManager.getShopSet(setOf(IGNORED_SHOP_ID, IGNORED_CLONE_SHOP_ID))).thenReturn(Optional.empty())
        whenever(clchService.getActiveShopIds(SHOP_SET)).thenReturn(SHOP_SET)
        whenever(antiFraudClchResult.shopId).thenReturn(IGNORED_SHOP_ID)
        whenever(antiFraudClchResult.cloneShopId).thenReturn(IGNORED_CLONE_SHOP_ID)

        antiFraudClonesDailyMonitoring.monitor()
        verify(antiFraudClonesStTicketService, never()).createStTicketForPotentialClone(anyLong(), any())
    }

    @Test
    fun `filter check results`() {
        whenever(antiFraudClonesStTicketService.noNewTicketsForPotentialClone(any())).thenReturn(true)
        whenever(clchService.getActiveShopIds(any())).thenReturn(setOf(1L, 2L))

        val result1 = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(1L, 2L)
            cloneFeatures = arrayOf(PASSPORT_SAME_CONTACTS)
        }
        val result2 = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(3L, 4L)
            cloneFeatures = arrayOf(PASSPORT_SAME_CONTACTS)
        }
        val result3 = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(5L, 6L)
        }
        val result4 = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(7L, 8L)
            cloneFeatures = arrayOf(PASSPORT_SAME_CONTACTS)
        }
        val result5 = AntiFraudClchResult().apply {
            compositeId = AntiFraudClchResult.Key(9L, 10L)
            cloneFeatures = arrayOf(PASSPORT_SAME_CONTACTS)
        }
        val results = listOf(result1, result2, result3, result4, result5)
        val limit = 2

        val filteredCheckResults = antiFraudClonesDailyMonitoring.filterCheckResults(results, setOf(4L), limit)
        assertEquals(limit, filteredCheckResults.size)
        assertTrue(filteredCheckResults.all { listOf(result1, result4).contains(it) })
        verify(antiFraudClonesStTicketService).noNewTicketsForPotentialClone(result1)
        verify(antiFraudClonesStTicketService, never()).noNewTicketsForPotentialClone(result5)
    }

    companion object {
        private const val TICKETS_LIMIT = 10
        private const val SHOP_ID = 123L
        private const val CLONE_SHOP_ID = 124L
        private val SHOP_SET = setOf(SHOP_ID, CLONE_SHOP_ID)
        private const val SESSION_ID = 11111L
        private const val DISTANCE = 1.0
        private val CLONE_FEATURES = arrayOf(PASSPORT_SAME_CONTACTS)
        private const val IGNORED_SHOP_ID = 321L
        private const val IGNORED_CLONE_SHOP_ID = 421L
        private val SHOPS_IGNORED_FOR_CHECK = setOf(IGNORED_SHOP_ID, IGNORED_CLONE_SHOP_ID)
        private const val LAST_ANTI_FRAUD_YT_TABLE = "2020-07-07"
    }
}
