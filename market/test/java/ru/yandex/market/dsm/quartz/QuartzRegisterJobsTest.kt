package ru.yandex.market.dsm.quartz

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.yandex.market.dsm.core.test.AbstractDsmQuartzTest
import java.sql.ResultSet

class QuartzRegisterJobsTest : AbstractDsmQuartzTest() {

    val EXPECTED_REGISTERED_JOBS =
        setOf("tmsLogsCleanupExecutor", "tmsSyncSelfemployedWithTracker", "selfemployedStatusCheckExecutor")
    val SELECT_ALL_JOBS_QUERY = "select * from qrtz_job_details"

    @Autowired
    lateinit var jdbcNamedTemplate: NamedParameterJdbcTemplate

    @Test
    fun `Check registering Quartz jobs`() {
        //when
        val actualRegisteredJobs: List<String> = jdbcNamedTemplate.query(
            SELECT_ALL_JOBS_QUERY, emptyMap<String, String>()
        ) { rs: ResultSet, i: Int -> rs.getString("JOB_NAME") }

        //then
        assertThat(actualRegisteredJobs).containsExactlyInAnyOrderElementsOf(EXPECTED_REGISTERED_JOBS)
    }
}
