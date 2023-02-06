package ru.yandex.market.wms.constraints.controller

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class JobMonitoringControllerTest : ConstraintsIntegrationTest() {

    @Test
    fun `getFailedJobs - OK`() {
        getFailedJobsAndExpect("0;OK")
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/job1-failed.xml")
    fun `getFailedJobs - warn job1 failed`() {
        getFailedJobsAndExpect("1;Job job1 failed at 18-10-2021 12:00:00")
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/job1-failed-twice.xml")
    fun `getFailedJobs - warn job1 failed twice`() {
        getFailedJobsAndExpect(
            "1;Job job1 failed at 18-10-2021 13:00:00, Job job1 failed at 18-10-2021 12:00:00"
        )
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/job1-and-job2-failed.xml")
    fun `getFailedJobs - warn job1 and job2 failed`() {
        getFailedJobsAndExpect(
            "1;Job job1 failed at 18-10-2021 12:00:00, Job job2 failed at 18-10-2021 12:30:00"
        )
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/job1-failed-crit.xml")
    fun `getFailedJobs - crit job1 failed`() {
        getFailedJobsAndExpect(
            "2;Job job1 failed at 18-10-2021 13:00:00, " +
                "Job job1 failed at 18-10-2021 12:00:00, " +
                "Job job1 failed at 18-10-2021 11:00:00"
        )
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/failed-executions/failures-exist/before.xml")
    fun `checkFailedJob returns information about failed job executions`() {
        checkFailedJob("failures-exist")
    }

    @Test
    @DatabaseSetup("/controller/job-monitoring/failed-executions/without-failures/before.xml")
    fun `checkFailedJob returns OK when there are no failures`() {
        checkFailedJob("without-failures")
    }

    private fun checkFailedJob(testCase: String) {
        mockMvc.perform(
            get("/job/monitoring/failedJobs/job1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                content()
                    .string(
                        FileContentUtils.getFileContent("controller/job-monitoring/failed-executions/$testCase/response.txt")
                    )
            )
    }

    private fun getFailedJobsAndExpect(expectedResponse: String) {
        mockMvc.perform(get("/job/monitoring/failedJobs"))
            .andExpect(status().isOk)
            .andExpect(content().string(expectedResponse))
            .andReturn()
    }
}
