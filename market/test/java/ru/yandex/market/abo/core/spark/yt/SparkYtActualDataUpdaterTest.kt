package ru.yandex.market.abo.core.spark.yt

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import ru.yandex.market.abo.core.CoreCounter
import ru.yandex.market.abo.core.yt.YtService
import ru.yandex.market.abo.core.yt.transfermanager.YtReplicator
import ru.yandex.market.abo.util.yql.YqlScheduler
import ru.yandex.market.util.db.ConfigurationService

class SparkYtActualDataUpdaterTest {
    private val yqlScheduler: YqlScheduler = mock()

    private val ytService: YtService = mock {
        on { isTableUnlocked(any()) } doReturn true
        on { list(any()) } doReturn listOf(oldTableName, actualTableName)
    }

    private val sparkYtReplicator: YtReplicator = mock()

    private val coreCounterService: ConfigurationService = mock {
        on { getValue(CoreCounter.LAST_SPARK_YT_UPDATE.name) } doReturn actualTableName
    }

    private val resourceData: Resource = ClassPathResource("yql/spark/update_spark_data.yql")
    private val resourceRisks: Resource = ClassPathResource("yql/spark/update_spark_risks.yql")

    private val sparkYtActualDataUpdater = SparkYtActualDataUpdater(
        yqlScheduler = yqlScheduler,
        ytService = ytService,
        sparkYtReplicator = sparkYtReplicator,
        coreCounterService = coreCounterService,
        sparkDataUpdateQueryFile = resourceData,
        sparkRisksUpdateQueryFile = resourceRisks,
        sparkSaveDataPath = "//home/market/development/abo/spark/actual_spark_data",
        sparkSaveRisksPath = "//home/market/development/abo/spark/actual_spark_risks",
    )

    @Test
    fun `no tables found`() {
        whenever(ytService.list(any())).doReturn(listOf())

        assertThrows<Exception> { sparkYtActualDataUpdater.updateSparkDataIfNeeded() }
    }

    @Test
    fun `table locked`() {
        whenever(ytService.isTableUnlocked(any())).doReturn(false)

        sparkYtActualDataUpdater.updateSparkDataIfNeeded()

        verify(ytService, never()).list(any())
    }

    @Test
    fun `tables are actual`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE.name)).doReturn(actualTableName)
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE_RELATED.name)).doReturn(actualTableName)

        sparkYtActualDataUpdater.updateSparkDataIfNeeded()

        verify(yqlScheduler, never()).schedule(any(), any())
        verify(sparkYtReplicator, never()).replicate(any(), any())
    }

    @Test
    fun `related tables not actual`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE.name)).doReturn(actualTableName)
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE_RELATED.name)).doReturn(oldTableName)

        sparkYtActualDataUpdater.updateSparkDataIfNeeded()

        verify(coreCounterService, times(1))
            .mergeValue(CoreCounter.LAST_SPARK_YT_UPDATE_RELATED.name, actualTableName)
    }

    @Test
    fun `Hahn not actual`() {
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE.name)).doReturn(oldTableName)
        whenever(coreCounterService.getValue(CoreCounter.LAST_SPARK_YT_UPDATE_RELATED.name)).doReturn(oldTableName)

        sparkYtActualDataUpdater.updateSparkDataIfNeeded()

        verify(yqlScheduler, times(1)).schedule(any(), any())
        verify(coreCounterService, times(1))
            .mergeValue(CoreCounter.LAST_SPARK_YT_UPDATE.name, actualTableName)
        verify(sparkYtReplicator, never()).replicate(any(), any())
    }

    companion object {
        const val actualTableName = "2021-10-20"
        const val oldTableName = "2021-10-19"
    }
}
