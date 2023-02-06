package ru.yandex.direct.core.entity.conversionsource.service

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.contains
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionActionValue
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.model.Destination
import ru.yandex.direct.core.entity.conversionsource.model.MetrikaGoalSelection
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode
import ru.yandex.direct.core.testing.data.defaultConversionSourceMetrika
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.result.PathHelper
import java.math.BigDecimal
import java.time.LocalDateTime

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ConversionCenterMetrikaGoalsServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    @Autowired
    private lateinit var conversionCenterMetrikaGoalsService: ConversionCenterMetrikaGoalsService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    private lateinit var clientInfo: ClientInfo

    private var firstCounterId = 1L
    private var secondCounterId = 2L
    private var counterWithEmptyNameId = 3L

    private var firstGoalId = 4L
    private var secondGoalId = 5L
    private var thirdGoalId = 6L
    private var counterWithEmptyNameGoalId = 7L

    private lateinit var firstConversionSource: ConversionSource
    private lateinit var secondConversionSource: ConversionSource
    private lateinit var conversionSourceWithEmptyCounterName: ConversionSource

    private lateinit var firstConversionAction: ConversionAction
    private lateinit var secondConversionAction: ConversionAction
    private lateinit var thirdConversionAction: ConversionAction
    private lateinit var conversionActionWithEmptyCounterName: ConversionAction

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()

        val counterString = "COUNTER"
        val goalString = "GOAL"
        val counterDomain = "site.ru"

        firstConversionAction = ConversionAction(
            name = "$goalString$firstGoalId",
            goalId = firstGoalId,
            value = null
        )

        secondConversionAction = ConversionAction(
            name = "$goalString$secondGoalId",
            goalId = secondGoalId,
            value = null
        )

        thirdConversionAction = ConversionAction(
            name = "$goalString$thirdGoalId",
            goalId = thirdGoalId,
            value = null
        )

        conversionActionWithEmptyCounterName = ConversionAction(
            name = "$goalString$counterWithEmptyNameGoalId",
            goalId = counterWithEmptyNameGoalId,
            value = null
        )

        val defaultConversionSource = defaultConversionSourceMetrika(clientId = clientInfo.clientId!!)

        firstConversionSource = defaultConversionSource.copy(
            name = counterString + firstCounterId,
            settings = ConversionSourceSettings.Metrika(firstCounterId, counterDomain),
            counterId = firstCounterId,
            actions = listOf(firstConversionAction),
            destination = Destination.CrmApi(firstCounterId, clientInfo.uid),
        )

        secondConversionSource = defaultConversionSource.copy(
            name = counterString + secondCounterId,
            settings = ConversionSourceSettings.Metrika(secondCounterId, counterDomain),
            counterId = secondCounterId,
            actions = listOf(secondConversionAction, thirdConversionAction),
            destination = Destination.CrmApi(secondCounterId, clientInfo.uid),
        )

        conversionSourceWithEmptyCounterName = defaultConversionSource.copy(
            name = counterDomain,
            settings = ConversionSourceSettings.Metrika(counterWithEmptyNameId, counterDomain),
            counterId = counterWithEmptyNameId,
            actions = listOf(conversionActionWithEmptyCounterName),
            destination = Destination.CrmApi(counterWithEmptyNameId, clientInfo.uid),
        )

        metrikaClientStub.addUserCounters(clientInfo.uid, listOf(
            CounterInfoDirect().withId(firstCounterId.toInt()).withName(counterString + firstCounterId)
                .withSitePath(counterDomain),
            CounterInfoDirect().withId(secondCounterId.toInt()).withName(counterString + secondCounterId)
                .withSitePath(counterDomain),
            CounterInfoDirect().withId(counterWithEmptyNameId.toInt()).withName("").withSitePath(counterDomain)
        ))

        metrikaClientStub.addCounterGoal(
            firstCounterId.toInt(),
            CounterGoal().withId(firstGoalId.toInt()).withName(goalString + firstGoalId)
        )
        metrikaClientStub.addCounterGoal(
            secondCounterId.toInt(),
            CounterGoal().withId(secondGoalId.toInt()).withName(goalString + secondGoalId)
        )
        metrikaClientStub.addCounterGoal(
            secondCounterId.toInt(),
            CounterGoal().withId(thirdGoalId.toInt()).withName(goalString + thirdGoalId)
        )
        metrikaClientStub.addCounterGoal(
            counterWithEmptyNameId.toInt(),
            CounterGoal().withId(counterWithEmptyNameGoalId.toInt()).withName(goalString + counterWithEmptyNameGoalId)
        )

        val input = listOf(MetrikaGoalSelection(firstCounterId, firstGoalId, null, true))

        conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(clientInfo.clientId!!, clientInfo.uid, input)
    }

    @Test
    fun addConversionSources_default_success() {
        val input = listOf(
            MetrikaGoalSelection(secondCounterId, secondGoalId, null, true),
            MetrikaGoalSelection(secondCounterId, thirdGoalId, null, true),
        )

        val result =
            conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(
                clientInfo.clientId!!,
                clientInfo.uid,
                input
            )

        val actualMetrikaConversionSources =
            conversionSourceService.getByClientId(clientInfo.clientId!!, ConversionSourceTypeCode.METRIKA)

        softly {
            assertThat(result.flattenErrors()).isEmpty()
            assertThat(actualMetrikaConversionSources)
                .usingRecursiveComparison()
                .ignoringFields("id", "createTime", "updateTime")
                .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(listOf(firstConversionSource, secondConversionSource))
        }
    }

    @Test
    fun addConversionSources_withEmptyCounterName_success() {
        val input = listOf(
            MetrikaGoalSelection(counterWithEmptyNameId, counterWithEmptyNameGoalId, null, true)
        )

        val result = conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(
            clientInfo.clientId!!,
            clientInfo.uid,
            input
        )

        val actualMetrikaConversionSources =
            conversionSourceService.getByClientId(clientInfo.clientId!!, ConversionSourceTypeCode.METRIKA)

        softly {
            assertThat(result.flattenErrors()).isEmpty()
            assertThat(actualMetrikaConversionSources)
                .usingRecursiveComparison()
                .ignoringFields("id", "createTime", "updateTime")
                .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(listOf(firstConversionSource,
                    conversionSourceWithEmptyCounterName
                ))
        }
    }

    @Test
    fun updateConversionSources_success() {
        val expectedConversionActionValue = ConversionActionValue.Fixed(Money.valueOf(BigDecimal(10), CurrencyCode.RUB))
        val input = listOf(MetrikaGoalSelection(firstCounterId, firstGoalId, expectedConversionActionValue, true))

        val result =
            conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(
                clientInfo.clientId!!,
                clientInfo.uid,
                input
            )

        val actualMetrikaConversionSources =
            conversionSourceService.getByClientId(clientInfo.clientId!!, ConversionSourceTypeCode.METRIKA)

        val expectedConversionActions = listOf(firstConversionAction.copy(value = expectedConversionActionValue))

        softly {
            assertThat(result.flattenErrors()).isEmpty()
            assertThat(actualMetrikaConversionSources)
                .usingRecursiveComparison()
                .ignoringFields("id", "createTime", "updateTime")
                .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(listOf(firstConversionSource.copy(actions = expectedConversionActions)))
        }
    }

    @Test
    fun removeConversionSources_success() {
        val input = listOf(
            MetrikaGoalSelection(firstCounterId, firstGoalId, null, false),
            MetrikaGoalSelection(secondCounterId, secondGoalId, null, true),
            MetrikaGoalSelection(secondCounterId, thirdGoalId, null, true),
        )

        val result =
            conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(
                clientInfo.clientId!!,
                clientInfo.uid,
                input
            )

        val actualMetrikaConversionSources =
            conversionSourceService.getByClientId(clientInfo.clientId!!, ConversionSourceTypeCode.METRIKA)

        softly {
            assertThat(result.flattenErrors()).isEmpty()
            assertThat(actualMetrikaConversionSources)
                .usingRecursiveComparison()
                .ignoringFields("id", "createTime", "updateTime")
                .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
                .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(listOf(secondConversionSource))
        }
    }

    @Test
    fun updateConversionSources_goalDoesNotMatchCounter() {
        val expectedConversionActionValue = ConversionActionValue.Fixed(Money.valueOf(BigDecimal(10), CurrencyCode.RUB))
        val input = listOf(
            MetrikaGoalSelection(firstCounterId, thirdGoalId, expectedConversionActionValue, true)
        )

        val result =
            conversionCenterMetrikaGoalsService.updateMetrikaGoalsSelection(
                clientInfo.clientId!!,
                clientInfo.uid,
                input
            )

        assertThat(result.flattenErrors()).`is`(
            matchedBy(
                contains(
                    validationError(
                        PathHelper.path(
                            PathHelper.index(0),
                            PathHelper.field("goalId")
                        ), invalidValue()
                    )
                )
            )
        )
    }
}
