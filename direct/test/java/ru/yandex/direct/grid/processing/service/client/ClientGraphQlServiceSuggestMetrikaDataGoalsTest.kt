package ru.yandex.direct.grid.processing.service.client

import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import graphql.ExecutionResult
import java.time.LocalDateTime
import java.time.ZoneOffset
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_SUFFICIENT_GOAL_CONVERSION_COUNT
import ru.yandex.direct.core.entity.metrika.model.MetrikaCounterByDomain
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.ConversionGrade
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo
import ru.yandex.direct.test.utils.RandomNumberUtils

private val QUERY_TEMPLATE = """
        {
          suggestMetrikaDataByUrl(input: {url: "%s"}){
            goals {
              id
              conversionVisitsCount
              conversionGrade
            }
          }
        }
    """.trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class ClientGraphQlServiceSuggestMetrikaDataGoalsTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var metriGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var context: GridGraphQLContext
    private lateinit var campaignInfo: CampaignInfo

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!

        TestAuthHelper.setDirectAuthentication(operator)

        context = GridGraphQLContext(operator)
        gridContextProvider.gridContext = context

        campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(CampaignType.TEXT, clientInfo)
    }

    @After
    fun after() {
        reset(metrikaCounterByDomainRepository, metrikaGoalsService, metrikaClientStub)
        metrikaClientStub.clearUnavailableCounters()
    }

    fun parametrizedTestData() = listOf(
        listOf("Without conversion data", true, null, ConversionGrade.LOW_CONVERSION),
        listOf("Without conversions", true, 0L, ConversionGrade.LOW_CONVERSION),
        listOf("Low count conversions", true, MIN_SUFFICIENT_GOAL_CONVERSION_COUNT - 1, ConversionGrade.LOW_CONVERSION),
        listOf("Enough count conversions", true, MIN_SUFFICIENT_GOAL_CONVERSION_COUNT, ConversionGrade.ENOUGH_CONVERSIONS),
        listOf("Without conversion data and without feature", false, null, ConversionGrade.LOW_CONVERSION),
        listOf("Without conversions and without feature", false, 0L, ConversionGrade.LOW_CONVERSION),
        listOf("Low count conversions and without feature", false, MIN_SUFFICIENT_GOAL_CONVERSION_COUNT - 1, ConversionGrade.LOW_CONVERSION),
        listOf("Enough count conversions and without feature", false, MIN_SUFFICIENT_GOAL_CONVERSION_COUNT, ConversionGrade.ENOUGH_CONVERSIONS),
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun test(
        @Suppress("UNUSED_PARAMETER") description: String,
        withFeature: Boolean,
        visitsCount: Long?,
        conversionGrade: ConversionGrade,
    ) {
        steps.featureSteps().addClientFeature(clientId, UAC_UNAVAILABLE_GOALS_ALLOWED, withFeature)

        val goal = createGoal(visitsCount)
        val unavailableGoal = createGoal(visitsCount)

        initAndMockData(goal, unavailableGoal, visitsCount)

        val result = executeRequest()
        val data = result.getData<Map<Any, Any?>>()

        val soft = SoftAssertions()
        soft.assertThat(data)
            .isNotEmpty

        val goals = GraphQLUtils.getDataValue<List<Map<String, Any?>>>(data, "suggestMetrikaDataByUrl/goals")
        soft.assertThat(goals)
            .`as`("Количество целей")
            .hasSize(if (withFeature) 2 else 1)

        val goalById = mutableMapOf<Any?, Any>()
        goalById[goals[0]["id"]] = goals[0]
        if (withFeature) {
            goalById[goals[1]["id"]] = goals[1]
        }

        val expectGoal = getExpectedGoalData(goal, conversionGrade, visitsCount)
        soft.assertThat(goalById)
            .`as`("Доступная цель")
            .containsEntry(goal.id, expectGoal)
        if (withFeature) {
            val expectUnavailableGoal = getExpectedGoalData(unavailableGoal, conversionGrade, null)
            soft.assertThat(goalById)
                .`as`("Недоступная цель")
                .containsEntry(unavailableGoal.id, expectUnavailableGoal)
        }
        soft.assertAll()
    }

    private fun getExpectedGoalData(
        goal: Goal,
        conversionGrade: ConversionGrade,
        visitsCount: Long?
    ) = mapOf(
        "id" to goal.id,
        "conversionGrade" to conversionGrade.name,
        "conversionVisitsCount" to visitsCount,
    )

    private fun initAndMockData(
        goal: Goal,
        unavailableGoal: Goal,
        visitsCount: Long?,
    ) {
        metrikaClientStub.addUnavailableCounter(unavailableGoal.counterId.toLong())

        whenever(metrikaCounterByDomainRepository.getRestrictedCountersByDomain(anyString(), anyBoolean()))
            .thenReturn(listOf(
                MetrikaCounterByDomain().apply {
                    counterId = goal.counterId.toLong()
                    ownerUid = operator.uid
                    timestamp = LocalDateTime.now().minusDays(2).toEpochSecond(ZoneOffset.UTC)
                },
                MetrikaCounterByDomain().apply {
                    counterId = unavailableGoal.counterId.toLong()
                    ownerUid = operator.uid
                    timestamp = LocalDateTime.now().minusDays(2).toEpochSecond(ZoneOffset.UTC)
                }))

        whenever(metrikaClientStub.getGoalsConversionInfoByCounterIds(anyCollection(), anyInt()))
            .thenReturn(mapOf(
                goal.id to GoalConversionInfo(goal.id, visitsCount, null),
                unavailableGoal.id to GoalConversionInfo(unavailableGoal.id, visitsCount, null),
            ))

        metrikaClientStub.addUserCounter(operator.uid, goal.counterId)
        metrikaClientStub.addCounterGoal(goal.counterId, goal.id.toInt())
        metrikaClientStub.addCounterGoal(unavailableGoal.counterId, unavailableGoal.id.toInt())
    }

    private fun createGoal(visitsCount: Long?) = Goal()
        .withId(Goal.METRIKA_SEGMENT_UPPER_BOUND + RandomNumberUtils.nextPositiveLong(300_000_000L))
        .withCounterId(RandomNumberUtils.nextPositiveInteger())
        .withConversionVisitsCount(visitsCount)
        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL) as Goal

    private fun executeRequest(): ExecutionResult {
        val query = String.format(QUERY_TEMPLATE, "https://ormatek.com")
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()
        return result
    }
}
