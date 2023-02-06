package ru.yandex.market.abo.core.antifraud.v2

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.market.abo.core.CoreCounter
import ru.yandex.market.abo.core.antifraud.v2.AntiFraudClchManager.Companion.NEW_CLONES_TABLES_PATH
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudClchResult
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudCloneFeature.UNKNOWN
import ru.yandex.market.abo.core.antifraud.v2.model.AntiFraudGlueContacts
import ru.yandex.market.abo.core.antifraud.v2.service.AntiFraudClchResultService
import ru.yandex.market.abo.core.yt.YtService
import ru.yandex.market.util.db.ConfigurationService

/**
 * @author zilzilok
 */
class AntiFraudClchManagerTest {

    private val ytService: YtService = mock()
    private val antiFraudClchResultService: AntiFraudClchResultService = mock()
    private val coreCounterService: ConfigurationService = mock()
    private val antiFraudClchManager = AntiFraudClchManager(
        antiFraudClchResultService, ytService, coreCounterService
    )

    @BeforeEach
    fun init() {
        whenever(ytService.list(YPath.simple(NEW_CLONES_TABLES_PATH)))
            .thenReturn(listOf(LAST_ANTI_FRAUD_NEW_CLONES_GENERATION, PREVIOUS_ANTI_FRAUD_NEW_CLONES_GENERATION))
    }

    @Test
    fun `update new clones results when no new generation`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_NEW_CLONES_GENERATION.name))
            .thenReturn(LAST_ANTI_FRAUD_NEW_CLONES_GENERATION)

        antiFraudClchManager.updateClonesResults()

        verify(ytService, never()).readTableJson(any(), eq(AntiFraudClchResult::class.java))
        verify(antiFraudClchResultService, never()).save(anyList())
    }

    @Test
    fun `update new clones results when exists new generation`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_NEW_CLONES_GENERATION.name))
            .thenReturn(PREVIOUS_ANTI_FRAUD_NEW_CLONES_GENERATION)
        val cloneCheckResult = createCloneCheckResult()
        whenever(ytService.readTableJson(any(), eq(AntiFraudClchResult::class.java)))
            .thenReturn(listOf(cloneCheckResult))

        antiFraudClchManager.updateClonesResults()
        verify(ytService).readTableJson(
            eq(YPath.simple("$NEW_CLONES_TABLES_PATH/$LAST_ANTI_FRAUD_NEW_CLONES_GENERATION")),
            eq(AntiFraudClchResult::class.java)
        )
        verify(antiFraudClchResultService).save(listOf(cloneCheckResult))
    }

    @Test
    fun `update new clones results when check result already saved`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_ANTI_FRAUD_NEW_CLONES_GENERATION.name))
            .thenReturn(PREVIOUS_ANTI_FRAUD_NEW_CLONES_GENERATION)
        val cloneCheckResult = createCloneCheckResult()
        whenever(ytService.readTableJson(any(), eq(AntiFraudClchResult::class.java)))
            .thenReturn(listOf(cloneCheckResult))
        whenever(antiFraudClchResultService.findAllCheckResultsForShops(setOf(SHOP_ID)))
            .thenReturn(listOf(cloneCheckResult))

        antiFraudClchManager.updateClonesResults()
        verify(antiFraudClchResultService, Mockito.never()).save(anyList())
    }

    private fun createCloneCheckResult() = AntiFraudClchResult().apply {
        compositeId = AntiFraudClchResult.Key(SHOP_ID, CLONE_SHOP_ID)
        cloneFeatures = arrayOf(UNKNOWN)
        distance = 1.0
        glueContacts = AntiFraudGlueContacts().apply {
            sameContacts = arrayOf("123@123.ru", "+777777777")
            sameJurInfo = null
        }
    }

    companion object {
        private const val LAST_ANTI_FRAUD_NEW_CLONES_GENERATION = "20220118_0433"
        private const val PREVIOUS_ANTI_FRAUD_NEW_CLONES_GENERATION = "20220118_0142"
        private const val SHOP_ID = 1L
        private const val CLONE_SHOP_ID = 2L
    }
}
