package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import java.time.LocalDateTime

@DisplayName("Тест джобы простановки фактов у дедлайнов клиентских возвратов")
class UpdateClientReturnDeadlinesFactTimeExecutorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var executor: UpdateClientReturnDeadlinesFactTimeExecutor

    @Autowired
    private lateinit var ytService: YtService

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @BeforeEach
    fun setup() {
        doReturn(
            mapOf(
                10L to LocalDateTime.parse("2021-03-30T10:00:00"),
                20L to LocalDateTime.parse("2021-03-29T09:00:00"),
                30L to LocalDateTime.parse("2021-03-30T15:00:00")
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnDeadlinesFactTimeExecutor/before/setup_client_return_deadlines.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnDeadlinesFactTimeExecutor"
                + "/after/processed_client_return_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выставление факта")
    fun successProcessing() {
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnDeadlinesFactTimeExecutor/before/setup_missing_client_return_deadlines.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnDeadlinesFactTimeExecutor"
                + "/after/processed_client_return_deadlines_without_missing_ones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ничего не делать с фактами из YT, для которых нет дедлайнов в базе данных")
    fun skipDeadlinesNotPresentInDatabase() {
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnDeadlinesFactTimeExecutor/before/setup_client_return_with_facts.xml"
    )
    @DisplayName("Не подгружать планфакты с проставленным фактом")
    fun skipPlanfactsWithFact() {
        executor.doJob(jobContext)
        verify(ytService, never())
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }
}
