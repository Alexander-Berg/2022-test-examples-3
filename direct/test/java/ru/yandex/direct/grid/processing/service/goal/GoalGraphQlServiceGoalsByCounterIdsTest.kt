package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.doReturn
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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_SUFFICIENT_GOAL_CONVERSION_COUNT
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.ConversionGrade
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo

private val QUERY_TEMPLATE = """
        query {
          client(searchBy: { login: "%s" }) {
            goalsByCounterIds(input: %s) {
                counterWithGoals {
                    goals {
                        id
                        counterId
              			conversionGrade
                      	conversionVisitsCount
                    }
                }
            }
        }
        }
    """.trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class GoalGraphQlServiceGoalsByCounterIdsTest {
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
        reset(metrikaCounterByDomainRepository, metrikaClientStub, metrikaGoalsService)
    }

    fun parametrizedTestData() = listOf(
        listOf("Without conversion data", null, ConversionGrade.LOW_CONVERSION),
        listOf("Without conversions", 0L, ConversionGrade.LOW_CONVERSION),
        listOf("Low count conversions", MIN_SUFFICIENT_GOAL_CONVERSION_COUNT - 1, ConversionGrade.LOW_CONVERSION),
        listOf("Enough count conversions", MIN_SUFFICIENT_GOAL_CONVERSION_COUNT, ConversionGrade.ENOUGH_CONVERSIONS),
    )

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun test(
        @Suppress("UNUSED_PARAMETER") description: String,
        visitsCount: Long?,
        conversionGrade: ConversionGrade,
    ) {
        val goal1 = createGoal(visitsCount, 1279)
        val goal2 = createGoal(visitsCount, 1351)

        initAndMockData(goal1, goal2)
        val counterIds = listOf(goal1.counterId, goal2.counterId)

        val result = executeRequest(counterIds)
        val data = result.getData<Map<Any, Any?>>()

        val soft = SoftAssertions()
        soft.assertThat(data)
            .isNotEmpty

        val counterWithGoals = GraphQLUtils.getDataValue<List<Map<String, List<Map<String, Any?>>>>>(
            data, "client/goalsByCounterIds/counterWithGoals")
        val goals = counterWithGoals
            .map { it.values }
            .flatten()
            .flatten()
            .toList()
        soft.assertThat(goals)
            .`as`("Количество целей")
            .hasSize(2)

        val goalById = goals
            .associateBy { it["id"] }

        val expectGoal = getExpectedGoalData(goal1, conversionGrade, visitsCount)
        soft.assertThat(goalById)
            .`as`("Цель 1")
            .containsEntry(goal1.id, expectGoal)
        val expectUnavailableGoal = getExpectedGoalData(goal2, conversionGrade, visitsCount)
        soft.assertThat(goalById)
            .`as`("Цель 2")
            .containsEntry(goal2.id, expectUnavailableGoal)
        soft.assertAll()
    }

    private fun getExpectedGoalData(
        goal: Goal,
        conversionGrade: ConversionGrade,
        visitsCount: Long?
    ) = mapOf(
        "id" to goal.id,
        "counterId" to goal.counterId,
        "conversionGrade" to conversionGrade.name,
        "conversionVisitsCount" to visitsCount,
    )

    private fun initAndMockData(
        goal1: Goal,
        goal2: Goal,
    ) {
        doReturn(
            mapOf(
                goal1.id to GoalConversionInfo(goal1.id, goal1.conversionVisitsCount, null),
                goal2.id to GoalConversionInfo(goal2.id, goal2.conversionVisitsCount, null),
            )
        ).`when`(metrikaClientStub).getGoalsConversionInfoByCounterIds(anyCollection(), anyInt())

        doReturn(
            mapOf(
                goal1.counterId.toLong() to setOf(goal1),
                goal2.counterId.toLong() to setOf(goal2)
            )
        ).`when`(metrikaGoalsService).getMetrikaGoalsByCounterIds(any(), any())


        metrikaClientStub.addUserCounter(operator.uid, goal1.counterId)
        metrikaClientStub.addUserCounter(operator.uid, goal2.counterId)
        metrikaClientStub.addCounterGoal(goal1.counterId, goal1.id.toInt())
        metrikaClientStub.addCounterGoal(goal2.counterId, goal2.id.toInt())
    }

    private fun createGoal(visitsCount: Long?, counterId: Int) = Goal()
        .withId(counterId.toLong())
        .withCounterId(counterId)
        .withConversionVisitsCount(visitsCount)
        .withMetrikaCounterGoalType(MetrikaCounterGoalType.EMAIL) as Goal

    private fun executeRequest(counterIds: List<Int>): ExecutionResult {
        val query = String.format(QUERY_TEMPLATE, context.operator.login, counterIds)
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()
        return result
    }
}
