package ru.yandex.market.abo.core.checkorder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam
import ru.yandex.market.abo.core.checkorder.CheckOrderOffersProvider.Companion.sortByDimensions
import ru.yandex.market.abo.core.datacamp.client.DataCampClient
import ru.yandex.market.abo.core.offer.report.Offer
import ru.yandex.market.abo.core.offer.report.OfferService
import ru.yandex.market.abo.util.db.toggle.CachedToggleService
import ru.yandex.market.common.report.MarketReportOverloadedException
import ru.yandex.market.common.report.indexer.IdxAPI
import ru.yandex.market.common.report.indexer.model.checksupplier.CheckSupplierResult
import ru.yandex.market.common.report.indexer.model.checksupplier.ErrorEntry
import ru.yandex.market.mbi.datacamp.saas.SaasService

class CheckOrderOffersProviderTest {

    private val offerService: OfferService = mock()
    private val cachedToggleService: CachedToggleService = mock()
    private val saasService: SaasService = mock()
    private val datacampClient: DataCampClient = mock()
    private val idxApiService: IdxAPI = mock()
    private val idxApiSandboxService: IdxAPI = mock()
    private val checkOrderOffersProvider = CheckOrderOffersProvider(
        offerService,
        cachedToggleService,
        saasService,
        datacampClient,
        idxApiService,
        idxApiSandboxService
    )

    private val createOrderParam: CreateOrderParam = mock()


    @Test
    fun reportOverloadException() {
        whenever(checkOrderOffersProvider.findOffersForOrderInReport(createOrderParam)).thenThrow(
            MarketReportOverloadedException::class.java
        )
        try {
            checkOrderOffersProvider.findOffersForOrder(createOrderParam)
        } catch (ex: CheckOrderCreationException) {
            assertEquals(CheckOrderScenarioErrorType.REPORT_OVERLOAD, ex.scenarioError.errorType)
        }
    }

    @Test
    fun testSortByWeight() {
        val tiny = offer(1, 1, 1, 1)
        val heavy = offer(100, 1, 1, 1)
        val high = offer(1, 1, 100, 1)
        val fat = offer(1, 100, 1, 1)
        val deep = offer(1, 1, 1, 100)
        val giant = offer(100, 100, 100, 100)
        val empty = Offer()
        val sorted = listOf(tiny, high, giant, heavy, fat, deep, empty).sortByDimensions()
        assertEquals(java.util.List.of(empty, tiny, deep, fat, high, heavy, giant), sorted)
    }

    @Test
    fun testNoOfferExceptionDetails() {
        val errorEntry1 = ErrorEntry()
        errorEntry1.text = "Offer's vat doesn't match shop's taxing system: "
        val errorEntry2 = ErrorEntry()
        errorEntry2.text = "Offer price is not specified"
        val checkSupplierResult = CheckSupplierResult()
        checkSupplierResult.dataCampStatistics = listOf(errorEntry1, errorEntry2)
        whenever(idxApiSandboxService.checkSupplierResult(1L, null, null, null, true)).thenReturn(checkSupplierResult)
        try {
            checkOrderOffersProvider.collectNoOfferDetailsAndThrowException(1L, true)
        } catch (exception: CheckOrderCreationException) {
            assertNotNull(exception)
            val checkOrderScenarioError = exception.scenarioError
            assertNotNull(checkOrderScenarioError)
            assertEquals(CheckOrderScenarioErrorType.NO_OFFERS, checkOrderScenarioError.errorType)
            val scenarioErrorDetails = checkOrderScenarioError.errorDetails
            assertNotNull(scenarioErrorDetails)
            assertEquals(1, scenarioErrorDetails.size)
            val scenarioErrorDetail = scenarioErrorDetails[0]
            assertEquals(ScenarioErrorParam.OFFER_ERRORS, scenarioErrorDetail.paramName)
        }

    }

    private fun offer(weight: Int, width: Int, height: Int, depth: Int) = Offer().apply {
        this.weight = weight.toBigDecimal()
        this.width = width.toBigDecimal()
        this.depth = depth.toBigDecimal()
        this.height = height.toBigDecimal()
    }
}
