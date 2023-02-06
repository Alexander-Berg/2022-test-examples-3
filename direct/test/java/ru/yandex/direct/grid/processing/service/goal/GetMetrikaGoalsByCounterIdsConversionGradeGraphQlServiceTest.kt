package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.reset
import graphql.ExecutionResult
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_SUFFICIENT_GOAL_CONVERSION_COUNT
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED
import ru.yandex.direct.grid.model.campaign.GdCampaignType
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.ConversionGrade
import ru.yandex.direct.grid.processing.model.goal.mutation.GdMetrikaGoalsByCounter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.RandomNumberUtils

private val QUERY_TEMPLATE = """
        query {
          getMetrikaGoalsByCounter(input: %s) {
            goals {
                id
                conversionGrade
                conversionVisitsCount
            }
          }
        }
    """.trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class GetMetrikaGoalsByCounterIdsConversionGradeGraphQlServiceTest {

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
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    private var shard = 0
    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var context: GridGraphQLContext
    private lateinit var campaignInfo: CampaignInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        TestAuthHelper.setDirectAuthentication(operator)

        context = ContextHelper.buildContext(operator)
            .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context

        campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(CampaignType.TEXT, clientInfo)
    }


    @After
    fun after() {
        reset(metrikaGoalsService)
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
        steps.featureSteps().addClientFeature(clientInfo.clientId, DIRECT_UNAVAILABLE_GOALS_ALLOWED, withFeature)

        val goal1 = createGoal(visitsCount)
        val goal2 = createGoal(visitsCount)
            .withCounterId(0) as Goal
        val unavailableGoal = createGoal(visitsCount)
        val mobileGoal = createMobileGoal(visitsCount)

        metrikaClientStub.addUserCounter(clientInfo.uid, goal1.counterId)
        metrikaClientStub.addUserCounter(clientInfo.uid, goal2.counterIds[0])
        steps.metrikaServiceSteps().initTestDataForGoalsSuggestion(setOf(goal1, goal2, unavailableGoal, mobileGoal))

        val input = GdMetrikaGoalsByCounter()
            .withCampaignId(campaignInfo.campaignId)
            .withCampaignType(GdCampaignType.TEXT)
            .withCounterIds(listOf(
                goal1.counterId.toLong(), goal2.counterIds[0].toLong(), unavailableGoal.counterId.toLong()))
        val result = executeRequest(input)
        val data = result.getData<Map<Any, Any?>>()

        val expectGoalData: List<Map<String, Any?>> = listOf(
            getExpectedGoalData(goal1, conversionGrade, visitsCount),
            getExpectedGoalData(goal2, conversionGrade, visitsCount),
            getExpectedGoalData(unavailableGoal, conversionGrade, if (withFeature) null else visitsCount),
            getExpectedGoalData(mobileGoal, conversionGrade, visitsCount),
        )

        SoftAssertions.assertSoftly { softly ->
            softly.assertThat(data)
                .isNotEmpty
            val goals = GraphQLUtils.getDataValue<List<Map<String, Any?>>>(data, "getMetrikaGoalsByCounter/goals")
            softly.assertThat(goals)
                .`as`("Количество целей")
                .hasSize(expectGoalData.size)

            val goalById = setOf(goals[0], goals[1], goals[2], goals[3])
                .associateBy { it["id"] }
            softly.assertThat(goalById)
                .`as`("Доступная цель 1")
                .containsEntry(expectGoalData[0]["id"], expectGoalData[0])
            softly.assertThat(goalById)
                .`as`("Доступная цель 2")
                .containsEntry(expectGoalData[1]["id"], expectGoalData[1])
            softly.assertThat(goalById)
                .`as`("Недоступная цель")
                .containsEntry(expectGoalData[2]["id"], expectGoalData[2])
            softly.assertThat(goalById)
                .`as`("Мобильная цель")
                .containsEntry(expectGoalData[3]["id"], expectGoalData[3])
        }
    }

    private fun getExpectedGoalData(goal: Goal,
                                    conversionGrade: ConversionGrade,
                                    visitsCount: Long?
    ) = mapOf(
        "id" to goal.id,
        "conversionGrade" to conversionGrade.name,
        "conversionVisitsCount" to visitsCount,
    )

    private fun createGoal(visitsCount: Long?) = Goal()
        .withId(1L + RandomNumberUtils.nextPositiveLong(Goal.METRIKA_GOAL_UPPER_BOUND))
        .withCounterId(RandomNumberUtils.nextPositiveInteger())
        .withCounterIds(listOf(RandomNumberUtils.nextPositiveInteger()))
        .withConversionVisitsCount(visitsCount)
        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL) as Goal

    private fun createMobileGoal(visitsCount: Long?) = Goal()
        .withId(Goal.LAL_SEGMENT_UPPER_BOUND + RandomNumberUtils.nextPositiveLong(10_000_000))
        .withConversionVisitsCount(visitsCount) as Goal

    private fun executeRequest(input: GdMetrikaGoalsByCounter): ExecutionResult {
        val query = String.format(QUERY_TEMPLATE, GraphQlJsonUtils.graphQlSerialize(input))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()
        return result
    }
}
