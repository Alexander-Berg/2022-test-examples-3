package ru.yandex.direct.core.entity.strategy.type.common

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignsWithDifferentTypesInOnePackage
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.AutobudgetWeekBundle
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.service.StrategyConstants
import ru.yandex.direct.core.entity.strategy.service.StrategyConstants.DEFAULT_ATTRIBUTION_MODEL
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.enums.CampaignsSource
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.StringDefects.notEmptyString
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@CoreTest
@RunWith(SpringRunner::class)
class CommonStrategyAddTypeSupportTest : StrategyAddOperationTestBase() {
    @Autowired
    lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    private lateinit var clientInfo: ClientInfo
    private lateinit var now: LocalDateTime

    private var walletId: Long = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        now = now()
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `add manual strategy and check populating of common fields`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = clientDefaultManualStrategy()
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
            .withAttributionModel(null)
        prepareAndApplyValid(listOf(strategy))
        val id = strategy.id

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            DefaultManualStrategy::class.java
        )[id]!!

        Assertions.assertThat(actualStrategy.clientId).isEqualTo(clientInfo.clientId!!.asLong())
        Assertions.assertThat(actualStrategy.walletId).isEqualTo(walletId)
        Assertions.assertThat(actualStrategy.lastChange).isAfter(now.minusSeconds(1))
        Assertions.assertThat(actualStrategy.statusArchived).isEqualTo(false)
        Assertions.assertThat(actualStrategy.attributionModel).isEqualTo(DEFAULT_ATTRIBUTION_MODEL)
    }

    @Test
    fun `fail to add strategies for client over limit`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategies = List(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER + 1) {
            clientDefaultManualStrategy().withClientId(null).withWalletId(null).withIsPublic(true)
        }
        val result = createOperation(strategies).prepareAndApply().validationResult

        Assertions.assertThat(result?.errors).has(
            matchedBy(
                contains(
                    StrategyDefects.unarchivedStrategiesNumberLimitExceeded(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER)
                )
            )
        )
    }

    @Test
    fun `add manual strategy for client without wallet and check throwing exception`() {
        val strategy = clientDefaultManualStrategy()
        assertThatThrownBy { createOperation(listOf(strategy)).prepareAndApply() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `add manual strategy for non existing client id and check throwing exception`() {
        val strategy = clientDefaultManualStrategy()
        assertThatThrownBy {
            strategyOperationFactory.createStrategyAddOperation(
                getShard(),
                getOperatorUid(),
                ClientId.fromLong(Long.MAX_VALUE),
                getClientUid(),
                listOf(strategy),
                StrategyOperationOptions()
            ).prepareAndApply()
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `fail to add invalid strategy`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val strategy = clientDefaultManualStrategy()
            .withName("")

        val result = prepareAndApplyInvalid(listOf(strategy))
        val expectedDefect = notEmptyString()
        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(CommonStrategy.NAME)),
                expectedDefect
            )
        )

        result.check(matcher)
    }

    @Test
    fun `add strategy with linking cid and check strategy_id is set`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = clientDefaultManualStrategy()
            .withCids(listOf(campaign.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
            .withEnableCpcHold(false)
        prepareAndApplyValid(listOf(strategy))

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(campaign.id),
            CampaignWithPackageStrategy::class.java
        )[0]

        assertThat(actualCampaign.strategyId).isEqualTo(strategy.id)
    }

    @Test
    fun `add strategy with linking cid and check strategy fields are set`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val campaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = autobudgetAvgCpa()
            .withCids(listOf(campaign.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
        prepareAndApplyValid(listOf(strategy))

        val actualCampaign = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(campaign.id),
            TextCampaign::class.java
        )[0]

        val expectingStrategyData = StrategyData()
        expectingStrategyData.goalId = strategy.goalId
        expectingStrategyData.avgCpa = strategy.avgCpa
        expectingStrategyData.bid = strategy.bid
        expectingStrategyData.payForConversion = strategy.isPayForConversionEnabled
        expectingStrategyData.name = strategy.type.name.lowercase()
        expectingStrategyData.version = 1

        assertThat(actualCampaign.strategy.strategyData)
            .usingRecursiveComparison()
            .usingOverriddenEquals()
            .ignoringExpectedNullFields()
            .isEqualTo(expectingStrategyData)
        assertThat(actualCampaign.strategy.strategy)
            .isEqualTo(campaign.typedCampaign.strategy.strategy)
        assertThat(actualCampaign.strategy.platform)
            .isEqualTo(campaign.typedCampaign.strategy.platform)
    }

    @Test
    fun `add strategy with linking different type campaigns return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val smartCampaign = steps.smartCampaignSteps().createDefaultCampaign(clientInfo)

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(textCampaign.id, smartCampaign.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                validationError(
                    path(index(0)),
                    campaignsWithDifferentTypesInOnePackage()
                )
            )
        )
    }

    @Test
    fun `add strategy with linking 2 text campaigns strategy_id is set`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val textCampaignFirst = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val textCampaignSecond = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(textCampaignFirst.id, textCampaignSecond.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withIsPublic(true)
            .withStatusArchived(null)

        prepareAndApplyValid(listOf(strategy))

        val actualCampaigns = campaignTypedRepository.getSafely(
            clientInfo.shard,
            listOf(textCampaignFirst.id, textCampaignSecond.id),
            CampaignWithPackageStrategy::class.java
        )

        assertThat(actualCampaigns[0].strategyId).isEqualTo(strategy.id)
        assertThat(actualCampaigns[1].strategyId).isEqualTo(strategy.id)
    }

    @Test
    fun `add public manual strategy return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = clientDefaultManualStrategy()
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withIsPublic(true)
            .withStatusArchived(null)
        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                validationError(
                    path(index(0)),
                    StrategyDefects.unavailableStrategyTypeForPublication()
                )
            )
        )
    }

    @Test
    fun `add non-public manual strategy with success`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = clientDefaultManualStrategy()
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withIsPublic(false)
            .withStatusArchived(null)
        prepareAndApplyValid(listOf(strategy))
    }

    @Test
    fun `add public strategy with linking cpm banner campaign`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val cpmBannerCampaign = steps.cpmBannerCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = clientAutobudgetReachStrategy()
            .withIsPublic(true)
            .withCids(listOf(cpmBannerCampaign.id))

        val result = prepareAndApplyInvalid(listOf(strategy))

        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(AutobudgetMaxReach.CIDS),
                        PathHelper.index(0)
                    ),
                    inconsistentStrategyToCampaignType()
                )
            )
        )
    }

    @Test
    fun `add strategy with linking uac campaigns return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        setUacSourceToCampaign(textCampaign)

        val strategy = autobudgetAvgCpa()
            .withCids(listOf(textCampaign.id))
            .withClientId(null)
            .withWalletId(null)
            .withLastChange(null)
            .withStatusArchived(null)
            .withIsPublic(true)

        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(AutobudgetWeekBundle.CIDS),
                        PathHelper.index(0)
                    ),
                    invalidValue()
                )
            )
        )
    }

    @Test
    fun `not supported strategy linking to campaign validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val strategy = autobudgetAvgCpi()
            .withCids(listOf(textCampaign.id))

        val result = prepareAndApplyInvalid(listOf(strategy))
        Assert.assertThat(
            result,
            Matchers.hasDefectDefinitionWith(
                validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(AutobudgetAvgCpi.CIDS),
                        PathHelper.index(0)
                    ),
                    inconsistentStrategyToCampaignType()
                )
            )
        )
    }

    private fun setUacSourceToCampaign(textCampaign: TextCampaignInfo) {
        dslContextProvider.ppc(textCampaign.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.SOURCE, CampaignsSource.uac)
            .where(CAMPAIGNS.CID.eq(textCampaign.id))
            .execute()
    }
}
