package ru.yandex.market.wms.constraints.scheduler.job

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.constraints.config.ConstraintsIntegrationTest

class FindExpiredConstraintsIssuesJobTest : ConstraintsIntegrationTest() {

    @Autowired
    private lateinit var findExpiredConstraintsIssuesJob: FindExpiredConstraintsIssuesJob

    @DatabaseSetup("/scheduler/job/find-expired-constraints-issues-job/before.xml")
    @ExpectedDatabase(
        value = "/scheduler/job/find-expired-constraints-issues-job/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    fun `run updates status when issue is expired by SLA timeout and has status PROCESSED`() {
        val message = findExpiredConstraintsIssuesJob.run()
        assertEquals("UpdatedIssuesCount=1", message)
    }
}
