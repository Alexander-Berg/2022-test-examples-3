package ru.yandex.direct.grid.processing.service.strategy

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.BroadMatch
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdPackageStrategyUnbindCampaigns
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUnbindedCampaignItem
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUnbindedCampaigns
import ru.yandex.direct.grid.processing.model.strategy.mutation.GdUpdatePackageStrategyiesUnbindCampaigns
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.counterId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.goalId
import ru.yandex.direct.grid.processing.service.strategy.AbstractPackageStrategyGraphQlServiceTest.Companion.meaningfulGoalId
import ru.yandex.direct.grid.processing.service.strategy.mutation.update.PackageStrategyUpdateGraphQlService.Companion.UNBIND_CAMPAIGNS
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.emptyValidationResult
import ru.yandex.direct.test.utils.RandomNumberUtils

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class PackageStrategyUnbindGraphQlServiceTest : StrategyAddOperationTestBase(),
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
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var user: User

    private val mutationName = UNBIND_CAMPAIGNS
    private val mutationTemplate = """
        mutation {
          %s(input:%s) {
            unbindedCampaigns {
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
    private val unbindTemplate = GraphQlTestExecutor.TemplateMutation(
        mutationName,
        mutationTemplate,
        GdUpdatePackageStrategyiesUnbindCampaigns::class.java,
        GdUnbindedCampaigns::class.java
    )

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        user = UserHelper.getUser(clientInfo.client!!)
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        stubGoals(counterId, listOf(goalId, meaningfulGoalId))
        TestAuthHelper.setDirectAuthentication(user)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.PACKAGE_STRATEGIES_STAGE_TWO, true)
    }

    @Test
    fun `do nothing on strategy without campaigns`() {
        val strategy = autobudgetCrr()
            .withIsPublic(true)
        prepareAndApplyValid(listOf(strategy))
        val unknownCampaignId = RandomNumberUtils.nextPositiveInteger()

        val result = execute(strategy.id, listOf(unknownCampaignId.toLong()))

        val expectedResult = GdUnbindedCampaigns().withUnbindedCampaigns(emptyList())

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun `unbind one campaign`() {
        val campaign = createTextCampaignWithDisabledBroadMatchFlag()

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(true)

        prepareAndApplyValid(listOf(strategy))

        val result = execute(strategy.id, listOf(campaign.campaignId))

        val expectedResult = expectedSuccess(listOf(campaign.id))

        val actualCampaign =
            campaignTypedRepository.getSafely(getShard(), listOf(campaign.campaignId), TextCampaign::class.java)
                .first()

        assertThat(result).isEqualTo(expectedResult)
        assertThat(actualCampaign.strategyId).isNotEqualTo(strategy.id)
    }

    @Test
    fun `do nothing on not public package strategy`() {
        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(campaign.campaignId))
            .withIsPublic(false)

        prepareAndApplyValid(listOf(strategy))

        val result = execute(strategy.id, listOf(campaign.campaignId))

        val expectedResult = GdUnbindedCampaigns().withUnbindedCampaigns(emptyList())

        val actualCampaign =
            campaignTypedRepository.getSafely(getShard(), listOf(campaign.campaignId), TextCampaign::class.java)
                .first()

        assertThat(result).isEqualTo(expectedResult)
        assertThat(actualCampaign.strategyId).isEqualTo(strategy.id)
    }

    @Test
    fun `unbind few campaigns`() {
        val campaign1 = createTextCampaignWithDisabledBroadMatchFlag()
        val campaign2 = createTextCampaignWithDisabledBroadMatchFlag()

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(campaign1.id, campaign2.id))
            .withIsPublic(true)

        prepareAndApplyValid(listOf(strategy))

        val result = execute(strategy.id, listOf(campaign1.id, campaign2.id))

        val expectedResult = expectedSuccess(listOf(campaign1.id, campaign2.id))

        val actualCampaigns =
            campaignTypedRepository.getSafely(getShard(), listOf(campaign1.id, campaign2.id), TextCampaign::class.java)

        softly {
            assertThat(result).isEqualTo(expectedResult)
            actualCampaigns.forEach {
                assertThat(it.strategyId).isNotEqualTo(strategy.id)
            }
        }
    }

    @Test
    fun `unbind only requested campaign`() {
        val campaign1 = createTextCampaignWithDisabledBroadMatchFlag()
        val campaign2 = createTextCampaignWithDisabledBroadMatchFlag()

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(campaign1.id, campaign2.id))
            .withIsPublic(true)

        prepareAndApplyValid(listOf(strategy))

        val result = execute(strategy.id, listOf(campaign1.id))

        val expectedResult = expectedSuccess(listOf(campaign1.id))

        val actualCampaign1 =
            campaignTypedRepository.getSafely(getShard(), listOf(campaign1.id), TextCampaign::class.java)
                .first()

        val actualCampaign2 =
            campaignTypedRepository.getSafely(getShard(), listOf(campaign2.id), TextCampaign::class.java)
                .first()

        assertThat(result).isEqualTo(expectedResult)
        assertThat(actualCampaign1.strategyId).isNotEqualTo(strategy.id)
        assertThat(actualCampaign2.strategyId).isEqualTo(strategy.id)
    }

    private fun createTextCampaignWithDisabledBroadMatchFlag() = steps.textCampaignSteps().createCampaign(
        clientInfo,
        TestCampaigns.defaultTextCampaignWithSystemFields()
            .withBroadMatch(
                BroadMatch()
                    .withBroadMatchFlag(false)
                    .withBroadMatchLimit(5)
            )
    )

    private fun expectedSuccess(ids: List<Long>): GdUnbindedCampaigns {
        val updateStrategies = ids.map {
            GdUnbindedCampaignItem()
                .withId(it)
        }
        return GdUnbindedCampaigns()
            .withUnbindedCampaigns(updateStrategies)
            .withValidationResult(
                emptyValidationResult()
            )
    }

    private fun execute(cidsByStrategyId: Map<Long, List<Long>>): GdUnbindedCampaigns {
        val items = cidsByStrategyId.map { (strategyId, campaignIds) ->
            GdPackageStrategyUnbindCampaigns()
                .withCampaignIds(campaignIds)
                .withStrategyId(strategyId)
        }
        val request = GdUpdatePackageStrategyiesUnbindCampaigns()
            .withUnbindItems(items)

        return processor.doMutationAndGetPayload(
            unbindTemplate,
            request,
            user
        )
    }

    private fun execute(strategyId: Long, campaignIds: List<Long>): GdUnbindedCampaigns =
        execute(mapOf(strategyId to campaignIds))
}
