package ru.yandex.direct.jobs.conversionsource

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.direct.core.entity.conversionsource.service.ConversionSourceService
import ru.yandex.direct.core.testing.data.defaultConversionSourceLink
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.metrika.client.model.response.CounterGoal

private const val CLIENT_ID = 2322L
private const val COUNTER_ID1 = 23333L
private const val GOAL_ID1 = 999999L
private const val COUNTER_ID2 = 23335L
private const val GOAL_ID2 = 999998L

@ExtendWith(MockitoExtension::class)
internal class SyncGoalsFromMetrikaJobTest {
    @Mock
    private lateinit var metrikaClient: MetrikaClient

    @Mock
    private lateinit var conversionSourceService: ConversionSourceService

    private lateinit var job: SyncGoalsFromMetrikaJob

    @BeforeEach
    fun setUp() {
        job = SyncGoalsFromMetrikaJob(metrikaClient, conversionSourceService, 1)
    }

    @Test
    fun empty() {
        whenever(conversionSourceService.getSourcesWithoutGoals()).thenReturn(emptySequence())

        job.execute()

        verify(conversionSourceService, never()).setConversionActionGoals(any())
    }

    @Test
    fun oneSource() {
        whenever(conversionSourceService.getSourcesWithoutGoals()).thenReturn(
            listOf(defaultConversionSourceLink(ClientId.fromLong(CLIENT_ID), COUNTER_ID1)).asSequence()
        )
        whenever(metrikaClient.getMassCountersGoalsFromMetrika(setOf(COUNTER_ID1.toInt()))).thenReturn(
            mapOf(COUNTER_ID1.toInt() to listOf(
                CounterGoal()
                    .withId(GOAL_ID1.toInt())
                    .withType(CounterGoal.Type.CDP_ORDER_IN_PROGRESS))
            )
        )
        job.execute()

        verify(conversionSourceService, times(1)).setConversionActionGoals(
            mapOf(COUNTER_ID1 to mapOf("IN_PROGRESS" to GOAL_ID1))
        )
    }

    @Test
    fun twoSources() {
        whenever(conversionSourceService.getSourcesWithoutGoals()).thenReturn(
            listOf(
                defaultConversionSourceLink(ClientId.fromLong(CLIENT_ID), COUNTER_ID1),
                defaultConversionSourceLink(ClientId.fromLong(CLIENT_ID), COUNTER_ID2),
            ).asSequence()
        )
        whenever(metrikaClient.getMassCountersGoalsFromMetrika(setOf(COUNTER_ID1.toInt()))).thenReturn(
            mapOf(COUNTER_ID1.toInt() to listOf(
                CounterGoal()
                    .withId(GOAL_ID1.toInt())
                    .withType(CounterGoal.Type.CDP_ORDER_IN_PROGRESS))
            )
        )
        whenever(metrikaClient.getMassCountersGoalsFromMetrika(setOf(COUNTER_ID2.toInt()))).thenReturn(
            mapOf(COUNTER_ID2.toInt() to listOf(
                CounterGoal()
                    .withId(GOAL_ID2.toInt())
                    .withType(CounterGoal.Type.CDP_ORDER_PAID))
            )
        )
        job.execute()

        verify(conversionSourceService, times(1)).setConversionActionGoals(
            mapOf(COUNTER_ID1 to mapOf("IN_PROGRESS" to GOAL_ID1))
        )
        verify(conversionSourceService, times(1)).setConversionActionGoals(
            mapOf(COUNTER_ID2 to mapOf("PAID" to GOAL_ID2))
        )
    }
}
