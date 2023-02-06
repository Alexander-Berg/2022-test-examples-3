package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames.DOMAINS_NOT_ALLOWED_FOR_UNAVAILABLE_COUNTERS
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.TURBODIRECT
import ru.yandex.direct.core.entity.metrika.model.MetrikaCounterByDomain
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.uac.model.ShopInShopBusiness
import ru.yandex.direct.core.entity.uac.model.Source
import ru.yandex.direct.core.entity.uac.repository.mysql.ShopInShopBusinessesRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdInaccessibleMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounterSource
import ru.yandex.direct.grid.processing.service.client.ClientDataService.MAX_INACCESSIBLE_METRIKA_COUNTERS_COUNT_FOR_SUGGEST
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.time.LocalDateTime
import java.time.ZoneOffset

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientDataServiceMetrikaCountersTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var clientDataService: ClientDataService
    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    @Autowired
    private lateinit var shopInShopBusinessesRepository: ShopInShopBusinessesRepository
    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository
    @Autowired
    private lateinit var metrikaClient: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var userInfo: UserInfo
    private lateinit var userCounter: MetrikaCounterByDomain
    private lateinit var domainCounter: MetrikaCounterByDomain

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        userInfo = clientInfo.chiefUserInfo!!
        metrikaClient.addUserCounter(userInfo.uid, userCounterId.toInt())
        metrikaClient.addUserCounter(userInfo.uid, marketCounterId.toInt(), CampaignConverter.MARKET)
        metrikaClient.addUserCounter(userInfo.uid, shopInShopCounterId.toInt())
        metrikaClient.addUnavailableCounter(domainCounterId, true)
        metrikaClient.addUnavailableCounter(notAllowedByMetrikaCounterId, false)
        metrikaClient.addUnavailableCounter(unavailableTurboCounterId, TURBODIRECT)

        userCounter = createMetrikaCounterByDomain(userCounterId)
        domainCounter = createMetrikaCounterByDomain(domainCounterId)
    }

    @After
    fun after() {
        Mockito.reset(metrikaCounterByDomainRepository, shopInShopBusinessesRepository)
        metrikaClient.clearUnavailableCounters()
        ppcPropertiesSupport.remove(DOMAINS_NOT_ALLOWED_FOR_UNAVAILABLE_COUNTERS)
    }

    @Test
    fun getMetrikaCountersByUrl_success() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(listOf(userCounter, domainCounter))

        val url = "https://somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf(gdDomainCounter))
    }

    @Test
    fun getMetrikaCountersByUrl_market_shopInShopDisabled() {
        val url = "https://market.yandex.ru/store--marvel-kt?businessId=860533"
        getAndAssertCounters(url, listOf(gdMarketCounter), listOf())
    }

    @Test
    fun getMetrikaCountersByUrl_market_withShopInShop() {
        steps.featureSteps().addClientFeature(clientId,
            FeatureName.ENABLE_FEED_AND_COUNTER_SUGGESTION_BY_SHOP_IN_SHOP, true)
        val url = "https://market.yandex.ru/store--marvel-kt?businessId=860533"
        whenever(shopInShopBusinessesRepository.getBySourceAndBusinessId(any(), any()))
            .thenReturn(ShopInShopBusiness().apply {
                source = Source.MARKET
                businessId = 860533
                feedUrl = url
                counterId = shopInShopCounterId
            })
        getAndAssertCounters(url, listOf(gdShopInShopCounter), listOf())
    }

    @Test
    fun getMetrikaCountersByUrl_market_withoutShopInShop() {
        steps.featureSteps().addClientFeature(clientId,
            FeatureName.ENABLE_FEED_AND_COUNTER_SUGGESTION_BY_SHOP_IN_SHOP, true)
        val url = "https://market.yandex.ru/store--marvel-kt?businessId=860533"
        getAndAssertCounters(url, listOf(gdMarketCounter), listOf())
    }

    @Test
    fun getMetrikaCountersByUrl_punycodeDomain() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("дом.рф"), anyBoolean()))
            .thenReturn(listOf(userCounter, domainCounter))

        val url = "https://xn--d1aqf.xn--p1ai"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf(gdDomainCounter))
    }

    @Test
    fun getMetrikaCountersByUrl_inaccessibleCounterNotAllowedByDomainsBlacklist() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(listOf(userCounter, domainCounter))
        ppcPropertiesSupport.set(DOMAINS_NOT_ALLOWED_FOR_UNAVAILABLE_COUNTERS, "somesite.com")

        val url = "https://Somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf())
    }

    @Test
    fun getMetrikaCountersByUrl_maxInaccessibleCounters() {
        val inaccessibleCounters = (1..MAX_INACCESSIBLE_METRIKA_COUNTERS_COUNT_FOR_SUGGEST)
            .map { RandomNumberUtils.nextPositiveInteger() }
            .map { createMetrikaCounterByDomain(it.toLong()) }
            .toMutableList()
        inaccessibleCounters.forEach { metrikaClient.addUnavailableCounter(it.counterId) }
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(inaccessibleCounters.plus(userCounter))

        val url = "https://Somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter),
            inaccessibleCounters.map { createGdInaccessibleMetrikaCounter(it.counterId) })
    }

    @Test
    fun getMetrikaCountersByUrl_tooManyInaccessibleCounters() {
        val inaccessibleCounters = (1..MAX_INACCESSIBLE_METRIKA_COUNTERS_COUNT_FOR_SUGGEST + 1)
            .map { RandomNumberUtils.nextPositiveInteger() }
            .map { createMetrikaCounterByDomain(it.toLong()) }
            .toMutableList()
        inaccessibleCounters.forEach { metrikaClient.addUnavailableCounter(it.counterId) }
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(inaccessibleCounters.plus(userCounter))

        val url = "https://Somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf())
    }

    @Test
    fun getMetrikaCountersByUrl_notExistentCounter() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(listOf(userCounter, domainCounter, createMetrikaCounterByDomain(notExistentDomainCounterId)))

        val url = "https://somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf(gdDomainCounter))
    }

    @Test
    fun getMetrikaCountersByUrl_inaccessibleCountersNotAllowedByMetrikaFlag() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(listOf(userCounter, domainCounter, createMetrikaCounterByDomain(notAllowedByMetrikaCounterId)))

        val url = "https://somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf(gdDomainCounter))
    }

    @Test
    fun getMetrikaCountersByUrl_inaccessibleTurboCounter() {
        `when`(metrikaCounterByDomainRepository.getAllCountersByDomain(eq("somesite.com"), anyBoolean()))
            .thenReturn(listOf(userCounter, createMetrikaCounterByDomain(unavailableTurboCounterId)))

        val url = "https://somesite.com"
        getAndAssertCounters(url, listOf(gdUserCounter), listOf(gdUnavailableTurboCounter))
    }

    private fun createMetrikaCounterByDomain(counterId: Long) =
        MetrikaCounterByDomain().apply {
            this.counterId = counterId
            ownerUid = userInfo.uid
            timestamp = LocalDateTime.now().minusDays(2).toEpochSecond(ZoneOffset.UTC)
        }

    private fun getAndAssertCounters(url: String,
                                     expectedAccessibleCounters: List<GdClientMetrikaCounter>,
                                     expectedInaccessibleCounters: List<GdInaccessibleMetrikaCounter>) {
        val payload = clientDataService.getMetrikaCountersByUrl(clientInfo.uid, userInfo.user!!, url, true)
        SoftAssertions.assertSoftly {
            it.assertThat(payload.isMetrikaAvailable).isTrue
            it.assertThat(payload.accessibleCountersByDomain)
                .`as`("accessible counters")
                .usingRecursiveFieldByFieldElementComparatorOnFields("id", "source")
                .containsExactlyInAnyOrderElementsOf(expectedAccessibleCounters)
            it.assertThat(payload.inaccessibleCountersByDomain)
                .`as`("inaccessible counters")
                .usingRecursiveFieldByFieldElementComparatorOnFields("id", "source")
                .containsExactlyInAnyOrderElementsOf(expectedInaccessibleCounters)
        }
    }

    companion object {
        private val userCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val domainCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val notExistentDomainCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val marketCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val shopInShopCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val notAllowedByMetrikaCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val unavailableTurboCounterId = RandomNumberUtils.nextPositiveInteger().toLong()

        private val gdUserCounter = createGdClientMetrikaCounter(userCounterId)
        private val gdMarketCounter = createGdClientMetrikaCounter(marketCounterId, GdMetrikaCounterSource.MARKET)
        private val gdShopInShopCounter = createGdClientMetrikaCounter(shopInShopCounterId)
        private val gdDomainCounter = createGdInaccessibleMetrikaCounter(domainCounterId)
        private val gdUnavailableTurboCounter =
            createGdInaccessibleMetrikaCounter(unavailableTurboCounterId, GdMetrikaCounterSource.TURBO_DIRECT)
    }
}

private fun createGdClientMetrikaCounter(
    counterId: Long,
    source: GdMetrikaCounterSource? = null,
) = GdClientMetrikaCounter().apply {
    id = counterId
    this.source = source
}

private fun createGdInaccessibleMetrikaCounter(
    counterId: Long,
    source: GdMetrikaCounterSource? = null,
) = GdInaccessibleMetrikaCounter().apply {
    id = counterId
    this.source = source
}
