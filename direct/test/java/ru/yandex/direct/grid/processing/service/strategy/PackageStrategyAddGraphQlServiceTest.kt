package ru.yandex.direct.grid.processing.service.strategy

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.jooq.Condition
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbschema.ppc.tables.Strategies.STRATEGIES
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPackageStrategies
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPackageStrategyPayload
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdAddPackageStrategyUnion
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.counterId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.goalId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.meaningfulGoalId
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.multitype.repository.filter.ConditionFilter
import ru.yandex.direct.test.utils.assertj.Conditions

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class PackageStrategyAddGraphQlServiceTest : StrategyAddOperationTestBase(),
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

    private val mutationName = "addStrategies"
    private val mutationTemplate = """
        mutation {
          %s(input:%s) {
            addedStrategies {
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
    private val updateTemplate = GraphQlTestExecutor.TemplateMutation(
        mutationName, mutationTemplate,
        GdAddPackageStrategies::class.java,
        GdAddPackageStrategyPayload::class.java
    )

    @Test
    @Parameters(method = "testStrategies")
    @TestCaseName("{0}")
    fun `add successfully strategies`(
        descriptions: String,
        strategy: CommonStrategy
    ) {
        val union = PackageStrategyAddConverters.toGdAddPackageStrategyUnion(strategy)
        val response = execute(listOf(union))
        prepareAndApplyValid(listOf(strategy))

        val expectedStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(),
            listOf(strategy.id),
            strategy.javaClass
        )[strategy.id]!!

        val actualStrategies = strategyTypedRepository.getSafely(
            getShard(),
            ClientIdFilter(clientInfo.client!!.clientId),
            strategy.javaClass
        )
        val actualStrategy = actualStrategies.first()

        assertThat(response.validationResult).isNull()
        assertThat(actualStrategy)
            .`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedStrategy)
                        .useCompareStrategy(
                            DefaultCompareStrategies.allFieldsExcept(
                                newPath("id"),
                                newPath("lastChange"),
                                newPath("lastBidderRestartTime"),
                                newPath("lastUpdateTime"),
                                newPath("dayBudgetLastChange")
                            )
                        )
                )
            )
    }

    data class ClientIdFilter(val clientId: Long) : ConditionFilter() {
        override fun isEmpty(): Boolean = false

        override fun getCondition(): Condition = STRATEGIES.CLIENT_ID.eq(clientId)
    }

    private fun execute(unions: List<GdAddPackageStrategyUnion>): GdAddPackageStrategyPayload {
        val add = GdAddPackageStrategies()
            .withStrategyAddItems(unions)

        return processor.doMutationAndGetPayload(
            updateTemplate,
            add,
            user
        )
    }
}
