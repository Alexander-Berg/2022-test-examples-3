package ru.yandex.direct.grid.core.entity.strategy.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.jooq.Select
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.mock
import ru.yandex.direct.core.entity.container.LocalDateRange
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.liveresource.LiveResourceFactory
import ru.yandex.direct.test.utils.QueryUtils
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import java.time.LocalDate

class GridPackageStrategyYtRepositoryQueryTest {
    private lateinit var ytDynamicSupport: YtDynamicSupport

    private lateinit var ytRepository: GridPackageStrategyYtRepository

    private lateinit var queryArgumentCaptor: ArgumentCaptor<Select<*>>

    @Before
    fun setUp() {
        ytDynamicSupport = mock(YtDynamicSupport::class.java)
        whenever(ytDynamicSupport.selectRows(any()))
            .thenReturn(RowsetBuilder().build())
        queryArgumentCaptor = ArgumentCaptor.forClass(Select::class.java)
        ytRepository = GridPackageStrategyYtRepository(ytDynamicSupport)
    }

    @Test
    fun `strategy goals conversions`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(
            tuples,
            mapOf(
                2L to listOf(10, 11),
                4L to listOf(12, 13)
            )
        )
        ytRepository.strategyGoalsConversions(filter, false)

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery("strategy_goals_conversions.query", queryArgumentCaptor.value.toString())
    }

    @Test
    fun `strategy goals conversions only assigned`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(
            tuples,
            mapOf(
                2L to listOf(10, 11),
                4L to listOf(12, 13)
            )
        )
        ytRepository.strategyGoalsConversions(filter, true)

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery("strategy_only_assigned_goals_conversions.query", queryArgumentCaptor.value.toString())
    }

    @Test
    fun `strategy goals conversions only assigned with available goals`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(
            tuples,
            mapOf(
                2L to listOf(10, 11),
                4L to listOf(12, 13)
            )
        )
        ytRepository.strategyGoalsConversions(filter, true, setOf(7, 8))

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery(
            "strategy_only_assigned_goals_conversions_with_available_goals.query",
            queryArgumentCaptor.value.toString()
        )
    }

    @Test
    fun `strategy goals conversions with available goals`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(
            tuples,
            mapOf(
                2L to listOf(10, 11),
                4L to listOf(12, 13)
            )
        )
        ytRepository.strategyGoalsConversions(filter, false, setOf(10, 12))

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery("strategy_goals_conversions_with_available_goals.query", queryArgumentCaptor.value.toString())
    }

    @Test
    fun `strategy entity stats`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(tuples)
        ytRepository.strategyEntityStats(filter)

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery("strategy_entity_stats.query", queryArgumentCaptor.value.toString())
    }

    @Test
    fun `strategy entity stats (ignore goals_ids)`() {
        val tuples = listOf(
            GridPackageStrategyYtRepository.FilteringTuple(1, 1000, 2),
            GridPackageStrategyYtRepository.FilteringTuple(3, 2000, 4),
            GridPackageStrategyYtRepository.FilteringTuple(1, 3000, 2),
        )
        val filter = GridPackageStrategyYtRepository.Filter(
            tuples,
            mapOf(
                2L to listOf(10, 11),
                4L to listOf(12, 13)
            )
        )
        ytRepository.strategyEntityStats(filter)

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())

        checkQuery("strategy_entity_stats.query", queryArgumentCaptor.value.toString())
    }

    @Test
    fun `get filtering tuples`() {
        val localDate = LocalDate.parse("2022-07-18")
        val cidToRange = mapOf(
            1L to LocalDateRange()
                .withFromInclusive(localDate)
                .withToInclusive(localDate.plusDays(1)),
            2L to LocalDateRange()
                .withFromInclusive(localDate.minusDays(1))
                .withToInclusive(localDate.plusDays(1))
        )
        val strategyIds = setOf(10L, 11L)

        ytRepository.getFilteringTuples(
            cidToRange,
            strategyIds
        )

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())
        checkQuery(
            "strategy_stats_get_filtering_tuples.query",
            queryArgumentCaptor.value.toString()
        )
    }

    private fun checkQuery(
        queryPathFile: String,
        query: String
    ) {
        val expectedQuery = LiveResourceFactory.get("classpath:///strategy/$queryPathFile").content
        QueryUtils.compareQueries(expectedQuery, query)
    }
}
