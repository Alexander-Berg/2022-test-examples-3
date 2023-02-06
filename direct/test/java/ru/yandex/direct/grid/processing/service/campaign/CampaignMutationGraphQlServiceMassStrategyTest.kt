package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.model.campaign.GdCampaignAttributionModel
import ru.yandex.direct.grid.model.campaign.GdCampaignPlatform
import ru.yandex.direct.grid.model.campaign.GdCampaignType
import ru.yandex.direct.grid.model.campaign.GdDayBudgetShowMode
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignBiddingStrategy
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyData
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdCampaignStrategyName
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayloadItem
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignsStrategy
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.FunctionalUtils
import java.math.BigDecimal
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext as buildContext1

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignMutationGraphQlServiceMassStrategyTest {
    @Autowired
    private lateinit var testExecutor: GraphQlTestExecutor

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var operator: User
    private lateinit var clientInfo: ClientInfo
    private val counterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val goalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableCounterId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableGoalId = RandomNumberUtils.nextPositiveInteger().toLong()
    private val unavailableEcommerceCounterId = RandomNumberUtils.nextPositiveInteger().toLong()

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = UserHelper.getUser(clientInfo.client!!)
        TestAuthHelper.setDirectAuthentication(operator)

        metrikaClientStub.addCounterGoal(counterId.toInt(), goalId.toInt())
        metrikaClientStub.addUserCounter(clientInfo.uid, counterId.toInt())

        metrikaClientStub.addCounterGoal(unavailableCounterId.toInt(), unavailableGoalId.toInt())
        metrikaClientStub.addUnavailableCounter(unavailableCounterId)

        metrikaClientStub.addUnavailableEcommerceCounter(unavailableEcommerceCounterId)
    }

    @Test
    fun testMassSetStrategy_successSingle() {
        val campaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo), BID)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)

        val expectedPayloadItem = GdUpdateCampaignPayloadItem().withId(campaignInfo.id)
        val expectedPayload = GdUpdateCampaignPayload().withUpdatedCampaigns(listOf(expectedPayloadItem))
        val actualCampaign = getTextCampaign(campaignInfo)

        SoftAssertions.assertSoftly {
            it.assertThat(payload).`is`(matchedBy(beanDiffer(expectedPayload)))
            it.assertThat(actualCampaign.dayBudget).isEqualByComparingTo(BigDecimal.ZERO)
            it.assertThat(actualCampaign.dayBudgetShowMode).isEqualTo(DayBudgetShowMode.DEFAULT_)
            it.assertThat(actualCampaign.strategy.platform).isEqualTo(CampaignsPlatform.SEARCH)
            it.assertThat(actualCampaign.strategy.autobudget).isEqualTo(CampaignsAutobudget.YES)
            it.assertThat(actualCampaign.strategy.strategyName).isEqualTo(StrategyName.AUTOBUDGET)
            it.assertThat(actualCampaign.strategy.strategyData.sum).isEqualByComparingTo(SUM)
            it.assertThat(actualCampaign.strategy.strategyData.bid).isEqualByComparingTo(BID)
        }
    }

    @Test
    fun testMassSetDayBudget_successMulti() {
        val campaignInfo = createTextCampaign()
        val anotherCampaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo, anotherCampaignInfo), BID)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)

        val expectedPayloadItem = GdUpdateCampaignPayloadItem().withId(campaignInfo.id)
        val anotherExpectedPayloadItem = GdUpdateCampaignPayloadItem().withId(anotherCampaignInfo.id)
        val actualCampaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.shard,
            setOf(campaignInfo.id, anotherCampaignInfo.id)).filterIsInstance(TextCampaign::class.java)

        SoftAssertions.assertSoftly { soft ->
            soft.assertThat(payload.updatedCampaigns).`is`(matchedBy(containsInAnyOrder(expectedPayloadItem,
                anotherExpectedPayloadItem)))
            soft.assertThat(actualCampaigns).allMatch { it.dayBudget.compareTo(BigDecimal.ZERO) == 0 }
            soft.assertThat(actualCampaigns).allMatch { it.dayBudgetShowMode == DayBudgetShowMode.DEFAULT_ }
            soft.assertThat(actualCampaigns).allMatch { it.strategy.platform == CampaignsPlatform.SEARCH }
            soft.assertThat(actualCampaigns).allMatch { it.strategy.autobudget == CampaignsAutobudget.YES }
            soft.assertThat(actualCampaigns).allMatch { it.strategy.strategyName == StrategyName.AUTOBUDGET }
            soft.assertThat(actualCampaigns).allMatch { it.strategy.strategyData.bid.compareTo(BID) == 0 }
            soft.assertThat(actualCampaigns).allMatch { it.strategy.strategyData.sum.compareTo(SUM) == 0 }
        }
    }

    @Test
    fun testMassSetStrategyWithGoal_success() {
        val campaignInfo = createTextCampaign(counterId)
        val input = createRequest(listOf(campaignInfo), BID, goalId)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        val actualCampaign = getTextCampaign(campaignInfo)

        assertThat(payload.updatedCampaigns[0]).isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0].id).`as`("updatedCampaignId").isEqualTo(campaignInfo.id)
            it.assertThat(actualCampaign.strategy.strategyData.goalId).`as`("goalId").isEqualTo(goalId)
        }
    }

    @Test
    fun testMassSetStrategyWithGoal_error() {
        val campaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo), BID, goalId)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)

        assertGoalNotFound(payload)
    }

    @Test
    fun testMassSetStrategyWithGoalAndBindCounter_success() {
        val campaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo), BID, goalId, bindCounters = true)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        val actualCampaign = getTextCampaign(campaignInfo)

        assertThat(payload.updatedCampaigns[0]).isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0].id).`as`("updatedCampaignId").isEqualTo(campaignInfo.id)
            it.assertThat(actualCampaign.strategy.strategyData.goalId).`as`("goalId").isEqualTo(goalId)
            it.assertThat(actualCampaign.metrikaCounters).`as`("counterIds").isEqualTo(listOf(counterId))
        }
    }

    @Test
    fun testMassSetStrategyWithUnavailableGoal_success() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val campaignInfo = createTextCampaign(unavailableCounterId)
        val input = createRequest(listOf(campaignInfo), BID, unavailableGoalId)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        val actualCampaign = getTextCampaign(campaignInfo)

        assertThat(payload.updatedCampaigns[0]).isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0].id).`as`("updatedCampaignId").isEqualTo(campaignInfo.id)
            it.assertThat(actualCampaign.strategy.strategyData.goalId).`as`("goalId").isEqualTo(unavailableGoalId)
        }
    }

    @Test
    fun testMassSetStrategyWithUnavailableEcommerceGoal_success() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.COLD_START_FOR_ECOMMERCE_GOALS, true)

        val campaignInfo = createTextCampaign(unavailableEcommerceCounterId)
        val ecommerceGoalId = ecommerceGoalId(unavailableEcommerceCounterId)
        val input = createRequest(listOf(campaignInfo), BID, ecommerceGoalId)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        val actualCampaign = getTextCampaign(campaignInfo)

        assertThat(payload.updatedCampaigns[0]).isNotNull
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0].id).`as`("updatedCampaignId").isEqualTo(campaignInfo.id)
            it.assertThat(actualCampaign.strategy.strategyData.goalId).`as`("goalId").isEqualTo(ecommerceGoalId)
        }
    }

    @Test
    fun testMassSetStrategyWithUnavailableGoalAndBindCounter_successMulti() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val campaignInfo1 = createTextCampaign(counterId)
        val campaignInfo2 = createTextCampaign(unavailableCounterId)
        val input = createRequest(listOf(campaignInfo1, campaignInfo2), BID, unavailableGoalId, bindCounters = true)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0])
                .`as`("first campaign updated")
                .isNotNull
            it.assertThat(payload.updatedCampaigns[1])
                .`as`("second campaign updated")
                .isNotNull
        }

        val actualCampaign1 = getTextCampaign(campaignInfo1)
        val actualCampaign2 = getTextCampaign(campaignInfo2)
        SoftAssertions.assertSoftly {
            it.assertThat(payload.updatedCampaigns[0].id)
                .`as`("first updatedCampaignId")
                .isEqualTo(campaignInfo1.id)
            it.assertThat(actualCampaign1.strategy.strategyData.goalId)
                .`as`("goalId from first campaign")
                .isEqualTo(unavailableGoalId)
            it.assertThat(actualCampaign1.metrikaCounters)
                .`as`("counterIds from first campaign")
                .containsExactlyInAnyOrder(counterId, unavailableCounterId)

            it.assertThat(payload.updatedCampaigns[1].id)
                .`as`("second updatedCampaignId")
                .isEqualTo(campaignInfo2.id)
            it.assertThat(actualCampaign2.strategy.strategyData.goalId)
                .`as`("goalId from second campaign")
                .isEqualTo(unavailableGoalId)
        }
    }

    @Test
    fun testMassSetStrategyWithUnavailableGoalAndBindCounter_error() {
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED, true)
        val campaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo), BID, unavailableGoalId, bindCounters = true)
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)

        assertGoalNotFound(payload)
    }

    @Test
    fun testMassSetDayBudget_validationError() {
        val campaignInfo = createTextCampaign()
        val input = createRequest(listOf(campaignInfo), BID.add(BigDecimal.TEN))
        val payload = testExecutor.doMutationAndGetPayload(UPDATE_CAMPAIGNS_STRATEGY_MUTATION, input, operator)
        assertThat(payload.validationResult.errors).hasSize(1)
    }

    @Test
    fun testMassSetDayBudget_incorrectRequest() {
        val campaignInfo = CampaignInfo().withCampaign(
            Campaign().withId(-1L))
        val input = createRequest(listOf(campaignInfo), BID)
        val query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(input))
        val executionResult = processor.processQuery(null, query, null, buildContext1(operator))
        assertThat(executionResult.errors).hasSize(1)
    }

    private fun createTextCampaign(counterId: Long? = null): TextCampaignInfo {
        val textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
        textCampaign.apply {
            counterId?.let { metrikaCounters = listOf(it) }
        }
        return steps.textCampaignSteps().createCampaign(clientInfo, textCampaign)
    }

    private fun getTextCampaign(campaignInfo: TextCampaignInfo) =
        campaignTypedRepository.getTypedCampaigns(campaignInfo.shard, setOf(campaignInfo.id))[0] as TextCampaign

    private fun createRequest(
        infos: List<CampaignInfo>,
        bid: BigDecimal,
        goalId: Long? = null,
        bindCounters: Boolean? = null,
    ): GdUpdateCampaignsStrategy {
        return GdUpdateCampaignsStrategy()
            .withType(GdCampaignType.TEXT)
            .withCampaignIds(FunctionalUtils.mapList(infos) { obj: CampaignInfo -> obj.campaignId })
            .withBiddingStrategy(GdCampaignBiddingStrategy()
                .withPlatform(GdCampaignPlatform.SEARCH)
                .withStrategyName(GdCampaignStrategyName.AUTOBUDGET)
                .withStrategyData(GdCampaignStrategyData()
                    .withBid(bid)
                    .withSum(SUM)
                    .withGoalId(goalId)
                )
            )
            .withAttributionModel(GdCampaignAttributionModel.FIRST_CLICK)
            .withContextLimit(100)
            .withEnableCpcHold(false)
            .withDayBudget(BigDecimal.ZERO)
            .withDayBudgetShowMode(GdDayBudgetShowMode.DEFAULT_)
            .withBindCounters(bindCounters)
    }

    private fun assertGoalNotFound(payload: GdUpdateCampaignPayload) {
        val expectedValidationResult = GdValidationResult()
            .withErrors(listOf(GdDefect()
                .withCode("DefectIds.OBJECT_NOT_FOUND")
                .withParams(null)
                .withPath("campaignUpdateItems[0].biddingStategy.strategyData.goalId")
            ))
        assertThat(payload.updatedCampaigns[0]).isNull()
        assertThat(payload.validationResult).`is`(matchedBy(beanDiffer(expectedValidationResult)))
    }

    companion object {
        private val SUM = Currencies.getCurrency(CurrencyCode.RUB).maxAutobudget
        private val BID = Currencies.getCurrency(CurrencyCode.RUB).maxAutobudgetBid
        private const val MUTATION_NAME = CampaignMutationGraphQlService.UPDATE_CAMPAIGNS_STRATEGY
        private const val MUTATION_TEMPLATE = """
            mutation {
              %s (input: %s) {
                validationResult {
                  errors {
                    code
                    path
                    params
                  }
                }
                updatedCampaigns {
                  id
                }
              }
            }"""

        private val UPDATE_CAMPAIGNS_STRATEGY_MUTATION = TemplateMutation(MUTATION_NAME, MUTATION_TEMPLATE,
            GdUpdateCampaignsStrategy::class.java, GdUpdateCampaignPayload::class.java)
    }
}
