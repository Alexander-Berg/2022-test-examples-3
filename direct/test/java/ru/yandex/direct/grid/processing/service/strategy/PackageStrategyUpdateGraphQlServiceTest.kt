package ru.yandex.direct.grid.processing.service.strategy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategies
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategyPayload
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategyPayloadItem
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategyUnion
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdateStrategyWithConversion
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.counterId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.goalId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.meaningfulGoalId
import ru.yandex.direct.grid.processing.service.strategy.mutation.update.PackageStrategyUpdateGraphQlService.Companion.UPDATE_STRATEGIES
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class PackageStrategyUpdateGraphQlServiceTest : StrategyUpdateOperationTestBase(),
    AbstractPackageStrategyGraphQlServiceTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    private lateinit var clientInfo: ClientInfo

    override fun getShard() = clientInfo.shard

    override fun getClientId(): ClientId = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Autowired
    private lateinit var processor: GraphQlTestExecutor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var user: User

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        user = UserHelper.getUser(clientInfo.client!!)
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        stubGoals(counterId, listOf(goalId, meaningfulGoalId))
        TestAuthHelper.setDirectAuthentication(user)
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

    private val mutationName = UPDATE_STRATEGIES
    private val mutationTemplate = """
        mutation {
          %s(input:%s) {
            updatedStrategies {
              id
            }
            validationResult {
              errors {
                ...ValidationErrorFragment
              }
              warnings {
                ...ValidationWarningFragment
              }
            }
          }
        }
        fragment ValidationErrorFragment on GdDefect {
          code
          params
          path
        }
        fragment ValidationWarningFragment on GdDefect {
          code
          params
          path
        }
    """
    private val updateTemplate = TemplateMutation(
        mutationName, mutationTemplate,
        GdUpdatePackageStrategies::class.java,
        GdUpdatePackageStrategyPayload::class.java
    )

    @Test
    @Parameters(method = "testStrategies")
    @TestCaseName("{0}")
    fun `do nothing on empty model changes`(
        description: String,
        strategy: CommonStrategy
    ) {
        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!

        val union = PackageStrategyUpdateConverters.toGdUpdatePackageStrategyUnion(actualStrategy)
        val response = execute(listOf(union))

        val expectedResponse = expectedSuccess(listOf(strategy.id))
        val strategyAfterUpdate = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!
        assertThat(response).isEqualTo(expectedResponse)
        assertThat(actualStrategy).isEqualTo(strategyAfterUpdate)
    }

    @Test
    fun `update crr strategy`() {
        val strategy = autobudgetCrr()
        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!

        val updatedStrategy =
            actualStrategy
                .withName("new one")
                .withCrr(5)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK_CROSS_DEVICE)
        val union = PackageStrategyUpdateConverters.toGdUpdatePackageStrategyUnion(updatedStrategy)
        val response = execute(listOf(union))

        val expectedResponse = expectedSuccess(listOf(strategy.id))
        val strategyAfterUpdate = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!
        assertThat(response).isEqualTo(expectedResponse)

        SoftAssertions.assertSoftly { it ->
            it.assertThat(strategyAfterUpdate.name).isEqualTo(updatedStrategy.name)
            it.assertThat(strategyAfterUpdate.crr).isEqualTo(updatedStrategy.crr)
            it.assertThat(strategyAfterUpdate.attributionModel).isEqualTo(updatedStrategy.attributionModel)
        }
    }

    @Test
    fun `fail update on validation errors`() {
        val strategy = autobudgetCrr()
        val addOperation = createAddOperation(listOf(strategy))
        addOperation.prepareAndApply()

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!

        val updatedStrategy =
            actualStrategy
                .withGoalId(RandomNumberUtils.nextPositiveLong())
        val union = PackageStrategyUpdateConverters.toGdUpdatePackageStrategyUnion(updatedStrategy)
        val response = execute(listOf(union))

        val strategyAfterUpdate = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!
        val expectedErrors = listOf(
            GdDefect()
                .withCode(CommonDefects.objectNotFound().defectId().code)
                .withPath(
                    path(
                        field(GdUpdatePackageStrategies.STRATEGY_UPDATE_ITEMS),
                        index(0),
                        field(GdUpdateStrategyWithConversion.GOAL_ID)
                    ).toString()
                )
        )
        SoftAssertions.assertSoftly {
            it.assertThat(response.updatedStrategies.size).isEqualTo(1)
            it.assertThat(response.updatedStrategies.first()).isNull()
            it.assertThat(response.validationResult.errors).isEqualTo(expectedErrors)
            it.assertThat(strategyAfterUpdate).isEqualTo(actualStrategy.withGoalId(strategy.goalId))
        }
    }

    private fun expectedSuccess(ids: List<Long>): GdUpdatePackageStrategyPayload {
        val updateStrategies = ids.map {
            GdUpdatePackageStrategyPayloadItem()
                .withId(it)
        }
        return GdUpdatePackageStrategyPayload()
            .withUpdatedStrategies(updateStrategies)
    }

    private fun execute(unions: List<GdUpdatePackageStrategyUnion>): GdUpdatePackageStrategyPayload {
        val update = GdUpdatePackageStrategies()
            .withStrategyUpdateItems(unions)

        return processor.doMutationAndGetPayload(
            updateTemplate,
            update,
            user
        )
    }
}
