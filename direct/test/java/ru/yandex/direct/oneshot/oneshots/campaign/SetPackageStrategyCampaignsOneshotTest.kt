package ru.yandex.direct.oneshot.oneshots.campaign

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.repository.filter.CampaignFilterFactory
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.utils.StrategyModelUtils.campaignIds
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.strategy.AutobudgetAvgCpaInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.multitype.repository.filter.ConditionFilterFactory
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper


@OneshotTest
@RunWith(SpringRunner::class)
class SetPackageStrategyCampaignsOneshotTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var oneshot: SetPackageStrategyCampaignsOneshot

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var walletService: WalletService

    @Autowired
    private lateinit var strategyTypedRepository: StrategyTypedRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0

    private lateinit var strategy: AutobudgetAvgCpaInfo
    private lateinit var dynamicCampaign: CampaignInfo
    private lateinit var smartCampaign: CampaignInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()

        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        walletService.createWalletForNewClient(clientId, clientInfo.uid)

        strategy = steps.autobudgetAvgCpaSteps().createDefaultStrategy(clientInfo)
        dynamicCampaign = steps.campaignSteps().createActiveDynamicCampaign(clientInfo)
        smartCampaign = steps.campaignSteps().createActiveSmartCampaign(clientInfo)
    }

    @Test
    fun validate_success() {
        val inputData = CreateCampaignsWithPackageStrategyInputData(
            clientId.asLong(),
            strategy.strategyId,
            listOf(dynamicCampaign.campaignId, smartCampaign.campaignId)
        )
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validate_withInvalidClientId() {
        val invalidClientId = 0L
        val inputData = CreateCampaignsWithPackageStrategyInputData(
            invalidClientId,
            strategy.strategyId,
            listOf(dynamicCampaign.campaignId, smartCampaign.campaignId)
        )
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                org.hamcrest.Matchers.contains(
                    Matchers.validationError(
                        PathHelper.path(PathHelper.field("clientId")),
                        CommonDefects.validId()
                    )
                )
            )
        )
    }

    @Test
    fun validate_withNotExistingFeedId() {
        val invalidStrategyId = strategy.strategyId + 1
        val inputData = CreateCampaignsWithPackageStrategyInputData(
            clientId.asLong(),
            invalidStrategyId,
            listOf(dynamicCampaign.campaignId, smartCampaign.campaignId)
        )
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                org.hamcrest.Matchers.contains(
                    Matchers.validationError(
                        PathHelper.path(PathHelper.field("strategyId")),
                        StrategyDefects.strategyNotFound()
                    )
                )
            )
        )
    }

    @Test
    fun execute_success() {
        val dslContext = dslContextProvider.ppc(shard)

        val textCampaign = steps.campaignSteps().createActiveTextCampaign(clientInfo)

        val inputData = CreateCampaignsWithPackageStrategyInputData(
            clientId.asLong(),
            strategy.strategyId,
            listOf(dynamicCampaign.campaignId, smartCampaign.campaignId, textCampaign.campaignId)
        )
        oneshot.execute(inputData, null)

        val actualStrategy = strategyTypedRepository.getTyped(shard, listOf(strategy.strategyId))[0]

        val actualSmartCampaign = campaignTypedRepository.getTypedCampaigns(dslContext,
            ConditionFilterFactory.multipleConditionFilter(
                CampaignFilterFactory.campaignClientIdFilter(clientId),
                CampaignFilterFactory.campaignTypesFilter(setOf(
                    CampaignType.PERFORMANCE
                ))
            )
        )[0] as SmartCampaign
        val actualDynamicCampaign = campaignTypedRepository.getTypedCampaigns(dslContext,
            ConditionFilterFactory.multipleConditionFilter(
                CampaignFilterFactory.campaignClientIdFilter(clientId),
                CampaignFilterFactory.campaignTypesFilter(setOf(
                    CampaignType.DYNAMIC
                ))
            )
        )[0] as DynamicCampaign
        val actualTextCampaign = campaignTypedRepository.getTypedCampaigns(dslContext,
            ConditionFilterFactory.multipleConditionFilter(
                CampaignFilterFactory.campaignClientIdFilter(clientId),
                CampaignFilterFactory.campaignTypesFilter(setOf(
                    CampaignType.TEXT
                ))
            )
        )[0] as TextCampaign

        softly {
            assertThat(actualStrategy.campaignIds()).containsExactlyInAnyOrder(
                smartCampaign.campaignId,
                dynamicCampaign.campaignId,
                textCampaign.campaignId
            )
            assertThat(actualSmartCampaign.strategyId).isEqualTo(strategy.strategyId)
            assertThat(actualDynamicCampaign.strategyId).isEqualTo(strategy.strategyId)
            assertThat(actualTextCampaign.strategyId).isEqualTo(strategy.strategyId)
        }
    }
}
