package ru.yandex.direct.core.entity.campaign.service.type.add

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCalltrackingOnSite
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultSmartCampaign
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rbac.RbacService

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class SmartAndDynamicCampaignAddCalltrackingTest {
    companion object {
        private const val COUNTER_ID = 1
        private const val VALID_GOAL_ID = 55L
        private const val DOMAIN_ID = 2L
    }

    @Autowired
    private lateinit var campaignModifyRepository: CampaignModifyRepository

    @Autowired
    private lateinit var strategyTypedRepository: StrategyTypedRepository

    @Autowired
    private lateinit var addRestrictedCampaignValidationService: AddRestrictedCampaignValidationService

    @Autowired
    private lateinit var campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var rbacService: RbacService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClientFactory: RequestBasedMetrikaClientFactory

    @Autowired
    private lateinit var goalUtilsService: GoalUtilsService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var defaultClient: ClientInfo
    private var calltrackingSettingsId = 0L

    @Before
    fun before() {
        defaultClient = steps.clientSteps()
            .createClient(
                TestClients.defaultClient().withWorkCurrency(CurrencyCode.RUB)
            )
        steps.featureSteps()
            .addClientFeature(defaultClient.clientId, FeatureName.DISABLE_BILLING_AGGREGATES, true)
        metrikaClientStub.addUserCounter(defaultClient.uid, COUNTER_ID)
        metrikaClientStub.addCounterGoal(COUNTER_ID, VALID_GOAL_ID.toInt())
        calltrackingSettingsId = steps.calltrackingSettingsSteps()
            .add(defaultClient.clientId, DOMAIN_ID)
    }

    @Test
    fun addSmartCampaignCalltrackingTest() {
        val smartCampaign = defaultSmartCampaign()
            .withMetrikaCounters(listOf(COUNTER_ID.toLong()))
            .withCalltrackingSettingsId(calltrackingSettingsId)

        checkAddCampaigns(smartCampaign)
    }

    @Test
    fun addDynamicCampaignCalltrackingTest() {
        val dynamicCampaign = defaultDynamicCampaign()
            .withCalltrackingSettingsId(calltrackingSettingsId)

        checkAddCampaigns(dynamicCampaign)
    }

    private fun checkAddCampaigns(campaign: CampaignWithCalltrackingOnSite) {
        val options = CampaignOptions()

        val addOperation = RestrictedCampaignsAddOperation(
            listOf(campaign),
            defaultClient.shard,
            UidAndClientId.of(defaultClient.uid, defaultClient.clientId!!),
            defaultClient.uid,
            campaignModifyRepository,
            strategyTypedRepository,
            addRestrictedCampaignValidationService,
            campaignAddOperationSupportFacade,
            dslContextProvider,
            rbacService, options, metrikaClientFactory, goalUtilsService
        )
        val result = addOperation.prepareAndApply()


        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(result.validationResult.flattenErrors()).isEmpty()
            softly.assertThat(result.result.size).isEqualTo(1)
            softly.assertThat(result[0].result).isNotNull
        }

        val typedCampaigns = campaignTypedRepository.getTypedCampaigns(defaultClient.shard, listOf(campaign.id))

        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(typedCampaigns).isNotNull
            softly.assertThat(typedCampaigns!!.size).isEqualTo(1)
            softly.assertThat((typedCampaigns[0] as CampaignWithCalltrackingOnSite).calltrackingSettingsId)
                .isEqualTo(calltrackingSettingsId)
        }
    }

}

