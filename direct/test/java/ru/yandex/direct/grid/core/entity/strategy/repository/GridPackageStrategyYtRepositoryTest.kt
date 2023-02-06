package ru.yandex.direct.grid.core.entity.strategy.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import ru.yandex.direct.grid.core.entity.model.GdiGoalConversion
import ru.yandex.direct.grid.core.entity.strategy.repository.GridPackageStrategyYtRepository.Filter
import ru.yandex.direct.grid.core.entity.strategy.repository.YtRowStatsUtils.Row
import ru.yandex.direct.grid.core.entity.strategy.repository.YtRowStatsUtils.rowset
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import java.time.LocalDate

internal class GridPackageStrategyYtRepositoryTest {

    private lateinit var ytDynamicSupport: YtDynamicSupport
    private lateinit var ytRepository: GridPackageStrategyYtRepository
    private val today = LocalDate.now()
    private val tomorrow = today.plusDays(1)
    private val strategyId1 = 1L
    private val strategyId2 = 2L
    private val goalId1 = 1L
    private val goalId2 = 2L
    private val defaultFilter = Filter(
        emptyList()
    )

    @Before
    fun setUp() {
        ytDynamicSupport = Mockito.mock(YtDynamicSupport::class.java)
        ytRepository = GridPackageStrategyYtRepository(ytDynamicSupport)
    }

    @Test
    fun `success get strategy entity stats on empty rowset`() {
        mockRowset(emptyList())
        val result = ytRepository.strategyEntityStats(defaultFilter)
        assertThat(result).isEmpty()
    }

    @Test
    fun `success get strategy goals stats on empty rowset`() {
        mockRowset(emptyList())
        val result = ytRepository.strategyGoalsConversions(defaultFilter, false)
        assertThat(result).isEmpty()
    }

    @Test
    fun `successfully get strategy goals`() {
        val rows = listOf(
            Row(strategyId1, tomorrow, 20, 50, goalId1),
            Row(strategyId1, tomorrow, 20, 50, goalId2),
            Row(strategyId2, tomorrow, 20, 50, goalId1),
            Row(strategyId2, tomorrow, 20, 50, goalId2),
        )
        mockRowset(rows)
        val result = ytRepository.strategyGoalsConversions(defaultFilter, false)
        val expected = arrayOf(
            GdiGoalConversion().withGoalId(goalId1).withGoals(20).withRevenue(50),
            GdiGoalConversion().withGoalId(goalId2).withGoals(20).withRevenue(50)
        )
        assertThat(result[strategyId1]).containsExactlyInAnyOrder(*expected)
        assertThat(result[strategyId2]).containsExactlyInAnyOrder(*expected)
    }

    private fun mockRowset(rows: List<Row>) {
        whenever(ytDynamicSupport.selectRows(any()))
            .thenReturn(rowset(rows))
    }
}
