package ru.yandex.direct.core.entity.campaign.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetCrrStrategy
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.randomPositiveInt

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignStrategyServiceCrrStrategyTest {

    companion object {
        private val COUNTER_ID = randomPositiveInt()
        private const val GOAL_ID = 20001L
    }

    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository
    @Autowired
    private lateinit var campaignStrategyService: CampaignStrategyService
    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var defaultUser: UserInfo
    private lateinit var strategy: DbStrategy
    private lateinit var textCampaignInfo: TextCampaignInfo

    @Before
    fun setUp() {
        defaultUser = steps.userSteps().createDefaultUser()
        metrikaClientStub.addUserCounter(defaultUser.uid, COUNTER_ID)
        metrikaClientStub.addCounterGoal(COUNTER_ID, GOAL_ID.toInt())
        strategy = defaultAutobudgetCrrStrategy(GOAL_ID)
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.CRR_STRATEGY_ALLOWED, true)
        steps.featureSteps().addClientFeature(defaultUser.clientId, FeatureName.FIX_CRR_STRATEGY_ALLOWED, true)

        val textCampaign = defaultTextCampaignWithSystemFields(defaultUser.clientInfo)
                .withMetrikaCounters(listOf(COUNTER_ID.toLong()))
                .withStrategy(strategy)
        textCampaignInfo = steps.textCampaignSteps().createCampaign(defaultUser.clientInfo!!, textCampaign)
    }

    @Test
    fun shouldSetPayForConversionToFalse() = shouldChangePayForConversionFlag(false)

    @Test
    fun shouldSetPayForConversionToTrue() = shouldChangePayForConversionFlag(true)

    private fun shouldChangePayForConversionFlag(payForConversion: Boolean) {
        strategy.strategyData.payForConversion = payForConversion
        val result = campaignStrategyService.updateTextCampaignStrategy(
                textCampaignInfo.id,
                strategy,
                textCampaignInfo.uid,
                UidAndClientId.of(textCampaignInfo.uid, textCampaignInfo.clientId),
                false
        )

        assertThat(result.validationResult.flattenErrors()).isEmpty()

        val campaigns = campaignTypedRepository.getTypedCampaigns(textCampaignInfo.shard, listOf(textCampaignInfo.id))

        assertThat((campaigns[0] as TextCampaign).strategy.strategyData.payForConversion).isEqualTo(payForConversion)
    }

}
