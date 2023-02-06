package ru.yandex.direct.core.entity.conversionsource.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingInfo
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingStatus
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_IN_PROGRESS
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_PAID
import ru.yandex.direct.core.grut.api.utils.moscowDateTimeFromGrut
import ru.yandex.direct.core.testing.data.defaultConversionSource
import ru.yandex.direct.core.testing.data.defaultConversionSourceLink
import ru.yandex.direct.core.testing.data.defaultConversionSourceMetrika
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyTimeComparator
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val COUNTER_ID = 23222L

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ConversionSourceServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()

        val counterIds = (0..10).map { COUNTER_ID + it }
        metrikaClientStub.addUserCounters(
            clientInfo.uid, counterIds.map {
                CounterInfoDirect().withId(it.toInt())
                    .withName("some counter name")
                    .withSitePath("some domain")
            }
        )
    }

    @Test
    fun add() {
        val lastScheduledTime = LocalDateTime.now()
        val conversionSource = defaultConversionSource(clientInfo.clientId!!, COUNTER_ID).copy(
            processingInfo = ProcessingInfo(
                processingStatus = ProcessingStatus.SUCCESS,
                lastScheduledTime = lastScheduledTime,
            )
        )
        val addResult = conversionSourceService.add(clientInfo.clientId!!, listOf(conversionSource))
        check(addResult.validationResult.flattenErrors().isEmpty()) {
            addResult.validationResult.flattenErrors().toString()
        }
        val id = addResult.result[0].result

        val sources = conversionSourceService.getByClientId(clientInfo.clientId!!)
        softly {
            assertThat(sources)
                .usingRecursiveComparison()
                .ignoringFields("processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
                .isEqualTo(listOf(conversionSource.copy(id = id!!)))
            assertThat(sources.firstOrNull())
                .extracting("processingInfo.lastScheduledTime")
                .isEqualTo(lastScheduledTime.truncatedTo(ChronoUnit.SECONDS))
        }
    }

    @Test
    fun update() {
        val origConversionSource = defaultConversionSource(clientInfo.clientId!!, COUNTER_ID).copy(
            processingInfo = ProcessingInfo(
                processingStatus = ProcessingStatus.SUCCESS,
                lastScheduledTime = LocalDateTime.now()
            )
        )
        val origId = conversionSourceService.add(clientInfo.clientId!!, listOf(origConversionSource)).result[0].result

        val newConversionSource = origConversionSource.copy(
            id = origId,
            name = "new name",
            processingInfo = ProcessingInfo(
                processingStatus = ProcessingStatus.ERROR,
                lastScheduledTime = LocalDateTime.now()
            )
        )
        val updateResult = conversionSourceService.update(clientInfo.clientId!!, listOf(newConversionSource))
        check(updateResult.validationResult.flattenErrors().isEmpty()) {
            updateResult.validationResult.flattenErrors().toString()
        }

        val sources = conversionSourceService.getByClientId(clientInfo.clientId!!)
        softly {
            assertThat(sources)
                .usingRecursiveComparison()
                .ignoringFields("processingInfo")
                .withComparatorForType(approximatelyTimeComparator(1), LocalDateTime::class.java)
                .isEqualTo(listOf(newConversionSource))
            assertThat(sources.firstOrNull())
                .hasFieldOrPropertyWithValue("processingInfo.processingStatus", ProcessingStatus.SUCCESS)
                .hasFieldOrPropertyWithValue("processingInfo.lastScheduledTime", moscowDateTimeFromGrut(0))
        }
    }

    @Test
    fun updateConversionActionGoals() {
        val origConversionSource = defaultConversionSourceLink(clientInfo.clientId!!, COUNTER_ID)
        val origId = conversionSourceService.add(clientInfo.clientId!!, listOf(origConversionSource)).result[0].result

        val inProgressGoalId = 22222L
        val paidGoalId = 33333L
        conversionSourceService.setConversionActionGoals(mapOf(
            origId to mapOf(
                ACTION_NAME_IN_PROGRESS to inProgressGoalId,
                ACTION_NAME_PAID to paidGoalId,
            )
        ))

        val sources = conversionSourceService.getByIds(listOf(origId))
        softly {
            assertThat(sources[0].name).`as`("name is unchanged").isEqualTo(origConversionSource.name)
            assertThat(sources[0].actions).extracting("goalId").containsOnly(paidGoalId, inProgressGoalId)
        }
    }

    @Test
    fun remove_success() {
        val conversionSource = defaultConversionSource(clientInfo.clientId!!, COUNTER_ID)
        val removeResult = conversionSourceService.add(clientInfo.clientId!!, listOf(conversionSource))
        check(removeResult.validationResult.flattenErrors().isEmpty()) {
            removeResult.validationResult.flattenErrors().toString()
        }

        val id = removeResult.result[0].result

        val vr = conversionSourceService.remove(clientInfo.clientId!!, listOf(id)).validationResult
        val sources = conversionSourceService.getByClientId(clientInfo.clientId!!)
        softly {
            assertThat(vr.flattenErrors()).isEmpty()
            assertThat(sources).isEmpty()
        }
    }

    @Test
    fun remove_withValidationError() {
        val id = 10001L
        val vr = conversionSourceService.remove(clientInfo.clientId!!, listOf(id)).validationResult
        assertThat(vr.flattenErrors()).`is`(
            Conditions.matchedBy(
                org.hamcrest.Matchers.contains(
                    Matchers.validationError(
                        PathHelper.path(
                            PathHelper.index(0)
                        ), objectNotFound()
                    )
                )
            )
        )
    }

    @Test
    fun getSourcesWithoutGoals_FilterLinkSourcesWithoutGoals() {
        val metrikaSource = defaultConversionSourceMetrika(clientInfo.clientId!!, COUNTER_ID)
            .copy(processingInfo = ProcessingInfo(ProcessingStatus.SUCCESS))
        val linkSource = defaultConversionSourceLink(clientInfo.clientId!!, COUNTER_ID + 1)
            .copy(processingInfo = ProcessingInfo(ProcessingStatus.SUCCESS))
        val linkSourceWithGoalIds = defaultConversionSourceLink(clientInfo.clientId!!, COUNTER_ID + 2).copy(
            actions = listOf(
                ConversionAction(ACTION_NAME_IN_PROGRESS, 2222221L, value = null),
                ConversionAction(ACTION_NAME_PAID, 3333331L, value = null),
            ),
            processingInfo = ProcessingInfo(ProcessingStatus.SUCCESS)
        )

        val res = conversionSourceService.add(
            clientInfo.clientId!!, listOf(metrikaSource, linkSource, linkSourceWithGoalIds))
        check(!res.validationResult.hasAnyErrors()) {
            res.validationResult.flattenErrors().toString()
        }
        val ids = res.result.map { it.result }
        val metrikaSourceId = ids[0]
        val linkSourceId = ids[1]
        val linkSourceWithGoalIdId = ids[2]

        val sources = conversionSourceService.getSourcesWithoutGoals(1).toList()
        softly {
            assertThat(sources).extracting("id").doesNotContain(metrikaSourceId, linkSourceWithGoalIdId)
            assertThat(sources).extracting("id").contains(linkSourceId)
        }
    }

    @Test
    fun getSourcesWithoutGoals_ManySources() {
        val sources = (0..10)
            .map {
                defaultConversionSourceLink(clientInfo.clientId!!, COUNTER_ID + it)
                    .copy(processingInfo = ProcessingInfo(ProcessingStatus.SUCCESS))
            }
        val res = conversionSourceService.add(clientInfo.clientId!!, sources)
        check(!res.validationResult.hasAnyErrors()) {
            res.validationResult.flattenErrors().toString()
        }
        val ids = res.result.map { it.result }

        val actualSources = conversionSourceService.getSourcesWithoutGoals(1).toList()
        assertThat(actualSources).extracting("id").contains(*ids.toTypedArray())
    }
}

