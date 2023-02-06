package ru.yandex.direct.core.entity.strategy.type.common

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.campaignsWithDifferentTypesInOnePackage
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.changePublicStrategyToPrivate
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.inconsistentStrategyToCampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.unavailableStrategyTypeForPublication
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpa
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi
import ru.yandex.direct.core.entity.strategy.model.AutobudgetMaxReach
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.model.DefaultManualStrategy
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel
import ru.yandex.direct.core.entity.strategy.model.StrategyName
import ru.yandex.direct.core.entity.strategy.service.StrategyConstants
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationTestBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpaStrategy.autobudgetAvgCpa
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetAvgCpiStrategy.autobudgetAvgCpi
import ru.yandex.direct.core.testing.data.strategy.TestAutobudgetMaxReachStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy
import ru.yandex.direct.core.testing.data.strategy.TestDefaultManualStrategy.clientDefaultManualStrategy
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.Tables.STRATEGIES
import ru.yandex.direct.dbschema.ppc.enums.CampaignsSource
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.testing.matchers.result.MassResultMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.defect.StringDefects.notEmptyString
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.time.LocalDateTime
import java.time.LocalDateTime.now

@CoreTest
@RunWith(SpringRunner::class)
class CommonStrategyUpdateTypeSupportTest : StrategyUpdateOperationTestBase() {

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var strategy: DefaultManualStrategy
    private lateinit var now: LocalDateTime
    private var walletId: Long = 0

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        strategy = TestDefaultManualStrategy.clientDefaultManualStrategy()

        val addOperation = createAddOperation(listOf(strategy))
        now = now()
        addOperation.prepareAndApply().get(0).result
    }

    override fun getShard() = clientInfo.shard

    override fun getClientId() = clientInfo.clientId!!

    override fun getOperatorUid(): Long = clientInfo.uid

    @Test
    fun `update to valid strategy`() {
        val name = strategy.name + "new"
        val modelChanges = ModelChanges(strategy.id, CommonStrategy::class.java)
            .process(name, CommonStrategy.NAME)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            CommonStrategy::class.java
        )[strategy.id]!!
        assertThat(actualStrategy.name).isEqualTo(name)
    }

    @Test
    fun `update to valid strategy check changing update time`() {
        val name = strategy.name + "new"
        val modelChanges = ModelChanges(strategy.id, CommonStrategy::class.java)
            .process(name, CommonStrategy.NAME)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val now = now()
        dslContextProvider.ppc(getShard())
            .update(STRATEGIES)
            .set(STRATEGIES.LAST_CHANGE, now.minusDays(1))
            .where(STRATEGIES.STRATEGY_ID.eq(strategy.id))
            .execute()

        val result = updateOperation.prepareAndApply()
        result.check(MassResultMatcher.isFullySuccessful())

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            CommonStrategy::class.java
        )[strategy.id]!!
        assertThat(actualStrategy.lastChange).isAfterOrEqualTo(now.minusSeconds(1))
    }

    @Test
    fun `fail on validate when update to invalid strategy`() {
        val name = strategy.name
        val modelChanges = ModelChanges(strategy.id, CommonStrategy::class.java)
            .process(" ", CommonStrategy.NAME)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply()

        val expectedDefect = notEmptyString()
        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(CommonStrategy.NAME)),
                expectedDefect
            )
        )

        assertThat(result.validationResult).`is`(matchedBy(matcher))

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            CommonStrategy::class.java
        )[strategy.id]!!
        assertThat(actualStrategy.name).isEqualTo(name)
    }

    @Test
    fun `fail on validate when update strategies for client over limit`() {
        val strategies = List(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER) {
            autobudgetAvgCpa().withClientId(null).withWalletId(null)
                .withIsPublic(true)
        }
        createAddOperation(strategies).prepareAndApply()

        val modelChanges = ModelChanges(strategy.id, CommonStrategy::class.java)
            .process(true, CommonStrategy.IS_PUBLIC)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply().validationResult

        assertThat(result?.errors).has(
            matchedBy(
                contains(
                    StrategyDefects.unarchivedStrategiesNumberLimitExceeded(StrategyConstants.MAX_UNARCHIVED_STRATEGIES_FOR_CLIENT_NUMBER)
                )
            )
        )

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(strategy.id),
            CommonStrategy::class.java
        )[strategy.id]!!
        assertThat(actualStrategy.isPublic).isFalse
    }

    @Test
    fun `fail on preValidate when update to invalid strategy`() {
        val modelChanges = ModelChanges(strategy.id, CommonStrategy::class.java)
            .process(123, CommonStrategy.WALLET_ID)

        val updateOperation = createUpdateOperation(listOf(modelChanges))

        val result = updateOperation.prepareAndApply()

        val expectedDefect = forbiddenToChange()
        val matcher = Matchers.hasDefectDefinitionWith<Any>(
            Matchers.validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(CommonStrategy.WALLET_ID)),
                expectedDefect
            )
        )

        assertThat(result.validationResult).`is`(matchedBy(matcher))
    }

    fun `add and after that update autobudgetAvgCpa strategy with expecting success of updating isPublic`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = autobudgetAvgCpa()
            .withIsPublic(false)
            .withCids(listOf())

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, AutobudgetAvgCpa::class.java)
            .process(true, AutobudgetAvgCpa.IS_PUBLIC)

        prepareAndApplyValid(listOf(modelChanges))

        val actualStrategy = strategyTypedRepository.getIdToModelSafely(
            getShard(), listOf(id),
            AutobudgetAvgCpa::class.java
        )[id]!!

        assertThat(actualStrategy.isPublic).isTrue
    }

    @Test
    fun `add public strategy and try to update it to private but fails with defect`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf())

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id
        val textCampaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id

        val modelChanges = ModelChanges(id, AutobudgetAvgCpa::class.java)
            .process(false, AutobudgetAvgCpa.IS_PUBLIC)
            .process(listOf(textCampaignId), AutobudgetAvgCpa.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(CommonStrategy.IS_PUBLIC)
                    ),
                    changePublicStrategyToPrivate()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `update strategy with linking different type campaigns return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf())

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id
        val textCampaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val smartCampaignId = steps.smartCampaignSteps().createDefaultCampaign(clientInfo).id
        val cids = listOf(textCampaignId, smartCampaignId)

        val modelChanges = ModelChanges(id, AutobudgetAvgCpa::class.java)
            .process(cids, AutobudgetAvgCpa.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    path(index(0)),
                    campaignsWithDifferentTypesInOnePackage()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `update strategy with text campaign with linking different type campaign return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id
        val smartCampaignId = steps.smartCampaignSteps().createDefaultCampaign(clientInfo).id
        val cids = listOf(textCampaignId, smartCampaignId)

        val modelChanges = ModelChanges(id, AutobudgetAvgCpa::class.java)
            .process(cids, AutobudgetAvgCpa.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    path(index(0)),
                    campaignsWithDifferentTypesInOnePackage()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `update strategy with text campaign with linking same type campaign set strategy_id`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id
        val textCampaignIdSecondId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val cids = listOf(textCampaignFirstId, textCampaignIdSecondId)

        val modelChanges = ModelChanges(id, AutobudgetAvgCpa::class.java)
            .process(cids, AutobudgetAvgCpa.CIDS)

        prepareAndApplyValid(listOf(modelChanges))
    }

    @Test
    fun `update public strategy to manual return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(StrategyName.DEFAULT_, DefaultManualStrategy.TYPE)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    path(index(0)),
                    unavailableStrategyTypeForPublication()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `add new campaigns to strategy, campaigns updated`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val textCampaignSecondId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val textCampaignThirdId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaignFirstId, textCampaignSecondId, textCampaignThirdId), DefaultManualStrategy.CIDS)

        prepareAndApplyValid(listOf(modelChanges))
        val typedCampaigns = campaignTypedRepository.getTypedCampaigns(
            clientInfo.shard, listOf(
                textCampaignFirstId, textCampaignSecondId,
                textCampaignThirdId
            )
        ).filterIsInstance(CampaignWithPackageStrategy::class.java)
        assertThat(typedCampaigns).allMatch { it.strategyId == id }
    }

    @Test
    fun `add new campaigns to strategy, platform and diff places not deleted`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)
        val strategy = autobudgetAvgCpa().withIsPublic(true)

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaign.id), DefaultManualStrategy.CIDS)

        prepareAndApplyValid(listOf(modelChanges))
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(
            clientInfo.shard, listOf(
                textCampaign.id
            )
        ).filterIsInstance(CampaignWithPackageStrategy::class.java)[0]

        assertThat(actualCampaign.strategy.platform).isEqualTo(textCampaign.typedCampaign.strategy.platform)
        assertThat(actualCampaign.strategy.strategy).isEqualTo(textCampaign.typedCampaign.strategy.strategy)
    }

    @Test
    fun `update strategy, campaigns updated`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaignFirstId), DefaultManualStrategy.CIDS)
            .process(StrategyAttributionModel.FIRST_CLICK_CROSS_DEVICE, DefaultManualStrategy.ATTRIBUTION_MODEL)

        prepareAndApplyValid(listOf(modelChanges))
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(
            clientInfo.shard, listOf(textCampaignFirstId)
        ).filterIsInstance(TextCampaign::class.java).get(0)

        assertThat(actualCampaign.attributionModel).isEqualTo(CampaignAttributionModel.FIRST_CLICK_CROSS_DEVICE)
    }

    @Test
    fun `update strategy without change, campaigns not updated`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val textCampaignSecondId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val textCampaignThirdId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId, textCampaignSecondId, textCampaignThirdId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaignSecondId, textCampaignFirstId, textCampaignThirdId), DefaultManualStrategy.CIDS)

        val typedCampaignsBeforeStrategyUpdate = campaignTypedRepository.getTypedCampaigns(
            clientInfo.shard, listOf(
                textCampaignFirstId, textCampaignSecondId,
                textCampaignThirdId
            )
        ).filterIsInstance(CampaignWithPackageStrategy::class.java)
            .map { it.lastChange }
        prepareAndApplyValid(listOf(modelChanges))
        val typedCampaignsAfterStrategyUpdate = campaignTypedRepository.getTypedCampaigns(
            clientInfo.shard, listOf(
                textCampaignFirstId, textCampaignSecondId,
                textCampaignThirdId
            )
        ).filterIsInstance(CampaignWithPackageStrategy::class.java)
            .map { it.lastChange }

        assertThat(typedCampaignsAfterStrategyUpdate)
            .containsExactlyInAnyOrderElementsOf(typedCampaignsBeforeStrategyUpdate)
    }

    @Test
    fun `add new campaigns and delete old to strategy return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val textCampaignFirstId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)
            .withCids(listOf(textCampaignFirstId))

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val textCampaignSecondId = steps.textCampaignSteps().createDefaultCampaign(clientInfo).id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaignSecondId), DefaultManualStrategy.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(CommonStrategy.CIDS)
                    ),
                    invalidValue()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `update strategy of other client return validation error`() {
        val otherClient = steps.clientSteps().createDefaultClient()
        walletId = walletService.createWalletForNewClient(otherClient.clientId, otherClient.uid)
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)

        strategyOperationFactory.createStrategyAddOperation(
            getShard(),
            getOperatorUid(),
            otherClient.clientId!!,
            getClientUid(),
            listOf(strategy),
            StrategyOperationOptions()
        ).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(StrategyAttributionModel.FIRST_CLICK_CROSS_DEVICE, DefaultManualStrategy.ATTRIBUTION_MODEL)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(CommonStrategy.ID)
                    ),
                    objectNotFound()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `making manual strategy public return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val strategy = clientDefaultManualStrategy()
            .withIsPublic(false)

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(true, DefaultManualStrategy.IS_PUBLIC)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    path(index(0)),
                    unavailableStrategyTypeForPublication()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `add cpm banner campaign to public strategy return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val strategy = TestAutobudgetMaxReachStrategy.clientAutobudgetReachStrategy();
        createAddOperation(listOf(strategy)).prepareAndApply()

        val cpmBannerCampaign = steps.cpmBannerCampaignSteps().createDefaultCampaign(clientInfo)

        val modelChanges = ModelChanges(strategy.id, AutobudgetMaxReach::class.java)
            .process(true, AutobudgetMaxReach.IS_PUBLIC)
            .process(listOf(cpmBannerCampaign.id), AutobudgetMaxReach.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(CommonStrategy.CIDS),
                        PathHelper.index(0)
                    ),
                    StrategyDefects.inconsistentStrategyToCampaignType()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `add uac campaign to public strategy return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val strategy = autobudgetAvgCpa()
            .withIsPublic(true)

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        setUacSourceToCampaign(textCampaign)

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaign.id), DefaultManualStrategy.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(CommonStrategy.CIDS),
                        PathHelper.index(0)
                    ),
                    invalidValue()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    @Test
    fun `add text campaign to autobudgetavgcpi strategy return validation error`() {
        walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)
        val strategy = autobudgetAvgCpi()

        createAddOperation(listOf(strategy)).prepareAndApply()

        val id = strategy.id

        val textCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo)

        val modelChanges = ModelChanges(id, DefaultManualStrategy::class.java)
            .process(listOf(textCampaign.id), DefaultManualStrategy.CIDS)

        val vr = prepareAndApplyInvalid(listOf(modelChanges))

        val matcher =
            Matchers.hasDefectDefinitionWith<Any>(
                Matchers.validationError(
                    PathHelper.path(
                        PathHelper.index(0),
                        PathHelper.field(AutobudgetAvgCpi.CIDS),
                        PathHelper.index(0)
                    ),
                    inconsistentStrategyToCampaignType()
                )
            )

        assertThat(vr).`is`(matchedBy(matcher))
    }

    private fun setUacSourceToCampaign(textCampaign: TextCampaignInfo) {
        dslContextProvider.ppc(textCampaign.shard)
            .update(Tables.CAMPAIGNS)
            .set(Tables.CAMPAIGNS.SOURCE, CampaignsSource.uac)
            .where(Tables.CAMPAIGNS.CID.eq(textCampaign.id))
            .execute()
    }
}
