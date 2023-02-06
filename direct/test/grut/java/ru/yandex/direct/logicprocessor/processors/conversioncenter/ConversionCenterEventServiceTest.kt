package ru.yandex.direct.logicprocessor.processors.conversioncenter

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.model.Destination
import ru.yandex.direct.core.entity.conversionsource.service.ConversionCenterMetrikaGoalsService
import ru.yandex.direct.core.entity.conversionsource.service.ConversionSourceService
import ru.yandex.direct.core.entity.goal.repository.MetrikaConversionAdGoalsRepository
import ru.yandex.direct.core.entity.goal.repository.MetrikaCountersRepository
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.defaultConversionSource
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.conversioncenter.ConversionCenterEventObject
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.rbac.RbacService
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import java.time.LocalDateTime

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class ConversionCenterEventServiceTest {

    private lateinit var service: ConversionCenterEventService

    private lateinit var conversionCenterMetrikaGoalsService: ConversionCenterMetrikaGoalsService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var rbacService: RbacService

    private lateinit var operatorInfo: UserInfo
    private var shard = 0
    private val counterId = 1
    private var goalId = 2L
    private val counterWithoutNameId = 3
    private var goalWithoutCounterNameId = 4L
    private val goalName = "GOAL"
    private val counterName = "COUNTER"
    private val domain = "example.com"

    @BeforeEach
    fun setUp() {
        operatorInfo = steps.clientSteps().createDefaultClient().chiefUserInfo!!
        shard = operatorInfo.shard

        val metrikaCountersRepository = mock<MetrikaCountersRepository> {
            on(it.getCountersByIds(listOf(counterId))) doReturn mapOf(
                counterId to CounterInfoDirect().withId(counterId).withName(counterName).withSitePath(domain)
            )
            on(it.getCountersByIds(listOf(counterWithoutNameId))) doReturn mapOf(
                counterWithoutNameId to CounterInfoDirect().withId(counterWithoutNameId).withName(null).withSitePath(domain))
        }
        val metrikaConversionAdGoalsRepository = mock<MetrikaConversionAdGoalsRepository> {
            on(it.getCounterGoalsByIds(setOf(counterId), setOf(goalId))) doReturn mapOf(
                counterId to listOf(CounterGoal().withId(goalId.toInt()).withName(goalName))
            )
            on(it.getCounterGoalsByIds(setOf(counterWithoutNameId), setOf(goalWithoutCounterNameId))) doReturn mapOf(
                counterWithoutNameId to listOf(CounterGoal().withId(goalWithoutCounterNameId.toInt()).withName(goalName))
            )
        }

        conversionCenterMetrikaGoalsService = ConversionCenterMetrikaGoalsService(mock(), conversionSourceService, rbacService, metrikaConversionAdGoalsRepository, metrikaCountersRepository)

        service = ConversionCenterEventService(campaignTypedRepository, dslContextProvider, conversionCenterMetrikaGoalsService)

    }

    @Test
    fun processCampaignStrategyChange_success() {
        val clientId = operatorInfo.user!!.clientId
        val expectedConversionSource = defaultConversionSource(clientId).copy(
            name = counterName,
            settings = ConversionSourceSettings.Metrika(counterId.toLong(), domain),
            actions = listOf(ConversionAction(goalName, goalId, null)),
            destination = Destination.CrmApi(counterId = counterId.toLong(), accessUid = operatorInfo.user!!.uid),
            counterId = counterId.toLong()
        )

        val strategy = TestCampaigns.averageCpaStrategy().withGoalId(goalId)
        val campaign = TestCampaigns.activeTextCampaign(null, null).withStrategy(strategy).withMetrikaCounters(listOf(counterId.toLong()))
        val clientInfo = ClientInfo().withClient(operatorInfo.clientInfo!!.client!!).withChiefUserInfo(operatorInfo).withShard(shard)
        val campaignInfo = CampaignInfo().withCampaign(campaign).withClientInfo(clientInfo)
        steps.campaignSteps().createCampaign(campaignInfo)

        val objects = listOf(ConversionCenterEventObject(clientId = clientId.asLong(), campaignId = campaign.id))
        service.processCampaignStrategyChange(shard, objects)

        val actualSources = conversionSourceService.getByClientId(clientId = clientId)
        Assertions.assertThat(actualSources).usingRecursiveComparison()
            .ignoringFields("id", "createTime", "updateTime")
            .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
            .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
            .isEqualTo(listOf(expectedConversionSource))
    }

    @Test
    fun processCampaignStrategyChange_withEmptyCounterName_success() {
        val clientId = operatorInfo.user!!.clientId
        val expectedConversionSource = defaultConversionSource(clientId).copy(
            name = domain,
            settings = ConversionSourceSettings.Metrika(counterWithoutNameId.toLong(), domain),
            actions = listOf(ConversionAction(goalName, goalWithoutCounterNameId, null)),
            destination = Destination.CrmApi(counterId = counterWithoutNameId.toLong(), accessUid = operatorInfo.user!!.uid),
            counterId = counterWithoutNameId.toLong()
        )

        val strategy = TestCampaigns.averageCpaStrategy().withGoalId(goalWithoutCounterNameId)
        val campaign = TestCampaigns.activeTextCampaign(null, null).withStrategy(strategy).withMetrikaCounters(listOf(counterWithoutNameId.toLong()))
        val clientInfo = ClientInfo().withClient(operatorInfo.clientInfo!!.client!!).withChiefUserInfo(operatorInfo).withShard(shard)
        val campaignInfo = CampaignInfo().withCampaign(campaign).withClientInfo(clientInfo)
        steps.campaignSteps().createCampaign(campaignInfo)

        val objects = listOf(ConversionCenterEventObject(clientId = clientId.asLong(), campaignId = campaign.id))
        service.processCampaignStrategyChange(shard, objects)

        val actualSources = conversionSourceService.getByClientId(clientId = clientId)
        Assertions.assertThat(actualSources).usingRecursiveComparison()
            .ignoringFields("id", "createTime", "updateTime")
            .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
            .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
            .isEqualTo(listOf(expectedConversionSource))
    }
}
