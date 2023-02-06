package ru.yandex.direct.grid.processing.service.client

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.db.PpcPropertyNames.METRIKA_COUNTER_IDS_EXCLUDED_FROM_AUTOCOMPLETE
import ru.yandex.direct.common.db.PpcPropertyNames.METRIKA_COUNTER_LIFETIME_IN_DAYS_FOR_AUTOCOMPLETE
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.metrika.model.MetrikaCounterByDomain
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdInaccessibleMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounter
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounterStatus.ACCESS_REQUESTED
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounterStatus.NOT_FOUND
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCounterStatus.NO_RIGHTS
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersByDomainPayload
import ru.yandex.direct.grid.processing.model.client.GdMetrikaCountersInfoPayload
import ru.yandex.direct.grid.processing.model.client.GdShowCpmUacPromoParams
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper.setDirectAuthentication
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.checkEquals
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val EXISTENT_COUNTER_ID = 1L
private const val NON_EXISTENT_COUNTER_ID = 2L
private const val ACCESS_REQUESTED_COUNTER_ID = 3L

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientGraphQlServiceTest {

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var steps: Steps

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var userInfo: UserInfo
    private lateinit var existentCounter: CounterInfoDirect

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()
        val anotherUserInfo = steps.userSteps().createDefaultUser()

        existentCounter = buildCounter(EXISTENT_COUNTER_ID.toInt())
        metrikaClientStub.addUserCounters(userInfo.uid, listOf(existentCounter))
        metrikaClientStub.addUserCounter(anotherUserInfo.uid, ACCESS_REQUESTED_COUNTER_ID.toInt())
        metrikaClientStub.requestAccess(userInfo.uid, ACCESS_REQUESTED_COUNTER_ID)

        setDirectAuthentication(userInfo.user!!)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)

        prepareForGetMetrikaCountersByDomain()
    }

    @Test
    fun metrikaCountersInfo_ExistentCounter() {
        ktGraphQLTestExecutor.metrikaCountersInfo(setOf(EXISTENT_COUNTER_ID))
            .checkEquals(GdMetrikaCountersInfoPayload().apply {
                isMetrikaAvailable = true
                counters = setOf(
                    GdMetrikaCounter().apply {
                        id = EXISTENT_COUNTER_ID
                        status = NO_RIGHTS
                    }
                )
            })
    }

    @Test
    fun metrikaCountersInfo_NonExistentCounter() {
        ktGraphQLTestExecutor.metrikaCountersInfo(setOf(NON_EXISTENT_COUNTER_ID))
            .checkEquals(GdMetrikaCountersInfoPayload().apply {
                isMetrikaAvailable = true
                counters = setOf(
                    GdMetrikaCounter().apply {
                        id = NON_EXISTENT_COUNTER_ID
                        status = NOT_FOUND
                    }
                )
            })
    }

    @Test
    fun metrikaCountersInfo_AccessRequestedCounter() {
        ktGraphQLTestExecutor.metrikaCountersInfo(setOf(ACCESS_REQUESTED_COUNTER_ID))
            .checkEquals(GdMetrikaCountersInfoPayload().apply {
                isMetrikaAvailable = true
                counters = setOf(
                    GdMetrikaCounter().apply {
                        id = ACCESS_REQUESTED_COUNTER_ID
                        status = ACCESS_REQUESTED
                    }
                )
            })
    }

    @Test
    fun metrikaCountersInfo_MultipleTest() {
        ktGraphQLTestExecutor.metrikaCountersInfo(
            setOf(EXISTENT_COUNTER_ID, NON_EXISTENT_COUNTER_ID, ACCESS_REQUESTED_COUNTER_ID))
            .checkEquals(GdMetrikaCountersInfoPayload().apply {
                isMetrikaAvailable = true
                counters = setOf(
                    GdMetrikaCounter().apply {
                        id = EXISTENT_COUNTER_ID
                        status = NO_RIGHTS
                    },
                    GdMetrikaCounter().apply {
                        id = NON_EXISTENT_COUNTER_ID
                        status = NOT_FOUND
                    },
                    GdMetrikaCounter().apply {
                        id = ACCESS_REQUESTED_COUNTER_ID
                        status = ACCESS_REQUESTED
                    }
                )
            })
    }

    @Test
    fun getMetrikaCountersByDomain_SimpleTest() {
        ktGraphQLTestExecutor.metrikaCountersByDomain(userInfo.login, "https://artelbook.ru")
            .checkEquals(GdMetrikaCountersByDomainPayload().apply {
                allAccessibleCounters = setOf(
                    existentCounter.convertToGd()
                )
                accessibleCountersByDomain = setOf(
                    existentCounter.convertToGd()
                )
                inaccessibleCountersByDomain = setOf(
                    GdInaccessibleMetrikaCounter().apply {
                        id = ACCESS_REQUESTED_COUNTER_ID
                        accessRequested = true
                    }
                )
                isMetrikaAvailable = true
            })
    }

    @Test
    fun getMetrikaCountersByDomain_OneCounterOutdated_AnotherCounterReturned() {
        ppcPropertiesSupport.set(METRIKA_COUNTER_LIFETIME_IN_DAYS_FOR_AUTOCOMPLETE, "3")

        ktGraphQLTestExecutor.metrikaCountersByDomain(userInfo.login, "https://artelbook.ru")
            .checkEquals(GdMetrikaCountersByDomainPayload().apply {
                allAccessibleCounters = setOf(
                    existentCounter.convertToGd()
                )
                accessibleCountersByDomain = setOf()
                inaccessibleCountersByDomain = setOf(
                    GdInaccessibleMetrikaCounter().apply {
                        id = ACCESS_REQUESTED_COUNTER_ID
                        accessRequested = true
                    }
                )
                isMetrikaAvailable = true
            })
    }

    @Test
    fun getMetrikaCountersByDomain_AllCountersOutdated_NoCountersReturned() {
        ppcPropertiesSupport.set(METRIKA_COUNTER_LIFETIME_IN_DAYS_FOR_AUTOCOMPLETE, "1")

        ktGraphQLTestExecutor.metrikaCountersByDomain(userInfo.login, "https://artelbook.ru")
            .checkEquals(GdMetrikaCountersByDomainPayload().apply {
                allAccessibleCounters = setOf(
                    existentCounter.convertToGd()
                )
                accessibleCountersByDomain = setOf()
                inaccessibleCountersByDomain = setOf()
                isMetrikaAvailable = true
            })
    }

    @Test
    fun getMetrikaCountersByDomain_AccessibleMetaCounter() {
        ppcPropertiesSupport.set(METRIKA_COUNTER_IDS_EXCLUDED_FROM_AUTOCOMPLETE, "$EXISTENT_COUNTER_ID")

        ktGraphQLTestExecutor.metrikaCountersByDomain(userInfo.login, "https://artelbook.ru")
            .checkEquals(GdMetrikaCountersByDomainPayload().apply {
                allAccessibleCounters = setOf(
                    existentCounter.convertToGd()
                )
                accessibleCountersByDomain = setOf()
                inaccessibleCountersByDomain = setOf(
                    GdInaccessibleMetrikaCounter().apply {
                        id = ACCESS_REQUESTED_COUNTER_ID
                        accessRequested = true
                    }
                )
                isMetrikaAvailable = true
            })
    }

    @Test
    fun getMetrikaCountersByDomain_InaccessibleMetaCounter() {
        ppcPropertiesSupport.set(METRIKA_COUNTER_IDS_EXCLUDED_FROM_AUTOCOMPLETE, "$ACCESS_REQUESTED_COUNTER_ID")

        ktGraphQLTestExecutor.metrikaCountersByDomain(userInfo.login, "https://artelbook.ru")
            .checkEquals(GdMetrikaCountersByDomainPayload().apply {
                allAccessibleCounters = setOf(
                    existentCounter.convertToGd()
                )
                accessibleCountersByDomain = setOf(
                    existentCounter.convertToGd()
                )
                inaccessibleCountersByDomain = setOf()
                isMetrikaAvailable = true
            })
    }

    @Test
    fun getShowCpmUacPromoParams_NullProperty() {
        ppcPropertiesSupport.set(PpcPropertyNames.CPM_UAC_PROMO_COLORS, null)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SHOW_CPM_UAC_PROMO, false)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SHOW_CPM_UAC_PROMO_BONUS, false)
        ktGraphQLTestExecutor.showCpmUacParams(userInfo.login)
            .checkEquals(GdShowCpmUacPromoParams().apply {
                showCpmUacPromo = false
                showCpmUacPromoBonus = false
                colorLeft = "#8a84ff"
                colorRight = "#2e69ff"
            })
    }

    @Test
    fun getShowCpmUacPromoParams_NotNullProperty() {
        ppcPropertiesSupport.set(PpcPropertyNames.CPM_UAC_PROMO_COLORS, "#aaaf46, #fff123")
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SHOW_CPM_UAC_PROMO, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.SHOW_CPM_UAC_PROMO_BONUS, true)
        ktGraphQLTestExecutor.showCpmUacParams(userInfo.login)
            .checkEquals(GdShowCpmUacPromoParams().apply {
                showCpmUacPromo = true
                showCpmUacPromoBonus = true
                colorLeft = "#aaaf46"
                colorRight = "#fff123"
            })
    }

    @Test
    fun cpmUacNewCamp_featureOn() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP_LAST, true)
        ktGraphQLTestExecutor.cpmUacNewCamp(userInfo.login)
            .checkEquals(true)
    }

    @Test
    fun cpmUacNewCamp_featureOffAll() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP, false)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP_LAST, false)
        ktGraphQLTestExecutor.cpmUacNewCamp(userInfo.login)
            .checkEquals(false)
    }

    @Test
    fun cpmUacNewCamp_featureOff_lastDna() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP, false)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP_LAST, true)
        //Создать две рк
        steps.campaignSteps().createCampaign(
            activeCpmBannerCampaign(userInfo.clientId, userInfo.uid).withSource(CampaignSource.UAC),
            userInfo.clientInfo)
        steps.campaignSteps().createCampaign(
            activeCpmBannerCampaign(userInfo.clientId, userInfo.uid).withSource(CampaignSource.DIRECT),
            userInfo.clientInfo)
        ktGraphQLTestExecutor.cpmUacNewCamp(userInfo.login)
            .checkEquals(false)
    }

    @Test
    fun cpmUacNewCamp_featureOff_lastUac() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP, false)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.CPM_UAC_NEW_CAMP_LAST, true)
        //Создать две рк
        steps.campaignSteps().createCampaign(
            activeCpmBannerCampaign(userInfo.clientId, userInfo.uid).withSource(CampaignSource.DIRECT),
            userInfo.clientInfo)
        steps.campaignSteps().createCampaign(
            activeCpmBannerCampaign(userInfo.clientId, userInfo.uid).withSource(CampaignSource.UAC),
            userInfo.clientInfo)
        ktGraphQLTestExecutor.cpmUacNewCamp(userInfo.login)
            .checkEquals(true)
    }

    private fun prepareForGetMetrikaCountersByDomain() {
        ppcPropertiesSupport.remove(METRIKA_COUNTER_IDS_EXCLUDED_FROM_AUTOCOMPLETE)
        ppcPropertiesSupport.remove(METRIKA_COUNTER_LIFETIME_IN_DAYS_FOR_AUTOCOMPLETE)

        val nowEpochSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val secondsInOneDay = Duration.ofDays(1).toSeconds()

        doReturn(listOf(
            MetrikaCounterByDomain().apply {
                counterId = EXISTENT_COUNTER_ID
                ownerUid = userInfo.uid
                timestamp = nowEpochSecond - 4 * secondsInOneDay
            },
            MetrikaCounterByDomain().apply {
                counterId = ACCESS_REQUESTED_COUNTER_ID
                ownerUid = userInfo.uid xor 1
                timestamp = nowEpochSecond - 2 * secondsInOneDay
            }
        )).`when`(metrikaCounterByDomainRepository).getRestrictedCountersByDomain(anyString(), anyBoolean())
    }
}

fun CounterInfoDirect.convertToGd(): GdClientMetrikaCounter {
    return GdClientMetrikaCounter()
        .withId(id.toLong())
        .withName(name)
        .withDomain(sitePath)
}
