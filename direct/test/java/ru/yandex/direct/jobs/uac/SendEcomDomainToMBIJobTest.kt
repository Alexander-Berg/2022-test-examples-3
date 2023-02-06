package ru.yandex.direct.jobs.uac

import com.nhaarman.mockitokotlin2.*
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.feed.repository.FeedRepository
import ru.yandex.direct.core.entity.feed.service.MbiService
import ru.yandex.direct.core.entity.uac.model.EcomDomain
import ru.yandex.direct.core.entity.uac.repository.mysql.EcomDomainsRepository
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.market.client.MarketClient

internal class SendEcomDomainToMBIJobTest {

    @Mock
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Mock
    private lateinit var ecomDomainsRepository: EcomDomainsRepository

    @Mock
    private lateinit var marketClient: MarketClient

    private lateinit var mbiService: MbiService

    private lateinit var job: SendEcomDomainToMBIJob

    @BeforeEach
    fun initTestData() {
        MockitoAnnotations.openMocks(this)

        val property = mock<PpcProperty<Long>>()
        whenever(property.getOrDefault(any()))
            .doReturn(10L)
        whenever(ppcPropertiesSupport.get(eq(PpcPropertyNames.MIN_OFFERS_COUNT_BY_HOST_FOR_SEND_TO_MBI)))
            .doReturn(property)

        mbiService = MbiService(
            Mockito.mock(ShardHelper::class.java),
            marketClient,
            Mockito.mock(FeedRepository::class.java),
            Mockito.mock(GeminiClient::class.java))

        job = SendEcomDomainToMBIJob(ecomDomainsRepository, mbiService, ppcPropertiesSupport)
    }

    @Test
    fun checkJob() {
        val ecomDomains = getEcomDomains(5)
        val results = getValidResultsByDomains(ecomDomains)

        whenever(ecomDomainsRepository.getWithoutMarketIds(any(), any()))
            .doReturn(ecomDomains)

        whenever(marketClient.addSitePreviewToMBI(any()))
            .doAnswer {
                val argument: MarketClient.SitePreviewInfo = it.getArgument(0)
                val domain = argument.domain
                results[domain]
            }

        whenever(ecomDomainsRepository.update(any()))
            .doReturn(ecomDomains.size)

        job.execute()

        verify(ecomDomainsRepository).getWithoutMarketIds(any(), any())

        verify(marketClient, times(ecomDomains.size)).addSitePreviewToMBI(any())

        verify(ecomDomainsRepository).update(argThat {
            this.size == ecomDomains.size
        })
    }

    @Test
    fun checkJob_withEmptyDomains() {
        val ecomDomains = emptyList<EcomDomain>()

        whenever(ecomDomainsRepository.getWithoutMarketIds(any(), any()))
            .doReturn(ecomDomains)

        job.execute()

        verify(ecomDomainsRepository).getWithoutMarketIds(any(), any())
        verifyNoMoreInteractions(ecomDomainsRepository)

        verifyZeroInteractions(marketClient)
    }

    @Test
    fun checkJob_withMarketExceptions() {
        val ecomDomains = getEcomDomains(10)
        val idsWithErrors = setOf(1L, 4L, 7L)
        val results = getValidResultsByDomains(ecomDomains, idsWithErrors)

        whenever(ecomDomainsRepository.getWithoutMarketIds(any(), any()))
            .doReturn(ecomDomains)

        whenever(marketClient.addSitePreviewToMBI(any()))
            .doAnswer {
                val argument: MarketClient.SitePreviewInfo = it.getArgument(0)
                val domain = argument.domain
                results[domain] ?: throw RuntimeException()
            }

        whenever(ecomDomainsRepository.update(any()))
            .doReturn(ecomDomains.size - idsWithErrors.size)

        job.execute()

        verify(ecomDomainsRepository).getWithoutMarketIds(any(), any())

        verify(marketClient, times(ecomDomains.size)).addSitePreviewToMBI(any())

        verify(ecomDomainsRepository).update(argThat {
            this.size == ecomDomains.size - idsWithErrors.size
        })
    }

    private fun getEcomDomains(count: Int): List<EcomDomain> {
        return (1..count).map {
            EcomDomain().withId(it.toLong())
                .withDomain(randomAlphanumeric(16))
                .withSchema(if (it % 2 == 0) "http" else "https")
        }
    }

    private fun getValidResultsByDomains(domains: List<EcomDomain>,
                                         excludedIds: Set<Long> = emptySet()): Map<String, MarketClient.SendFeedResult?> {
        val range = 0..Long.MAX_VALUE

        return domains.associateBy(EcomDomain::getDomain) {
            if (excludedIds.contains(it.id)) {
                null
            } else {
                MarketClient.SendFeedResult(range.random(), range.random(), range.random())
            }
        }
    }

}
