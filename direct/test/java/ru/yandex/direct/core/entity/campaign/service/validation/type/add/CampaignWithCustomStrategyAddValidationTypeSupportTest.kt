package ru.yandex.direct.core.entity.campaign.service.validation.type.add

import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.net.NetAcl
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomStrategy
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.TextCampaignWithCustomStrategy
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl
import ru.yandex.direct.core.entity.metrika.repository.MetrikaCounterByDomainRepository
import ru.yandex.direct.core.entity.metrika.utils.MetrikaGoalsUtils.ecommerceGoalId
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultStrategyForSimpleView
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import java.net.InetAddress

private const val COUNTER_ID = 122L
private const val UNAVAILABLE_COUNTER_ID = 123L
private const val UNAVAILABLE_COUNTER_ID_2 = 124L
private const val NOT_ALLOWED_BY_METRIKA_COUNTER_ID = 125L
private const val UNAVAILABLE_ECOMMERCE_COUNTER_ID = 126L

private const val GOAL_ID = 1233L
private const val UNAVAILABLE_AUTO_GOAL_ID = 1234L
private const val UNAVAILABLE_USER_GOAL_ID = 1235L
private const val NOT_ALLOWED_BY_METRIKA_GOAL_ID = 1236L

@CoreTest
@RunWith(SpringRunner::class)
class CampaignWithCustomStrategyAddValidationTypeSupportTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var netAcl: NetAcl
    @Autowired
    private lateinit var typeSupport: CampaignWithCustomStrategyAddValidationTypeSupport
    @Autowired
    private lateinit var metrikaCounterByDomainRepository: MetrikaCounterByDomainRepository
    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        doReturn(false).`when`(netAcl).isInternalIp(any(InetAddress::class.java))

        clientInfo = steps.clientSteps().createDefaultClient()

        metrikaClientStub.addUserCounter(clientInfo.uid, COUNTER_ID.toInt())
        metrikaClientStub.addUnavailableCounter(UNAVAILABLE_COUNTER_ID)
        metrikaClientStub.addUnavailableCounter(UNAVAILABLE_COUNTER_ID_2)
        metrikaClientStub.addUnavailableCounter(NOT_ALLOWED_BY_METRIKA_COUNTER_ID, false)
        metrikaClientStub.addUnavailableEcommerceCounter(UNAVAILABLE_ECOMMERCE_COUNTER_ID)

        metrikaClientStub.addCounterGoal(COUNTER_ID.toInt(), GOAL_ID.toInt())
        metrikaClientStub.addCounterGoal(UNAVAILABLE_COUNTER_ID.toInt(), CounterGoal()
            .withId(UNAVAILABLE_AUTO_GOAL_ID.toInt())
            .withType(CounterGoal.Type.URL)
            .withSource(CounterGoal.Source.AUTO))
        metrikaClientStub.addCounterGoal(UNAVAILABLE_COUNTER_ID.toInt(), CounterGoal()
            .withId(UNAVAILABLE_USER_GOAL_ID.toInt())
            .withType(CounterGoal.Type.URL)
            .withSource(CounterGoal.Source.USER))
        metrikaClientStub.addCounterGoal(NOT_ALLOWED_BY_METRIKA_COUNTER_ID.toInt(),
            NOT_ALLOWED_BY_METRIKA_GOAL_ID.toInt())
    }

    @After
    fun after() {
        Mockito.reset(netAcl, metrikaCounterByDomainRepository)
    }

    @Test
    fun validate_Successfully() {
        val campaign = createCampaign(listOf(COUNTER_ID))
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_StrategyWithGoalIdAndCampaignWithoutCounters_Error() {
        val campaign = createCampaign(goalId = GOAL_ID)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_StrategyWithUnavailableAutoGoalId_FeaturesDisabled_Error() {
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED)
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_AUTO_GOAL_ID)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    /**
     * Включена фича на проверку флага Метрики о возможности использовать счетчик без доступа.
     * На счетчике флаг не установлен, но по умолчанию считаем, что счетчик можно использовать без доступа,
     * поэтому все хорошо.
     */
    @Test
    fun validate_StrategyWithUnavailableGoalId_AllowedByMetrikaFlagCounter_Success() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID)
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    /**
     * Включена фича на проверку флага Метрики о возможности использовать счетчик без доступа.
     * На счетчике флаг установлен и не разрешает использовать его без доступа,
     * поэтому не даем использовать цели с этого счетчика.
     */
    @Test
    fun validate_StrategyWithUnavailableGoalId_NotAllowedByMetrikaFlagCounter_Error() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(NOT_ALLOWED_BY_METRIKA_COUNTER_ID), NOT_ALLOWED_BY_METRIKA_GOAL_ID)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_UacStrategyWithUnavailableGoalId_Success() {
        disableFeature(FeatureName.UAC_UNAVAILABLE_AUTO_GOALS_ALLOWED)
        enableFeature(FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED)
        
        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.UAC)
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_UacStrategyWithUnavailableGoalId_Error() {
        enableFeature(FeatureName.UAC_UNAVAILABLE_AUTO_GOALS_ALLOWED)
        disableFeature(FeatureName.UAC_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.UAC)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_StrategyWithUnavailableGoalId_Success() {
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED)
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.DIRECT)
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_StrategyWithUnavailableGoalId_Error() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_AUTO_GOALS_ALLOWED)
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.DIRECT)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_ApiStrategyWithUnavailableGoalId_Success() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.API)
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_ApiStrategyWithUnavailableGoalId_Error() {
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)
        
        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.API)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_GeoStrategyWithUnavailableGoalId_Success() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)
        
        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.GEO)
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_GeoStrategyWithUnavailableGoalId_Error() {
        disableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)

        val campaign = createCampaign(listOf(UNAVAILABLE_COUNTER_ID), UNAVAILABLE_USER_GOAL_ID, CampaignSource.GEO)
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    @Test
    fun validate_StrategyWithUnavailableEcommerceGoalId_Success() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)
        enableFeature(FeatureName.COLD_START_FOR_ECOMMERCE_GOALS)

        val campaign = createCampaign(
            listOf(UNAVAILABLE_ECOMMERCE_COUNTER_ID),
            ecommerceGoalId(UNAVAILABLE_ECOMMERCE_COUNTER_ID)
        )
        val vr = validate(campaign)
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validate_StrategyWithUnavailableEcommerceGoalId_Error() {
        enableFeature(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED)
        enableFeature(FeatureName.COLD_START_FOR_ECOMMERCE_GOALS)

        val campaign = createCampaign(
            listOf(UNAVAILABLE_COUNTER_ID),
            ecommerceGoalId(UNAVAILABLE_COUNTER_ID)
        )
        val vr = validate(campaign)
        assertGoalNotFound(vr)
    }

    private fun validate(
        campaign: CampaignWithCustomStrategy
    ): ValidationResult<List<CampaignWithCustomStrategy>, Defect<*>> {
        val metrikaAdapter = RequestBasedMetrikaClientAdapter(metrikaClientStub,
            listOf(clientInfo.uid), setOf(),
            listOf(campaign), true
        )
        val container = createContainer(campaign, metrikaAdapter)
        return typeSupport.validate(container, ValidationResult(listOf(campaign)))
    }

    private fun assertGoalNotFound(vr: ValidationResult<List<CampaignWithCustomStrategy>, Defect<*>>) =
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0),
            field(TextCampaignWithCustomStrategy.STRATEGY),
            field(DbStrategy.STRATEGY_DATA), field(StrategyData.GOAL_ID)), objectNotFound())))

    private fun createContainer(campaign: BaseCampaign, metrikaAdapter: RequestBasedMetrikaClientAdapter) =
        RestrictedCampaignsAddOperationContainerImpl(
            clientInfo.shard, clientInfo.uid,
            clientInfo.clientId!!, clientInfo.uid, clientInfo.uid,
            listOf(campaign), CampaignOptions(), metrikaAdapter, emptyMap()
        ).apply {
            goalIdToCounterIdForCampaignsWithoutCounterIds = mapOf()
        }

    private fun createCampaign(
        counterIds: List<Long>? = null,
        goalId: Long? = null,
        source: CampaignSource? = null,
    ) = defaultTextCampaignWithSystemFields().apply {
        strategy = defaultStrategyForSimpleView(goalId)
        metrikaCounters = counterIds
        this.source = source
    }
    
    private fun enableFeature(featureName: FeatureName) =
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, featureName, true)
    
    private fun disableFeature(featureName: FeatureName) =
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, featureName, false)
}
