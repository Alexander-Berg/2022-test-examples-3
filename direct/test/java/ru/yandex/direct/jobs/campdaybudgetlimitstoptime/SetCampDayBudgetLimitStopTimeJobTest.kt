package ru.yandex.direct.jobs.campdaybudgetlimitstoptime

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.config.DirectConfig
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.sharding.ShardKey
import ru.yandex.direct.dbutil.sharding.ShardSupport
import ru.yandex.direct.jobs.campdaybudgetlimitstoptime.QueueRecordParserFactory.ORDER_ID_COLUMN_NAME
import ru.yandex.direct.jobs.campdaybudgetlimitstoptime.QueueRecordParserFactory.ROW_INDEX_COLUMN_NAME
import ru.yandex.direct.jobs.campdaybudgetlimitstoptime.QueueRecordParserFactory.STOP_TIME_COLUMN_NAME
import ru.yandex.direct.jobs.campdaybudgetlimitstoptime.SetCampDayBudgetLimitStopTimeJob.Companion.CONFIG_BRANCH_NAME
import ru.yandex.direct.jobs.campdaybudgetlimitstoptime.SetCampDayBudgetLimitStopTimeJob.Companion.LAST_READ_ROW_INDEX
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap
import ru.yandex.direct.scheduler.support.DirectShardedJob
import ru.yandex.direct.scheduler.support.PeriodicJobWrapper
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator
import ru.yandex.yt.ytclient.tables.ColumnValueType
import ru.yandex.yt.ytclient.tables.TableSchema
import ru.yandex.yt.ytclient.wire.UnversionedRow
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import ru.yandex.yt.ytclient.wire.UnversionedValue
import java.time.LocalDateTime
import java.time.Month
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

private const val SHARD: Int = ClientSteps.DEFAULT_SHARD

@JobsTest
@ExtendWith(SpringExtension::class)
internal class SetCampDayBudgetLimitStopTimeJobTest {
    private var directConfigMock = mock<DirectConfig>()

    private var jobConfigMock = mock<DirectConfig>()

    private var ppcPropertiesSupportMock = mock<PpcPropertiesSupport>()

    private var ppcPropertyLongMock = mock<PpcProperty<Long>>()

    private var ppcPropertyBoolMock = mock<PpcProperty<Boolean>>()

    private var ytProviderMock = mock<YtProvider>()

    private var ytOperatorMock = mock<YtDynamicOperator>()

    private var shardSupportMock = mock<ShardSupport>()

    private var campaignRepositoryMock = mock<CampaignRepository>()

    private var dayBudgetStopTimeDbHelperMock = mock<DayBudgetStopTimeDbHelper>()

    private lateinit var shardHelper: ShardHelper

    private lateinit var job: SetCampDayBudgetLimitStopTimeJob

    private lateinit var inputData: Map<Int, List<UnversionedRow>>

    @BeforeEach
    fun setUp() {
        shardHelper = ShardHelper(shardSupportMock)

        doReturn(jobConfigMock).`when`(directConfigMock).getBranch(CONFIG_BRANCH_NAME)
        doReturn("YT_LOCAL").`when`(jobConfigMock).getString(any())
        doReturn(ppcPropertyLongMock).`when`(ppcPropertiesSupportMock).get(LAST_READ_ROW_INDEX)
        doReturn(-1L).`when`(ppcPropertyLongMock).getOrDefault(any())
        doReturn(ppcPropertyBoolMock).`when`(ppcPropertiesSupportMock)
            .get(PpcPropertyNames.CAMP_DAY_BUDGET_LIMIT_STOP_TIME_JAVA_WRITING_ENABLED)
        doReturn(true).`when`(ppcPropertyBoolMock).getOrDefault(any())
        doReturn(ytOperatorMock).`when`(ytProviderMock).getDynamicOperator(any())
        doAnswer {
            UnversionedRowset(
                TableSchema.Builder()
                    .addKey(ROW_INDEX_COLUMN_NAME, ColumnValueType.INT64)
                    .addValue(ORDER_ID_COLUMN_NAME, ColumnValueType.INT64)
                    .addValue(STOP_TIME_COLUMN_NAME, ColumnValueType.INT64)
                    .build(),
                inputData.flatMap { (_, v) -> v }
            )
        }.`when`(ytOperatorMock).selectRows(any<String>(), any())
        doAnswer {
            // val list = it.getArgument<List<Any>>(1)
            inputData.flatMap { (k, v) -> List<Int>(v.size) { k } }
        }.`when`(shardSupportMock).getShards(any<ShardKey>(), any<List<Long>>())
        doAnswer {
            val list = it.getArgument<List<Long>>(1)
            list.associateWith(this::orderToCampaignId)
        }.`when`(campaignRepositoryMock).getCidsForOrderIds(any(), any<List<Long>>())

        job = SetCampDayBudgetLimitStopTimeJob(
            directConfigMock, ppcPropertiesSupportMock, ytProviderMock,
            shardHelper, campaignRepositoryMock, dayBudgetStopTimeDbHelperMock
        )
    }

    private fun orderToCampaignId(orderId: Long) = orderId + 1000L

    private fun setInputData(shard0Data: List<UnversionedRow>) {
        inputData = mapOf(0 to shard0Data)
    }

    private fun setInputData(shard0Data: List<UnversionedRow>, shard1Data: List<UnversionedRow>) {
        inputData = mapOf(0 to shard0Data, 1 to shard1Data)
    }

    private fun createUnversionedRow(rowIndex: Long, orderId: Long, stopTime: Long): UnversionedRow {
        return UnversionedRow(
            listOf(
                UnversionedValue(0, ColumnValueType.INT64, false, rowIndex),
                UnversionedValue(1, ColumnValueType.INT64, false, orderId),
                UnversionedValue(2, ColumnValueType.INT64, false, stopTime)
            )
        )
    }

    private fun executeJob() {
        val shardContext = TaskParametersMap.of(DirectShardedJob.SHARD_PARAM, SHARD.toString())
        PeriodicJobWrapper(job).execute(shardContext)
    }

    @Test
    fun execute_OnReadException_failure() {
        doThrow(RuntimeException()).`when`(ytOperatorMock).selectRows(any<String>(), any())
        Assertions.assertThatCode { executeJob() }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun execute_NoRows_success() {
        setInputData(emptyList())
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(0))
            .addUpdateStopTimeAndStatus(any(), any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(0))
            .addInsertStopHistory(any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(0))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(0))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }

    @Test
    fun execute_OneRowZeroStopTime_success() {
        val orderId = 100L
        setInputData(listOf(createUnversionedRow(0L, orderId, 0L)))
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .addUpdateStopTimeAndStatus(eq(0L), any(), eq(orderToCampaignId(orderId)))
        verify(dayBudgetStopTimeDbHelperMock, times(0))
            .addInsertStopHistory(any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(1))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }

    @Test
    fun execute_OneRowNonZeroStopTime_success() {
        val unixTime = 1625656228L
        val orderId = 100L
        val dateTime = LocalDateTime.of(2021, Month.JULY, 7, 14, 10, 28)
        setInputData(listOf(createUnversionedRow(0L, orderId, unixTime)))
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .addUpdateStopTimeAndStatus(eq(unixTime), eq(dateTime), eq(orderToCampaignId(orderId)))
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .addInsertStopHistory(eq(orderToCampaignId(orderId)), eq(dateTime))
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(1))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }

    @Test
    fun execute_ManyRows_success() {
        setInputData(
            listOf(
                createUnversionedRow(0L, 100L, 0L),
                createUnversionedRow(1L, 101L, 200001L),
                createUnversionedRow(2L, 102L, 200002L),
                createUnversionedRow(3L, 103L, 0L),
                createUnversionedRow(4L, 104L, 200003L)
            )
        )
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(5))
            .addUpdateStopTimeAndStatus(any(), any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(3))
            .addInsertStopHistory(any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(1))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(1))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }

    @Test
    fun execute_ManyShards_success() {
        setInputData(
            listOf(
                createUnversionedRow(0L, 100L, 200000L),
                createUnversionedRow(1L, 101L, 200001L),
                createUnversionedRow(2L, 102L, 200002L),
                createUnversionedRow(5L, 103L, 0L),
                createUnversionedRow(6L, 100L, 0L)
            ), listOf(
                createUnversionedRow(3L, 200L, 200000L),
                createUnversionedRow(4L, 201L, 200001L),
                createUnversionedRow(7L, 200L, 0L),
                createUnversionedRow(8L, 201L, 0L),
                createUnversionedRow(9L, 200L, 200003L)
            )
        )
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(10))
            .addUpdateStopTimeAndStatus(any(), any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(6))
            .addInsertStopHistory(any(), any())
        verify(dayBudgetStopTimeDbHelperMock, times(2))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(1))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }

    @Test
    fun execute_JavaWritingDisabled() {
        setInputData(
            listOf(
                createUnversionedRow(0L, 100L, 0L),
                createUnversionedRow(1L, 101L, 200001L),
                createUnversionedRow(2L, 102L, 200002L),
                createUnversionedRow(3L, 103L, 0L),
                createUnversionedRow(4L, 104L, 200003L)
            )
        )
        doReturn(false).`when`(ppcPropertyBoolMock).getOrDefault(any())
        Assertions.assertThatCode { executeJob() }
            .doesNotThrowAnyException()
        verify(dayBudgetStopTimeDbHelperMock, times(0))
            .executeTransactionBatch(any())
        verify(ytOperatorMock, times(1))
            .runRpcCommandWithTimeout(any<Supplier<CompletableFuture<Any>>>())
    }
}
