package ru.yandex.direct.grid.processing.service.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils
import ru.yandex.direct.core.entity.metrika.model.MetrikaCounterByDomain
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.DomainInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdSocialNetworkAccountType
import ru.yandex.direct.grid.processing.model.campaign.GdSuggestSocialNetworkAccount
import ru.yandex.direct.grid.processing.model.client.GdCalltrackingOnSiteSuggest
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounterForSuggest
import ru.yandex.direct.grid.processing.model.goal.GdGoal
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaCounterGoalType
import ru.yandex.direct.grid.processing.model.trackingphone.GdCalltrackingPhoneOnSite
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.servlet.http.HttpServletRequest

private const val COUNTER_ID = 111L
private const val GOAL_ID = 1111
private const val UNAVAILABLE_COUNTER_ID = 112L
private const val UNAVAILABLE_GOAL_ID = 1121L
private const val UNAVAILABLE_ECOMMERCE_COUNTER_ID = 113L

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientGraphQlServiceSuggestDataTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    @Autowired
    private lateinit var metriGoalsService: MetrikaGoalsService
    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo
    private lateinit var domainInfo: DomainInfo

    private var chiefUid = -1L
    private var shard = 0

    @Before
    fun setUp() {
        val userInfo = steps.userSteps().createDefaultUser()
        val anotherUserInfo = steps.userSteps().createDefaultUser()
        val user = userInfo.user!!
        clientInfo = userInfo.clientInfo!!
        chiefUid = user.chiefUid
        shard = clientInfo.shard

        domainInfo = steps.domainSteps().createDomain(shard, ".ru")

        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        metrikaClientStub.addUnavailableCounter(UNAVAILABLE_COUNTER_ID)
        metrikaClientStub.addCounterGoal(UNAVAILABLE_COUNTER_ID.toInt(), UNAVAILABLE_GOAL_ID.toInt())
        metrikaClientStub.addUnavailableEcommerceCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID)
        Mockito.doReturn(listOf(
            createMetrikaCounterByDomain(COUNTER_ID, chiefUid),
            createMetrikaCounterByDomain(UNAVAILABLE_COUNTER_ID, anotherUserInfo.uid),
            createMetrikaCounterByDomain(UNAVAILABLE_ECOMMERCE_COUNTER_ID, anotherUserInfo.uid),
        )
        ).`when`(metrikaCounterByDomainRepository).getRestrictedCountersByDomain(anyString(), anyBoolean())

        val request = Mockito.mock(HttpServletRequest::class.java)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        `when`(request.cookies).thenReturn(arrayOf())

        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)
    }

    @After
    fun after() {
        metrikaClientStub.clearUnavailableCounters()
    }

    @Test
    fun test_hasCalltrackingOnSiteCampaigns() {
        val phone = ClientPhoneTestUtils.getUniqPhone()
        val calltrackingSettingsId = steps.calltrackingSettingsSteps().add(
            clientInfo.clientId,
            domainInfo.domainId,
            COUNTER_ID,
            listOf(phone)
        )

        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val campaignId = campaignInfo.campaignId
        steps.campCalltrackingSettingsSteps().link(shard, campaignId, calltrackingSettingsId)

        metrikaClientStub.addUserCounter(
            chiefUid,
            MetrikaClientStub.buildCounter(COUNTER_ID.toInt(), "system", MetrikaCounterPermission.EDIT)
        )
        metrikaClientStub.addCounterGoal(COUNTER_ID.toInt(), GOAL_ID)

        val payload = ktGraphQLTestExecutor.suggestDataByUrl("https://${domainInfo.domain.domain}/")
        SoftAssertions.assertSoftly {
            it.assertThat(payload.calltrackingOnSite).`as`("calltrackingOnSite").isEqualTo(
                GdCalltrackingOnSiteSuggest().apply {
                    campaignIds = setOf(campaignId)
                    settingsId = calltrackingSettingsId
                    counterId = COUNTER_ID
                    hasExternalCalltracking = false
                    phones = listOf(GdCalltrackingPhoneOnSite().apply {
                        hasClicks = false
                        redirectPhone = phone
                        isCalltrackingOnSiteActive = false
                        isCalltrackingOnSiteEnabled = true
                    })
                }
            )
            it.assertThat(payload.counterIds).`as`("counterIds").containsExactlyInAnyOrder(
                COUNTER_ID, UNAVAILABLE_COUNTER_ID, UNAVAILABLE_ECOMMERCE_COUNTER_ID
            )
            it.assertThat(payload.counters).`as`("counters").containsExactlyInAnyOrder(
                createAvailableGdCounter(isEditableByOperator = true, isCalltrackingOnSiteCompatible = false),
                createUnavailableGdCounter(UNAVAILABLE_COUNTER_ID),
                createUnavailableGdCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID),
            )
            it.assertThat(payload.goals).`as`("goals").containsExactlyInAnyOrder(
                createGdGoal(GOAL_ID.toLong(), COUNTER_ID),
            )
        }
    }

    @Test
    fun test_hasNotCalltrackingOnSiteCampaigns() {
        metrikaClientStub.addUserCounter(
            chiefUid,
            MetrikaClientStub.buildCounter(COUNTER_ID.toInt(), null, MetrikaCounterPermission.VIEW)
        )
        metrikaClientStub.addCounterGoal(COUNTER_ID.toInt(), GOAL_ID)

        val payload = ktGraphQLTestExecutor.suggestDataByUrl("https://${domainInfo.domain.domain}/")
        SoftAssertions.assertSoftly {
            it.assertThat(payload.calltrackingOnSite).`as`("calltrackingOnSite").isEqualTo(
                GdCalltrackingOnSiteSuggest().apply {
                    campaignIds = setOf()
                    phones = listOf()
                }
            )
            it.assertThat(payload.counterIds).`as`("counterIds").containsExactlyInAnyOrder(
                COUNTER_ID, UNAVAILABLE_COUNTER_ID, UNAVAILABLE_ECOMMERCE_COUNTER_ID
            )
            it.assertThat(payload.counters).`as`("counters").containsExactlyInAnyOrder(
                createAvailableGdCounter(isEditableByOperator = false, isCalltrackingOnSiteCompatible = true),
                createUnavailableGdCounter(UNAVAILABLE_COUNTER_ID),
                createUnavailableGdCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID),
            )
            it.assertThat(payload.goals).`as`("goals").containsExactlyInAnyOrder(
                createGdGoal(GOAL_ID.toLong(), COUNTER_ID),
            )
        }
    }

    @Test
    fun test_hasInstagramAccountUrl() {
        val payload = ktGraphQLTestExecutor.suggestDataByUrl("https://instagram.com/mylogin")
        assertThat(payload.campaignSettings.socialNetworkAccount).isEqualTo(
                GdSuggestSocialNetworkAccount().apply {
                    username = "mylogin"
                    type = GdSocialNetworkAccountType.INSTAGRAM
                }
        )
    }

    @Test
    fun test_unavailableGoalsAllowed() {
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED, true)
        metrikaClientStub.addUserCounter(
            chiefUid,
            MetrikaClientStub.buildCounter(COUNTER_ID.toInt(), null, MetrikaCounterPermission.VIEW)
        )
        metrikaClientStub.addCounterGoal(COUNTER_ID.toInt(), GOAL_ID)

        val payload = ktGraphQLTestExecutor.suggestDataByUrl("https://${domainInfo.domain.domain}/")
        SoftAssertions.assertSoftly {
            it.assertThat(payload.calltrackingOnSite).isEqualTo(GdCalltrackingOnSiteSuggest().apply {
                campaignIds = setOf()
                phones = listOf()
            })
            it.assertThat(payload.counterIds).`as`("counterIds").containsExactlyInAnyOrder(
                COUNTER_ID, UNAVAILABLE_COUNTER_ID, UNAVAILABLE_ECOMMERCE_COUNTER_ID
            )
            it.assertThat(payload.counters).`as`("counters").containsExactlyInAnyOrder(
                createAvailableGdCounter(isEditableByOperator = false, isCalltrackingOnSiteCompatible = true),
                createUnavailableGdCounter(UNAVAILABLE_COUNTER_ID),
                createUnavailableGdCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID),
            )
            it.assertThat(payload.goals).`as`("goals").containsExactlyInAnyOrder(
                createGdGoal(GOAL_ID.toLong(), COUNTER_ID),
                createGdGoal(UNAVAILABLE_GOAL_ID, UNAVAILABLE_COUNTER_ID),
                createGdGoal(
                    ecommerceGoalId(UNAVAILABLE_ECOMMERCE_COUNTER_ID),
                    UNAVAILABLE_ECOMMERCE_COUNTER_ID, GdMetrikaCounterGoalType.ECOMMERCE
                ),
            )
        }
    }

    private fun createMetrikaCounterByDomain(counterId: Long, uid: Long) =
        MetrikaCounterByDomain().apply {
            this.counterId = counterId
            ownerUid = uid
            timestamp = LocalDateTime.now().minusDays(2).toEpochSecond(ZoneOffset.UTC)
        }

    private fun createAvailableGdCounter(
        isEditableByOperator: Boolean,
        isCalltrackingOnSiteCompatible: Boolean) =
        GdClientMetrikaCounterForSuggest().apply {
            id = COUNTER_ID
            this.isEditableByOperator = isEditableByOperator
            this.isCalltrackingOnSiteCompatible = isCalltrackingOnSiteCompatible
            isAccessible = true
            isAccessRequested = null
        }

    private fun createUnavailableGdCounter(counterId: Long) =
        GdClientMetrikaCounterForSuggest().apply {
            id = counterId
            isEditableByOperator = false
            isCalltrackingOnSiteCompatible = false
            isAccessible = false
            isAccessRequested = false
        }

    private fun createGdGoal(
        goalId: Long,
        counterId: Long,
        goalType: GdMetrikaCounterGoalType = GdMetrikaCounterGoalType.URL) =
        GdGoal().apply {
            id = goalId
            metrikaGoalType = goalType
            this.counterId = counterId.toInt()
        }
}
