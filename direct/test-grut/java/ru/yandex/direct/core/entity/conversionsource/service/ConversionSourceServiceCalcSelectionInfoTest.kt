package ru.yandex.direct.core.entity.conversionsource.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionActionValue
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.testing.data.defaultConversionSourceMetrika
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.TestUtils.randomName
import kotlin.random.Random

private val SOME_FIXED_VALUE = ConversionActionValue.Fixed(Money.valueOf("200", CurrencyCode.RUB))
private val SELECTED_GOAL_ID = Random.nextLong(1000, Goal.METRIKA_GOAL_UPPER_BOUND - 10)
private val UNSELECTED_GOAL_ID = SELECTED_GOAL_ID + 1
private const val COUNTER_ID = 82392432L

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ConversionSourceServiceCalcSelectionInfoTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()

        metrikaClientStub.addUserCounters(
            clientInfo.uid, listOf(
                CounterInfoDirect()
                    .withId(COUNTER_ID.toInt())
                    .withName("some name")
                    .withSitePath("some-domain"),
            )
        )
    }

    @Test
    fun calcSelectionInfo_WithoutValue() {
        createMetrikaConversionSourceWithAction(
            clientInfo.clientId!!, ConversionAction(
                randomName("test conversion action ", 30),
                goalId = SELECTED_GOAL_ID,
                value = null,
            )
        )

        val result = conversionSourceService.calcSelectionInfo(
            clientInfo.clientId!!,
            setOf(SELECTED_GOAL_ID, UNSELECTED_GOAL_ID)
        )

        assertThat(result).isEqualTo(
            mapOf(
                UNSELECTED_GOAL_ID to GoalSelectionInfo(false, null),
                SELECTED_GOAL_ID to GoalSelectionInfo(true, null),
            )
        )
    }

    @Test
    fun calcSelectionInfo_WithValue() {
        createMetrikaConversionSourceWithAction(
            clientInfo.clientId!!, ConversionAction(
                randomName("test conversion action ", 30),
                goalId = SELECTED_GOAL_ID,
                value = SOME_FIXED_VALUE,
            )
        )

        val result = conversionSourceService.calcSelectionInfo(
            clientInfo.clientId!!,
            setOf(SELECTED_GOAL_ID, UNSELECTED_GOAL_ID)
        )

        assertThat(result).isEqualTo(
            mapOf(
                UNSELECTED_GOAL_ID to GoalSelectionInfo(false, null),
                SELECTED_GOAL_ID to GoalSelectionInfo(true, SOME_FIXED_VALUE),
            )
        )
    }

    private fun createMetrikaConversionSourceWithAction(clientId: ClientId, action: ConversionAction) {
        val conversionSource = defaultConversionSourceMetrika(clientId, COUNTER_ID).copy(actions = listOf(action))
        val res = conversionSourceService.add(clientId, listOf(conversionSource))
        check(!res.validationResult.hasAnyErrors()) { "${res.validationResult.flattenErrors()}" }
    }
}
