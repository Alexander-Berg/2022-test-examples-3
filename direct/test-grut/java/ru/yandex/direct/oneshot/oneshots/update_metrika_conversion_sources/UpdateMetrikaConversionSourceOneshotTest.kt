package ru.yandex.direct.oneshot.oneshots.update_metrika_conversion_sources

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.model.Destination
import ru.yandex.direct.core.entity.conversionsource.service.ConversionSourceService
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.defaultConversionSource
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.oneshot.configuration.GrutOneshotTest
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper
import java.time.LocalDateTime

@GrutOneshotTest
@RunWith(SpringRunner::class)
class UpdateMetrikaConversionSourceOneshotTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var oneshot: UpdateMetrikaConversionSourceOneshot

    private lateinit var clientId: ClientId
    private lateinit var operatorInfo: UserInfo
    private var shard = 0
    private val counterId = 1
    private var goalId = 2L
    private val goalName = "GOAL"
    private val counterName = "COUNTER"
    private val domain = "example.com"
    private lateinit var conversionSource: ConversionSource


    @Before
    fun setUp() {
        operatorInfo = steps.clientSteps().createDefaultClient().chiefUserInfo!!
        val operator = operatorInfo.user!!
        clientId = operator.clientId
        shard = operatorInfo.shard

        conversionSource = defaultConversionSource(clientId).copy(
            name = counterName,
            settings = ConversionSourceSettings.Metrika(counterId.toLong(), domain),
            actions = listOf(ConversionAction(goalName, goalId, null)),
            destination = Destination.CrmApi(counterId = counterId.toLong(), accessUid = operator.uid),
            counterId = counterId.toLong(),
        )
        metrikaClientStub.addUserCounters(
            operator.uid, listOf(
            CounterInfoDirect().withId(counterId).withName(counterName).withSitePath(domain)
        ))
        metrikaClientStub.addCounterGoal(
            counterId,
            CounterGoal().withId(goalId.toInt()).withName(goalName)
        )
    }

    @Test
    fun execute_success() {
        val strategy = TestCampaigns.averageCpaStrategy().withGoalId(goalId)
        val campaign = TestCampaigns.activeTextCampaign(null, null).withStrategy(strategy).withMetrikaCounters(listOf(counterId.toLong()))
        val clientInfo = ClientInfo().withClient(operatorInfo.clientInfo!!.client!!).withChiefUserInfo(operatorInfo).withShard(shard)
        val campaignInfo = CampaignInfo().withCampaign(campaign).withClientInfo(clientInfo)
        steps.campaignSteps().createCampaign(campaignInfo)

        val inputData = InputData(listOf(clientId.asLong()))
        oneshot.execute(inputData, null, shard)

        val actualSources = conversionSourceService.getByClientId(clientId = clientId)
        assertThat(actualSources).usingRecursiveComparison()
            .ignoringFields("id", "createTime", "updateTime")
            .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
            .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
            .isEqualTo(listOf(conversionSource))
    }

    @Test
    fun validate_success() {
        val inputData = InputData(listOf(clientId.asLong()))
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validate_withInvalidClientId() {
        val invalidClientId = 0L
        val inputData = InputData(listOf(invalidClientId))
        val vr = oneshot.validate(inputData)
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                org.hamcrest.Matchers.contains(
                    Matchers.validationError(
                        PathHelper.path(
                            PathHelper.field("clientIds"),
                            PathHelper.index(0)
                        ), CommonDefects.validId()
                    )
                )
            )
        )
    }
}
