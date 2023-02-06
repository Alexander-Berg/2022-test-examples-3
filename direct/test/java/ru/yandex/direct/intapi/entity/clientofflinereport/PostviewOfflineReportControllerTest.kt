package ru.yandex.direct.intapi.entity.clientofflinereport

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.POSTVIEW_OFFLINE_REPORT
import ru.yandex.direct.core.entity.postviewofflinereport.model.PostviewOfflineReport
import ru.yandex.direct.core.entity.postviewofflinereport.model.PostviewOfflineReportJobResult
import ru.yandex.direct.core.entity.postviewofflinereport.repository.PostViewOfflineReportYtRepository
import ru.yandex.direct.core.entity.postviewofflinereport.service.PostviewOfflineReportService
import ru.yandex.direct.core.entity.postviewofflinereport.validation.PostviewOfflineReportConstraints.Companion.MAX_DAYS_BEFORE_TODAY
import ru.yandex.direct.core.entity.postviewofflinereport.validation.PostviewOfflineReportConstraints.Companion.MAX_PROCESSING_REPORTS_COUNT
import ru.yandex.direct.core.entity.postviewofflinereport.validation.PostviewOfflineReportConstraints.Companion.MIN_DAYS_BEFORE_TODAY
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbqueue.model.DbQueueJobStatus
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.intapi.IntApiException
import ru.yandex.direct.intapi.configuration.IntApiTest
import ru.yandex.direct.intapi.entity.clientofflinereport.PostviewOfflineReportController.Companion.convertReportToReportData
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.utils.JsonUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@IntApiTest
@RunWith(SpringRunner::class)
class PostviewOfflineReportControllerTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var shardHelper: ShardHelper

    private lateinit var postviewOfflineReportService: PostviewOfflineReportService
    private lateinit var postviewOfflineReportYtRepository: PostViewOfflineReportYtRepository

    lateinit var userInfo: UserInfo
    lateinit var clientInfo: ClientInfo
    lateinit var controller: PostviewOfflineReportController
    private lateinit var cids: Set<Long>
    private lateinit var cids2: Set<Long>
    private lateinit var dateFrom: LocalDate
    private lateinit var dateTo: LocalDate
    private lateinit var createRequest: PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest

    @Before
    fun init() {
        dateFrom = LocalDate.now().minusDays(MAX_DAYS_BEFORE_TODAY - 1L)
        dateTo = LocalDate.now().minusDays(MIN_DAYS_BEFORE_TODAY + 1L)
        steps.dbQueueSteps().registerJobType(POSTVIEW_OFFLINE_REPORT)
        userInfo = steps.userSteps().createDefaultUser()
        clientInfo = steps.clientSteps().createDefaultClient()
        cids = listOf(1, 2).map { steps.campaignSteps().createActiveCampaign(clientInfo).campaignId }.toSet()
        cids2 = cids.plus(steps.campaignSteps().createActiveCampaign(clientInfo).campaignId)
        postviewOfflineReportYtRepository = Mockito.mock(PostViewOfflineReportYtRepository::class.java)
        whenever(postviewOfflineReportYtRepository.createTask(Mockito.anyLong(), any())).thenReturn(true)
        whenever(postviewOfflineReportYtRepository.deleteTask(Mockito.anyLong())).thenReturn(true)
        postviewOfflineReportService =
            PostviewOfflineReportService(dbQueueRepository, shardHelper, postviewOfflineReportYtRepository)
        controller = PostviewOfflineReportController(postviewOfflineReportService, shardHelper)
        createRequest = PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
            userInfo.uid, cids, dateFrom, dateTo
        )
    }

    @Test
    fun validatePostviewOfflineReportTest_Success() {
        val response = controller.validatePostviewOfflineReport(createRequest)
        assertThat(response.isSuccessful).isTrue
    }

    @Test
    fun validatePostviewOfflineReportTest_FailedProcessedReportsLimitReached() {
        val reportIds = mutableListOf<Long>()
        (1L..MAX_PROCESSING_REPORTS_COUNT + 2L).forEach {
            val request = PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids, dateFrom.plusDays(it), dateTo
            )
            val result = controller.createPostviewOfflineReport(request)
            assumeThat(result.isSuccessful, `is`(true))
            reportIds.add(result.reportId!!)
        }
        markReportFinished(clientInfo.clientId!!, reportIds[0])
        markReportRevoked(clientInfo.clientId!!, reportIds[1])
        markReportFailed(clientInfo.clientId!!, reportIds[2])

        val request = PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
            userInfo.uid, cids, dateFrom.plusDays(MAX_PROCESSING_REPORTS_COUNT + 3L), dateTo
        )
        assumeThat(controller.validatePostviewOfflineReport(request).isSuccessful, `is`(true))
        assumeThat(controller.createPostviewOfflineReport(request).isSuccessful, `is`(true))

        val exception = assertThrows<IntApiException> {
            controller.validatePostviewOfflineReport(createRequest)
        }
        assertSoftly {
            it.assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            it.assertThat(JsonUtils.fromJson(exception.localizedMessage)["message"].textValue())
                .isEqualTo("processing_reports_limit_reached")
        }
    }

    private fun markReportFinished(clientId: ClientId, reportId: Long) {
        val shard = shardHelper.getShardByClientId(clientId)
        dbQueueRepository.markJobFinished(
            shard,
            dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, reportId)!!,
            PostviewOfflineReportJobResult("https://ya.ru/report_url", null)
        )
    }

    private fun markReportFailed(clientId: ClientId, reportId: Long) {
        val shard = shardHelper.getShardByClientId(clientId)
        dbQueueRepository.markJobFailedPermanently(
            shard,
            dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, reportId)!!,
            PostviewOfflineReportJobResult(null, "job failed")
        )
    }

    private fun markReportRevoked(clientId: ClientId, reportId: Long) {
        val shard = shardHelper.getShardByClientId(clientId)
        dbQueueRepository.markJobRevoked(
            shard,
            dbQueueRepository.findJobById(shard, POSTVIEW_OFFLINE_REPORT, reportId)!!,
            PostviewOfflineReportJobResult(null, "process failed")
        )
    }

    @Test
    fun validatePostviewOfflineReportTest_InvalidDateFrom() {
        val exception = assertThrows<IntApiException> {
            controller.validatePostviewOfflineReport(
                PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                    userInfo.uid, cids, LocalDate.now().minusDays(MAX_DAYS_BEFORE_TODAY + 1L), dateTo
                )
            )
        }
        assertSoftly {
            it.assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            it.assertThat(JsonUtils.fromJson(exception.localizedMessage)["message"].textValue())
                .isEqualTo("invalid_date_from")
        }
    }

    @Test
    fun validatePostviewOfflineReportTest_InvalidDateTo() {
        val exception = assertThrows<IntApiException> {
            controller.validatePostviewOfflineReport(
                PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                    userInfo.uid, cids, dateFrom, LocalDate.now()
                )
            )
        }
        assertSoftly {
            it.assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            it.assertThat(JsonUtils.fromJson(exception.localizedMessage)["message"].textValue())
                .isEqualTo("invalid_date_to")
        }
    }

    @Test
    fun validatePostviewOfflineReportTest_DateToBeforeDateFrom() {
        val exception = assertThrows<IntApiException> {
            controller.validatePostviewOfflineReport(
                PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                    userInfo.uid, cids, dateTo, dateFrom
                )
            )
        }
        assertSoftly {
            it.assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            it.assertThat(JsonUtils.fromJson(exception.localizedMessage)["message"].textValue())
                .isEqualTo("date_to_before_date_from")
        }
    }

    @Test
    fun createPostviewOfflineReportTest_SuccessTwoReports() {
        val response = controller.createPostviewOfflineReport(createRequest)
        assumeThat(response.isSuccessful, `is`(true))
        val response2 = controller.createPostviewOfflineReport(
            PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids2, dateFrom, dateTo
            )
        )
        assertSoftly {
            it.assertThat(response2.isSuccessful).isTrue
            it.assertThat(response2.reportId).isNotEqualTo(response.reportId)
            it.assertThat(response2.message).isNull()
        }
    }

    @Test
    fun createPostviewOfflineReportTest_FailedSameReport() {
        val response = controller.createPostviewOfflineReport(createRequest)
        assumeThat(response.isSuccessful, `is`(true))
        val exception = assertThrows<IntApiException> {
            controller.createPostviewOfflineReport(createRequest)
        }
        assertSoftly {
            it.assertThat(exception.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
            it.assertThat(JsonUtils.fromJson(exception.localizedMessage)["message"].textValue())
                .isEqualTo("report_exists")
        }
    }

    @Test
    fun deletePostviewOfflineReportTest_Success() {
        val response = controller.createPostviewOfflineReport(createRequest)
        assumeThat(response.isSuccessful, `is`(true))
        markReportFinished(clientInfo.clientId!!, response.reportId!!)
        val deleteResponse = controller.deletePostviewOfflineReport(
            PostviewOfflineReportController.Companion.DeletePostviewOfflineReportRequest(
                clientInfo.clientId!!, response.reportId!!
            )
        )
        assertThat(deleteResponse.isSuccessful).isTrue
    }

    @Test
    fun deletePostviewOfflineReportTest_Fail() {
        val exception = assertThrows<IntApiException> {
            controller.deletePostviewOfflineReport(
                PostviewOfflineReportController.Companion.DeletePostviewOfflineReportRequest(
                    clientInfo.clientId!!, 0
                )
            )
        }
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun deletePostviewOfflineReportTest_FailWrongClientId() {
        val response = controller.createPostviewOfflineReport(createRequest)
        assumeThat(response.isSuccessful, `is`(true))
        val exception = assertThrows<IntApiException> {
            controller.deletePostviewOfflineReport(
                PostviewOfflineReportController.Companion.DeletePostviewOfflineReportRequest(
                    userInfo.clientId, response.reportId!!
                )
            )
        }
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun getPostviewOfflineReportListTest_Success() {
        val result1 = controller.createPostviewOfflineReport(createRequest)
        assumeThat(result1.isSuccessful, `is`(true))
        val report1 =  postviewOfflineReportService.getReport(clientInfo.clientId!!, result1.reportId!!)!!
        assumeThat(report1.status, `is`(DbQueueJobStatus.NEW))

        val result2 = controller.createPostviewOfflineReport(
            PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids, dateFrom.minusDays(1), dateTo.minusDays(1)
            )
        )
        assumeThat(result2.isSuccessful, `is`(true))
        markReportRevoked(clientInfo.clientId!!, result2.reportId!!)
        val report2 = postviewOfflineReportService.getReport(clientInfo.clientId!!, result2.reportId!!)!!
        assumeThat(report2.status, `is`(DbQueueJobStatus.REVOKED))

        val result3 = controller.createPostviewOfflineReport(
            PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids, dateFrom.minusDays(2), dateTo.minusDays(2)
            )
        )
        assumeThat(result3.isSuccessful, `is`(true))
        markReportFinished(clientInfo.clientId!!, result3.reportId!!)
        val report3 = postviewOfflineReportService.getReport(clientInfo.clientId!!, result3.reportId!!)!!
        assumeThat(report3.status, `is`(DbQueueJobStatus.FINISHED))

        val result4 = controller.createPostviewOfflineReport(
            PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids, dateFrom.minusDays(3), dateTo.minusDays(3)
            )
        )
        assumeThat(result4.isSuccessful, `is`(true))
        markReportFailed(clientInfo.clientId!!, result4.reportId!!)
        val report4 = postviewOfflineReportService.getReport(clientInfo.clientId!!, result4.reportId!!)!!
        assumeThat(report4.status, `is`(DbQueueJobStatus.FAILED))

        val getResponse = controller.getPostviewOfflineReportList(clientInfo.clientId!!.asLong())
        assertThat(getResponse.reports).containsExactlyInAnyOrderElementsOf(
            setOf(report1, report2, report3, report4).map { convertReportToReportData(it) }
        )
    }

    @Test
    fun getPostviewOfflineReportListTest_EmptyList() {
        val getResponse = controller.getPostviewOfflineReportList(clientInfo.clientId!!.asLong())
        assertThat(getResponse.reports).isEmpty()
    }

    @Test
    fun getPostviewOfflineReportTest_Success() {
        val result1 = controller.createPostviewOfflineReport(createRequest)
        assumeThat(result1.isSuccessful, `is`(true))
        val report1 = postviewOfflineReportService.getReport(clientInfo.clientId!!, result1.reportId!!)!!

        val result2 = controller.createPostviewOfflineReport(
            PostviewOfflineReportController.Companion.CreatePostviewOfflineReportRequest(
                userInfo.uid, cids, dateFrom.minusDays(1), dateTo.minusDays(1)
            )
        )
        assumeThat(result2.isSuccessful, `is`(true))

        val getResponse = controller.getPostviewOfflineReport(clientInfo.clientId!!.asLong(), report1.id)
        assertThat(getResponse).isEqualTo(convertReportToReportData(report1))
    }

    @Test
    fun getPostviewOfflineReportTest_NotFound() {
        val exception = assertThrows<IntApiException> {
            controller.getPostviewOfflineReport(clientInfo.clientId!!.asLong(), 0L)
        }
        assertThat(exception.httpStatus).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun convertReportToReportDataTest() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val report = PostviewOfflineReport(
            clientId = ClientId.fromLong(1L),
            id = 2L,
            campaignIds = setOf(3L,4L,5L),
            dateFrom = LocalDate.parse("2022-07-10"),
            dateTo = LocalDate.parse("2022-07-11"),
            status = DbQueueJobStatus.NEW,
            createTime = LocalDateTime.parse("2022-07-12 10:07:50", formatter),
            finishTime = LocalDateTime.parse("2022-07-13 21:42:10", formatter),
            url = "test_report.xlsx",
            error = "test error"
        )
        assertThat(convertReportToReportData(report)).isEqualTo(
            PostviewOfflineReportController.Companion.PostviewOfflineReportData(
                reportId = 2L,
                cids = setOf(3L,4L,5L),
                dateFrom = "2022-07-10",
                dateTo = "2022-07-11",
                status = DbQueueJobStatus.NEW,
                createTime = "2022-07-12 10:07:50",
                finishTime = "2022-07-13 21:42:10",
                reportUrl = "test_report.xlsx",
                error = "test error"
            )
        )
    }
}
