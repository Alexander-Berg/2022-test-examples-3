package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.IssueLinkService

@DisplayName("Тест удаление ненужных связок")
class DeleteUnnecessaryIssueLinksExecutorTest: AbstractContextualTest() {
    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var issueLinkService: IssueLinkService

    private lateinit var executor: DeleteUnnecessaryIssueLinksExecutor

    private val time = Instant.parse("2021-09-09T18:00:00Z")

    @BeforeEach
    fun setup() {
        clock.setFixed(time, DateTimeUtils.MOSCOW_ZONE)
        executor = DeleteUnnecessaryIssueLinksExecutor(
            issueLinkService,
            clock,
        )
    }

    @Test
    @DatabaseSetup("/tms/processDeleteUnnecessaryIssueLinksExecutor/before/success.xml")
    @ExpectedDatabase(
        value = "/tms/processDeleteUnnecessaryIssueLinksExecutor/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное удаление")
    fun successTest() {
        executor.doJob(jobContext)
    }
}
