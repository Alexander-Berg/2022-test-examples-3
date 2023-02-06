package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.TURBODIRECT
import ru.yandex.direct.core.entity.metrika.model.MetrikaCounterByDomain
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.entity.metrikacounter.model.MetrikaCounterPermission
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.client.GdClientMetrikaCounterForSuggest
import ru.yandex.direct.grid.processing.model.client.GdSuggestMetrikaDataByUrlPayload
import ru.yandex.direct.grid.processing.model.goal.GdGoal
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaCounterGoalType
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.RandomNumberUtils
import java.time.LocalDateTime
import java.time.ZoneOffset

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientGraphQlServiceSuggestMetrikaDataTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor
    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository

    private var chiefUid: Long = -1
    private var anotherChiefUid: Long = -1

    @Before
    fun before() {
        val userInfo = steps.userSteps().createDefaultUser()
        val anotherUserInfo = steps.userSteps().createDefaultUser()
        val user = userInfo.user!!
        chiefUid = user.chiefUid
        anotherChiefUid = anotherUserInfo.chiefUid

        TestAuthHelper.setDirectAuthentication(user)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(user)

        metrikaClientStub.addUserCounter(
            chiefUid,
            MetrikaClientStub.buildCounter(counterId.toInt(), "system", MetrikaCounterPermission.EDIT)
        )
        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId)

        metrikaClientStub.addUnavailableCounter(allowedUnavailableCounterId, true)
        metrikaClientStub.addCounterGoal(allowedUnavailableCounterId.toInt(), unavailableGoalId)
        metrikaClientStub.addUnavailableCounter(unAllowedUnavailableCounterId, false)
        metrikaClientStub.addCounterGoal(unAllowedUnavailableCounterId.toInt(),
            RandomNumberUtils.nextPositiveInteger())
        metrikaClientStub.addUnavailableEcommerceCounter(unavailableEcommerceCounterId)
        metrikaClientStub.addUnavailableCounter(unavailableTurboCounterId, TURBODIRECT)

        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED, true)
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)
    }

    @After
    fun after() {
        Mockito.reset(metrikaCounterByDomainRepository)
        metrikaClientStub.clearUnavailableCounters()
    }

    @Test
    fun suggestMetrikaDataByUrl() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf(
                createMetrikaCounterByDomain(counterId, chiefUid),
                createMetrikaCounterByDomain(allowedUnavailableCounterId, anotherChiefUid),
                createMetrikaCounterByDomain(unAllowedUnavailableCounterId, anotherChiefUid),
            ))

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru")
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(
                counterId, allowedUnavailableCounterId
            )
            counters = setOf(
                gdCounter, gdAllowedUnavailableCounter
            )
            goals = setOf(
                gdGoal, gdUnavailableGoal
            )
        })
    }

    @Test
    fun suggestMetrikaDataByUrl_Ecommerce() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf(createMetrikaCounterByDomain(unavailableEcommerceCounterId, anotherChiefUid)))

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru")
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(unavailableEcommerceCounterId)
            counters = setOf(createUnavailableGdCounter(unavailableEcommerceCounterId))
            goals = setOf(createGdGoal(
                ecommerceGoalId(unavailableEcommerceCounterId),
                unavailableEcommerceCounterId, GdMetrikaCounterGoalType.ECOMMERCE)
            )
        })
    }

    @Test
    fun suggestMetrikaDataByAdditionalCounters() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf())

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru",
            setOf(counterId, allowedUnavailableCounterId, unAllowedUnavailableCounterId))
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(
                counterId, allowedUnavailableCounterId, unAllowedUnavailableCounterId
            )
            counters = setOf(
                gdCounter, gdAllowedUnavailableCounter, gdUnAllowedUnavailableCounter
            )
            goals = setOf(
                gdGoal, gdUnavailableGoal
            )
        })
    }

    @Test
    fun suggestMetrikaDataByAdditionalCounters_Ecommerce() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf())

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru",
            setOf(unavailableEcommerceCounterId))
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(unavailableEcommerceCounterId)
            counters = setOf(createUnavailableGdCounter(unavailableEcommerceCounterId))
            goals = setOf(createGdGoal(
                ecommerceGoalId(unavailableEcommerceCounterId),
                unavailableEcommerceCounterId, GdMetrikaCounterGoalType.ECOMMERCE)
            )
        })
    }

    @Test
    fun suggestMetrikaData_AvailableCounterWithSource() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf(
                createMetrikaCounterByDomain(counterId, chiefUid),
            ))

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru", setOf(counterId))
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(counterId)
            counters = setOf(gdCounter)
            goals = setOf(gdGoal)
        })
    }

    @Test
    fun suggestMetrikaData_UnavailableCounterWithSource() {
        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(any(), any()))
            .thenReturn(listOf(
                createMetrikaCounterByDomain(unavailableTurboCounterId, anotherChiefUid)
            ))

        val result = ktGraphQLTestExecutor.suggestMetrikaDataByUrl("http://url.ru", setOf(unavailableTurboCounterId))
        assertThat(result).isEqualTo(GdSuggestMetrikaDataByUrlPayload().apply {
            counterIds = setOf(unavailableTurboCounterId)
            counters = setOf(createUnavailableGdCounter(unavailableTurboCounterId))
            goals = setOf()
        })
    }

    private fun createMetrikaCounterByDomain(counterId: Long, ownerUid: Long) =
        MetrikaCounterByDomain().apply {
            this.counterId = counterId
            this.ownerUid = ownerUid
            timestamp = LocalDateTime.now().minusDays(2).toEpochSecond(ZoneOffset.UTC)
        }

    companion object {
        private val counterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val allowedUnavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val unAllowedUnavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val unavailableEcommerceCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val unavailableTurboCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
        private val goalId = RandomNumberUtils.nextPositiveInteger()
        private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger()

        private val gdCounter = GdClientMetrikaCounterForSuggest().apply {
            id = counterId
            isEditableByOperator = true
            isCalltrackingOnSiteCompatible = false
            isAccessible = true
            isAccessRequested = null
        }
        private val gdAllowedUnavailableCounter = createUnavailableGdCounter(allowedUnavailableCounterId)
        private val gdUnAllowedUnavailableCounter = createUnavailableGdCounter(unAllowedUnavailableCounterId)

        private val gdGoal = createGdGoal(goalId.toLong(), counterId)
        private val gdUnavailableGoal = createGdGoal(unavailableGoalId.toLong(), allowedUnavailableCounterId)
    }
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
        id = goalId.toLong()
        metrikaGoalType = goalType
        this.counterId = counterId.toInt()
    }
