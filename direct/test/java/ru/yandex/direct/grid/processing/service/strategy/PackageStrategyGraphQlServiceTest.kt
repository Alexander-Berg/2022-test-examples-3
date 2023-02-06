package ru.yandex.direct.grid.processing.service.strategy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CPI_GOAL_ID
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.model.campaign.GdCampaignTruncated
import ru.yandex.direct.grid.model.strategy.GdStrategyName
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategiesContainer
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyFilter
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyOrderBy
import ru.yandex.direct.grid.processing.model.strategy.query.GdPackageStrategyOrderByField
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.campaign.CampaignInfoService
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.counterId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.goalId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.meaningfulGoalId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.objectMapper
import ru.yandex.direct.grid.processing.service.strategy.query.GdPackageConverter
import ru.yandex.direct.grid.processing.service.strategy.query.PackageStrategyService
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import ru.yandex.direct.grid.processing.util.StatHelper.calcTotalStats
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class PackageStrategyGraphQlServiceTest : StrategyAddOperationTestBase(), AbstractPackageStrategyGraphQlServiceTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var campaignInfoService: CampaignInfoService

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var packageStrategyService: PackageStrategyService

    private lateinit var userInfo: UserInfo

    override fun getShard() = userInfo.shard

    override fun getClientId(): ClientId = userInfo.clientId

    override fun getOperatorUid(): Long = userInfo.uid

    private val queryTemplate = "" +
        "{\n" +
        "client(searchBy:{login:\"%s\"}) {\n" +
        "    strategies(input: %s) {\n" +
        "      totalCount\n" +
        "      totalStats {\n" +
        "           avgClickCost\n" +
        "           avgClickPosition\n" +
        "           avgDepth\n" +
        "           avgGoalCost\n" +
        "           avgShowPosition\n" +
        "           bounceRate\n" +
        "           clicks\n" +
        "           conversionRate\n" +
        "           cost\n" +
        "           costWithTax\n" +
        "           cpmPrice\n" +
        "           crr\n" +
        "           ctr\n" +
        "           day\n" +
        "           goals\n" +
        "           profitability\n" +
        "           revenue\n" +
        "           shows\n" +
        "      }\n" +
        "      totalGoalStats {\n" +
        "          goalId\n" +
        "          goals\n" +
        "          conversionRate\n" +
        "      }\n" +
        "      filter {\n" +
        "        strategyIdIn\n" +
        "        strategyIdNotIn\n" +
        "        strategyIdContainsAny\n" +
        "        strategyTypeIn\n" +
        "        strategyTypeNotIn\n" +
        "        isPublic\n" +
        "        isArchived\n" +
        "        goalStats {\n" +
        "           goalId\n" +
        "        }\n" +
        "        stat {\n" +
        "           minCost\n" +
        "        }\n" +
        "      }\n" +
        "      rowset {\n" +
        "        __typename\n" +
        "        name\n" +
        "        id\n" +
        "        isPublic\n" +
        "        walletId\n" +
        "        cids\n" +
        "        linkedCampaigns {\n" +
        "           id\n" +
        "           name\n" +
        "        }\n" +
        "        clientId\n" +
        "        attributionModel\n" +
        "        statusArchived\n" +
        "        type\n" +
        "        lastChange\n" +
        "        stats {\n" +
        "           avgClickCost\n" +
        "           avgClickPosition\n" +
        "           avgDepth\n" +
        "           avgGoalCost\n" +
        "           avgShowPosition\n" +
        "           bounceRate\n" +
        "           clicks\n" +
        "           conversionRate\n" +
        "           cost\n" +
        "           costWithTax\n" +
        "           cpmPrice\n" +
        "           crr\n" +
        "           ctr\n" +
        "           day\n" +
        "           goals\n" +
        "           profitability\n" +
        "           revenue\n" +
        "           shows\n" +
        "        }\n" +
        "        goalStats {\n" +
        "          goalId\n" +
        "          goals\n" +
        "          conversionRate\n" +
        "        }\n" +
        "        ... on GdAutobudgetRoi {\n" +
        "          reserveReturn\n" +
        "          roiCoef\n" +
        "          profitability\n" +
        "          metrikaCounters\n" +
        "          bid\n" +
        "          goalId\n" +
        "          sum\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "        }\n" +
        "        ... on GdAutobudgetWeekSum {\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "          lastBidderRestartTime\n" +
        "          goalId\n" +
        "          bid\n" +
        "          sum\n" +
        "          \n" +
        "        }\n" +
        "        ... on GdDefaultManualStrategy {\n" +
        "          dayBudget\n" +
        "          dayBudgetShowMode\n" +
        "          enableCpcHold\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdCpmDefault {\n" +
        "          dayBudget\n" +
        "          dayBudgetShowMode\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpvCustomPeriod {\n" +
        "          avgCpv\n" +
        "          budget\n" +
        "          metrikaCounters\n" +
        "          autoProlongation\n" +
        "          finish\n" +
        "          start\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpv {\n" +
        "          avgCpv\n" +
        "          metrikaCounters\n" +
        "          sum\n" +
        "        }\n" +
        "        ... on GdAutobudgetCrr {\n" +
        "          crr\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          isPayForConversionEnabled\n" +
        "          metrikaCounters\n" +
        "          lastBidderRestartTime\n" +
        "          goalId\n" +
        "          sum\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpaPerCamp {\n" +
        "          avgCpa\n" +
        "          goalId\n" +
        "          isPayForConversionEnabled\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "          lastBidderRestartTime\n" +
        "          sum\n" +
        "          bid\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpaPerFilter {\n" +
        "          goalId\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "          lastBidderRestartTime\n" +
        "          sum\n" +
        "          bid\n" +
        "          filterAvgCpa\n" +
        "          isPayForConversionEnabled\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpa {\n" +
        "          avgCpa\n" +
        "          goalId\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "          lastBidderRestartTime\n" +
        "          isPayForConversionEnabled\n" +
        "          sum\n" +
        "          bid\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpi {\n" +
        "          avgCpi\n" +
        "          isPayForConversionEnabled\n" +
        "          lastBidderRestartTime\n" +
        "          goalId\n" +
        "          sum\n" +
        "          bid\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgClick {\n" +
        "          avgBid\n" +
        "          sum\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpcPerCamp {\n" +
        "          avgBid\n" +
        "          sum\n" +
        "          metrikaCounters\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          bid\n" +
        "        }\n" +
        "        ... on GdAutobudgetAvgCpcPerFilter {\n" +
        "          metrikaCounters\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          filterAvgBid\n" +
        "          bid\n" +
        "          sum\n" +
        "        }\n" +
        "        ... on GdAutobudgetMaxImpressions {\n" +
        "          metrikaCounters\n" +
        "          sum\n" +
        "          avgCpm\n" +
        "        }\n" +
        "        ... on GdAutobudgetMaxImpressionsCustomPeriod {\n" +
        "          budget\n" +
        "          start\n" +
        "          finish\n" +
        "          avgCpm\n" +
        "          autoProlongation\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdAutobudgetMaxReach {\n" +
        "          avgCpm\n" +
        "          metrikaCounters\n" +
        "          sum\n" +
        "        }\n" +
        "        ... on GdAutobudgetMaxReachCustomPeriod {\n" +
        "          budget\n" +
        "          start\n" +
        "          finish\n" +
        "          avgCpm\n" +
        "          autoProlongation\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdAutobudgetMedia {\n" +
        "          date\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdAutobudgetWeekBundle {\n" +
        "          limitClicks\n" +
        "          bid\n" +
        "          avgBid\n" +
        "          meaningfulGoals {\n" +
        "            conversionValue\n" +
        "            goalId\n" +
        "            isMetrikaSourceOfValue\n" +
        "          }\n" +
        "          metrikaCounters\n" +
        "        }\n" +
        "        ... on GdPeriodFixBid {\n" +
        "          budget\n" +
        "          metrikaCounters\n" +
        "          autoProlongation\n" +
        "          finish\n" +
        "          start\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}"

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    private lateinit var context: GridGraphQLContext

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()
        walletService.createWalletForNewClient(userInfo.clientId, userInfo.uid)
        context = ContextHelper.buildContext(userInfo.user)
        gridContextProvider.gridContext = context
        stubGoals(counterId, listOf(goalId, meaningfulGoalId))
    }

    fun testStrategies(): List<List<Any>> = listOf(
        listOf("AutobudgetCrr", autobudgetCrr()),
        listOf("AutobudgetAvgClick", autobudgetAvgClick()),
        listOf("AutobudgetAvgCpa", autobudgetAvgCpa()),
        listOf("AutobudgetAvgCpaPerCamp", autobudgetAvgCpaPerCamp()),
        listOf("AutobudgetAvgCpaPerFilter", autobudgetAvgCpaPerFilter()),
        listOf("AutobudgetAvgCpcPerCamp", autobudgetAvgCpcPerCamp()),
        listOf("AutobudgetAvgCpcPerFilter", autobudgetAvgCpcPerFilter()),
        listOf("AutobudgetAvgCpi", autobudgetAvgCpi()),
        listOf("AutobudgetAvgCpv", autobudgetAvgCpv()),
        listOf("AutobudgetAvgCpvCustomPeriod", clientAutobudgetAvgCpvCustomPeriodStrategy()),
        listOf("AutobudgetMaxImpressions", clientAutobudgetMaxImpressions()),
        listOf("AutobudgetMaxImpressionsCustomPeriod", clientAutobudgetMaxImpressionsCustomPeriodStrategy()),
        listOf("AutobudgetMaxReach", clientAutobudgetReachStrategy()),
        listOf("AutobudgetMaxReachCustomPeriod", clientAutobudgetMaxReachCustomPeriodStrategy()),
        listOf("AutobudgetRoi", autobudgetRoi()),
        listOf("AutobudgetWeekBundle", autobudgetWeekBundle()),
        listOf("AutobudgetWeekSum", autobudget()),
        listOf("CpmDefault", clientCpmDefaultStrategy()),
        listOf("DefaultManualStrategy", clientDefaultManualStrategy()),
        listOf("PeriodFixBid", clientPeriodFixBidStrategy())
    )

    fun testArchiveData(): List<List<*>> = listOf(
        true,
        false,
        null
    ).map { listOf(it) }

    @Test
    @Parameters(method = "testStrategies")
    @TestCaseName("{0}")
    fun `list all type of strategies`(
        description: String,
        strategy: CommonStrategy
    ) {
        prepareAndApplyValid(listOf(strategy))
        val container = defaultContainer()

        val response = execute(container)
        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!

        if (actualStrategy is AutobudgetAvgCpi) {
            actualStrategy.goalId = DEFAULT_CPI_GOAL_ID
        }

        val expectedResponse = expectedResponse(listOf(actualStrategy), container.filter)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by strategyIdIn`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withFilter(GdPackageStrategyFilter().withStrategyIdIn(setOf(crrStrategy.id)))

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(crrStrategy.id),
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    @Parameters(method = "testArchiveData")
    @TestCaseName("isArchived={0}")
    fun `test filtration by statusArchived`(isArchived: Boolean?) {
        val crrStrategy = autobudgetCrr()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, defaultStrategy))
        archiveUnarchive(defaultStrategy.id, archive = true)

        val container = defaultContainer()
            .withFilter(
                GdPackageStrategyFilter()
                    .withIsArchived(isArchived)
            )

        val expectedStrategyIds = if (isArchived == null) {
            listOf(crrStrategy.id, defaultStrategy.id)
        } else if (isArchived) {
            listOf(defaultStrategy.id)
        } else {
            listOf(crrStrategy.id)
        }

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            expectedStrategyIds,
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by strategyIdContainsAny`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val pattern = crrStrategy.id.toString()
        val container = defaultContainer()
            .withFilter(GdPackageStrategyFilter().withStrategyIdContainsAny(setOf(pattern)))

        val filtered = listOf<CommonStrategy>(crrStrategy, roiStrategy, defaultStrategy)
            .filter { it.id.toString().contains(pattern) }

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            filtered.map { it.id },
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by strategyTypeIn`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withFilter(
                GdPackageStrategyFilter()
                    .withStrategyTypeIn(setOf(GdStrategyName.AUTOBUDGET_CRR))
            )

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(crrStrategy.id),
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by strategyTypeNotIn`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withFilter(
                GdPackageStrategyFilter()
                    .withStrategyTypeNotIn(setOf(GdStrategyName.AUTOBUDGET_CRR))
            )

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(defaultStrategy.id, roiStrategy.id),
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by isPublic`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withFilter(
                GdPackageStrategyFilter()
                    .withIsPublic(false)
            )

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(defaultStrategy.id, crrStrategy.id, roiStrategy.id),
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test filtration by strategyIdNotIn`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withFilter(GdPackageStrategyFilter().withStrategyIdNotIn(setOf(defaultStrategy.id)))

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(crrStrategy.id, roiStrategy.id),
            CommonStrategy::class.java
        )
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test order by ID`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withOrderBy(
                listOf(
                    GdPackageStrategyOrderBy()
                        .withField(GdPackageStrategyOrderByField.ID)
                        .withOrder(Order.ASC)
                )
            )

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(crrStrategy.id, roiStrategy.id, defaultStrategy.id),
            CommonStrategy::class.java
        )

        actualStrategies.sortBy { it.id }
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `list strategies with linked campaigns`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(userInfo.clientInfo!!)

        val strategyId = campaignTypedRepository.getSafely(getShard(), listOf(campaign.id), TextCampaign::class.java)
            .first().strategyId

        val strategy = autobudgetCrr()
            .withCids(listOf(campaign.campaignId))
        prepareAndApplyValid(listOf(strategy))
        val container = defaultContainer()

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(strategy.id, strategyId),
            CommonStrategy::class.java
        )

        actualStrategies.sortBy { it.id }
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    @Test
    fun `test order by TYPE`() {
        val crrStrategy = autobudgetCrr()
        val roiStrategy = autobudgetRoi()
        val defaultStrategy = clientDefaultManualStrategy()
        prepareAndApplyValid(listOf(crrStrategy, roiStrategy, defaultStrategy))

        val container = defaultContainer()
            .withOrderBy(
                listOf(
                    GdPackageStrategyOrderBy()
                        .withField(GdPackageStrategyOrderByField.TYPE)
                        .withOrder(Order.DESC)
                )
            )

        val actualStrategies = strategyTypedRepository.getStrictlyFullyFilled(
            getShard(),
            listOf(crrStrategy.id, roiStrategy.id, defaultStrategy.id),
            CommonStrategy::class.java
        )

        actualStrategies.sortByDescending { it.type }
        val expectedResponse = expectedResponse(actualStrategies.toList(), container.filter)
        val response = execute(container)
        assertThat(response).isEqualTo(expectedResponse)
    }

    private fun archiveUnarchive(strategyId: Long, archive: Boolean) {
        val archiveOperation = strategyOperationFactory.createChangeStatusArchiveOperation(
            getShard(),
            getClientId(),
            listOf(strategyId),
            archive
        )
        assertThat(archiveOperation.prepareAndApply()).`is`(matchedBy(isFullySuccessful<Long>()))
    }

    private fun execute(container: GdPackageStrategiesContainer): Map<String, Any> {
        val query = queryTemplate.format(
            context.operator.login,
            graphQlSerialize(container)
        )

        val result = processor.processQuery(null, query, null, context)

        GraphQLUtils.logErrors(result.errors)
        return result.getData()
    }

    private fun defaultContainer() =
        GdPackageStrategiesContainer()
            .withLimitOffset(GdLimitOffset().withOffset(0).withLimit(10))
            .withOrderBy(
                listOf(
                    GdPackageStrategyOrderBy()
                        .withField(GdPackageStrategyOrderByField.ID)
                        .withOrder(Order.ASC)
                )
            )

    private fun expectedResponse(
        strategies: List<CommonStrategy>,
        filter: GdPackageStrategyFilter?
    ): Map<String, Any> {
        strategies.forEach(packageStrategyService::enrichWithEngagedSession)
        strategies.forEach(packageStrategyService::setGoalIdForStrategy)
        val gdStrategies = strategies.mapNotNull(GdPackageConverter::convert)
        val campaigns = truncatedCampaigns(strategies)
        val m = objectMapper()
        val convertedStrategies = gdStrategies.map {
            val linkedCampaigns: List<Map<String, Any>> = campaigns[it.id]?.map { campaign ->
                mapOf(
                    "id" to campaign.id,
                    "name" to campaign.name,
                )
            } ?: emptyList()
            val map = m.convertValue(it, Map::class.java) as MutableMap<String, Any>
            map.remove("_type")
            map["__typename"] = it.javaClass.simpleName
            map["linkedCampaigns"] = linkedCampaigns
            map
        }
        val entityStats = m.convertValue(calcTotalStats(emptyList()), Map::class.java)
        val converterFilter = filter?.let { m.convertValue(it, Map::class.java) as Map<String, Any> }
        return mapOf(
            "client" to mapOf(
                "strategies" to mapOf(
                    "rowset" to convertedStrategies,
                    "totalCount" to gdStrategies.size,
                    "filter" to converterFilter,
                    "totalStats" to entityStats,
                    "totalGoalStats" to emptyList<Any>()
                )
            )
        )
    }

    private fun truncatedCampaigns(strategies: List<CommonStrategy>): Map<Long, List<GdCampaignTruncated>> {
        val cids = strategies.mapNotNull { it.cids }.flatten()
        val campaigns = campaignInfoService.getTruncatedCampaigns(getClientId(), cids)
        return strategies.associateBy(CommonStrategy::getId) {
            it.cids?.mapNotNull(campaigns::get) ?: emptyList()
        }
    }
}
