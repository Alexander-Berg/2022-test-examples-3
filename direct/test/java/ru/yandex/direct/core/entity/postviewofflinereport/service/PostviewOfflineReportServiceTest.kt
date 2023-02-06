package ru.yandex.direct.core.entity.postviewofflinereport.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.POSTVIEW_OFFLINE_REPORT
import ru.yandex.direct.core.entity.postviewofflinereport.model.PostviewOfflineReportJobResult
import ru.yandex.direct.core.entity.postviewofflinereport.repository.PostViewOfflineReportYtRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@CoreTest
@RunWith(SpringRunner::class)
class PostviewOfflineReportServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var shardHelper: ShardHelper

    private lateinit var postviewOfflineReportYtRepository: PostViewOfflineReportYtRepository
    private lateinit var postviewOfflineReportService: PostviewOfflineReportService

    lateinit var userInfo: UserInfo
    lateinit var clientInfo: ClientInfo
    private lateinit var cids: Set<Long>
    private lateinit var cids2: Set<Long>
    private lateinit var dateFrom: LocalDate
    private lateinit var dateTo: LocalDate

    private val reportUrl = "https://report.url"

    @Before
    fun init() {
        dateFrom = LocalDate.now().minusDays(30)
        dateTo = LocalDate.now().minusDays(20)
        steps.dbQueueSteps().registerJobType(POSTVIEW_OFFLINE_REPORT)
        userInfo = steps.userSteps().createDefaultUser()
        clientInfo = steps.clientSteps().createDefaultClient()
        cids = listOf(1, 2).map { steps.campaignSteps().createActiveCampaign(clientInfo).campaignId }.toSet()
        cids2 = cids.plus(steps.campaignSteps().createActiveCampaign(clientInfo).campaignId)
        postviewOfflineReportYtRepository = mock(PostViewOfflineReportYtRepository::class.java)
        postviewOfflineReportService =
            PostviewOfflineReportService(dbQueueRepository, shardHelper, postviewOfflineReportYtRepository)
    }

    @Test
    fun createReport_Success() {
        whenever(postviewOfflineReportYtRepository.createTask(anyLong(), any())).thenReturn(true)
        val report =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids, dateFrom, dateTo)
        assertSoftly {
            assertThat(report).isNotNull
            assertThat(report.id).isGreaterThan(0)
            assertThat(report.clientId).isEqualTo(clientInfo.clientId!!)
            assertThat(report.status).isEqualTo(DbQueueJobStatus.NEW)
            assertThat(report.campaignIds).isEqualTo(cids)
            assertThat(report.dateFrom).isEqualTo(dateFrom)
            assertThat(report.dateTo).isEqualTo(dateTo)
        }
    }

    @Test
    fun createReport_SaveToYtFailed() {
        whenever(postviewOfflineReportYtRepository.createTask(anyLong(), any())).thenReturn(false)
        val report =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids, dateFrom, dateTo)
        assertSoftly {
            assertThat(report).isNotNull
            assertThat(report.id).isGreaterThan(0)
            assertThat(report.clientId).isEqualTo(clientInfo.clientId!!)
            assertThat(report.status).isEqualTo(DbQueueJobStatus.REVOKED)
            assertThat(report.campaignIds).isEqualTo(cids)
            assertThat(report.dateFrom).isEqualTo(dateFrom)
            assertThat(report.dateTo).isEqualTo(dateTo)
        }
    }

    @Test
    fun getReportTest_Success() {
        whenever(postviewOfflineReportYtRepository.createTask(anyLong(), any())).thenReturn(true)
        val createdReport =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids, dateFrom, dateTo)
        assumeThat(createdReport.status, `is`(DbQueueJobStatus.NEW))

        val receivedReport = postviewOfflineReportService.getReport(clientInfo.clientId!!, createdReport.id)
        assertSoftly{
            assertThat(receivedReport?.id).isEqualTo(createdReport.id)
            assertThat(receivedReport?.campaignIds).isEqualTo(cids)
            assertThat(receivedReport?.dateFrom).isEqualTo(dateFrom)
            assertThat(receivedReport?.dateTo).isEqualTo(dateTo)
            assertThat(receivedReport?.status).isEqualTo(DbQueueJobStatus.NEW)
            assertThat(receivedReport?.createTime).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.MINUTES))
            assertThat(receivedReport?.finishTime).isNull()
            assertThat(receivedReport?.error).isNull()
        }
    }

    @Test
    fun getReportListTest_Success() {
        whenever(postviewOfflineReportYtRepository.createTask(anyLong(), any())).thenReturn(true)
        val report1 =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids, dateFrom, dateTo)
        assumeThat(report1.status, `is`(DbQueueJobStatus.NEW))

        val report2 =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids2, dateFrom, dateTo)
        assumeThat(report2.status, `is`(DbQueueJobStatus.NEW))
        val shard = shardHelper.getShardByClientId(clientInfo.clientId!!)
        val job = dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, report2.id)
        assumeThat(job, notNullValue())
        dbQueueRepository.markJobFinished(shard, job!!, PostviewOfflineReportJobResult(reportUrl, null))

        val reports = postviewOfflineReportService.getReportList(clientInfo.clientId!!).associateBy { it.id }
        assertSoftly{
            assertThat(reports.size).isEqualTo(2)
            assertThat(reports.keys).isEqualTo(setOf(report1.id, report2.id))
            val actualReport1 = reports[report1.id]
            assertThat(actualReport1?.campaignIds).isEqualTo(cids)
            assertThat(actualReport1?.dateFrom).isEqualTo(dateFrom)
            assertThat(actualReport1?.dateTo).isEqualTo(dateTo)
            assertThat(actualReport1?.status).isEqualTo(DbQueueJobStatus.NEW)
            assertThat(actualReport1?.createTime).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.MINUTES))
            assertThat(actualReport1?.finishTime).isNull()
            assertThat(actualReport1?.error).isNull()
            val actualReport2 = reports[report2.id]
            assertThat(actualReport2?.campaignIds).isEqualTo(cids2)
            assertThat(actualReport2?.dateFrom).isEqualTo(dateFrom)
            assertThat(actualReport2?.dateTo).isEqualTo(dateTo)
            assertThat(actualReport2?.status).isEqualTo(DbQueueJobStatus.FINISHED)
            assertThat(actualReport1?.createTime).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.MINUTES))
            assertThat(actualReport2?.finishTime).isNull()
            assertThat(actualReport2?.url).isEqualTo(reportUrl)
            assertThat(actualReport2?.error).isNull()
        }
    }

    @Test
    fun deleteReportTest_Success() {
        whenever(postviewOfflineReportYtRepository.createTask(anyLong(), any())).thenReturn(true)
        val report =
            postviewOfflineReportService.createReport(userInfo.uid, clientInfo.clientId!!, cids, dateFrom, dateTo)
        assumeThat(report.status, `is`(DbQueueJobStatus.NEW))

        val shard = shardHelper.getShardByClientId(clientInfo.clientId!!)
        val job = dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, report.id)
        assumeThat(job, notNullValue())
        dbQueueRepository.markJobFinished(shard, job!!, PostviewOfflineReportJobResult(reportUrl, null))

        whenever(postviewOfflineReportYtRepository.deleteTask(anyLong())).thenReturn(true)
        val result = postviewOfflineReportService.deleteReport(clientInfo.clientId!!, report.id)
        assertSoftly{
            assertThat(result).isTrue
            assertThat(dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, report.id)).isNull()
        }
    }
}
