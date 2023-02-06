package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtCteReturnFactDto
import java.time.Instant

@DisplayName("Тест джобы простановки фактов у дедлайнов клиентских возвратов на сегменте от клиента до ЦТЭ")
class UpdateReturnFromClientToCteDeadlinesFactTimeExecutorTest : AbstractContextualTest() {

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: UpdateReturnFromClientToCteDeadlinesFactTimeExecutor

    @Autowired
    private lateinit var ytService: YtService

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-01-01T10:00:00Z"), DateTimeUtils.MOSCOW_ZONE)
        doReturn(100L).whenever(ytService).getRowCount(any())
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/before/success_one_item.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/after/success_one_item.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выставление факта для возврата с одним предметом")
    fun oneItem() {
        doReturn(
            listOf(
                YtCteReturnFactDto(11L, 1000L, 1L, "2021-03-11 11:00:00")
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow(any(), eq(YtCteReturnFactDto::class.java), any(), anyOrNull(), any())
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/before/right_number_of_items.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/after/right_number_of_items.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Правильное проставление количества предметов")
    fun rightNumberOfItems() {
        doReturn(
            listOf(
                YtCteReturnFactDto(11L, 1000L, 1L, "2021-03-11 11:00:00"),
                YtCteReturnFactDto(11L, 1000L, 2L, "2021-03-11 11:00:00"),
                YtCteReturnFactDto(11L, 1001L, 2L, "2021-03-11 11:00:00"),
                YtCteReturnFactDto(11L, 1001L, 2L, "2021-03-11 11:00:00"),
                YtCteReturnFactDto(11L, 1002L, 3L, "2021-03-11 11:00:00")
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow(any(), eq(YtCteReturnFactDto::class.java), any(), anyOrNull(), any())
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/before/right_fact_time.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateReturnFromClientToCteDeadlinesFactTimeExecutor/after/right_fact_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Правильное проставление фактического времени")
    fun rightFactTime() {
        doReturn(
            listOf(
                YtCteReturnFactDto(11L, 1000L, 1L, "2021-03-01 11:00:00"),
                YtCteReturnFactDto(11L, 1000L, 1L, "2021-03-02 11:00:00"),
                YtCteReturnFactDto(11L, 1000L, 1L, "2021-03-03 11:00:00"),
                YtCteReturnFactDto(11L, 1001L, 1L, "2021-03-04 11:00:00"),
                YtCteReturnFactDto(11L, 1001L, 1L, "2021-03-05 11:00:00"),
                YtCteReturnFactDto(11L, 1001L, 1L, "2021-03-06 11:00:00"),
                YtCteReturnFactDto(11L, 1002L, 3L, "2021-03-01 11:00:00")
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow(any(), eq(YtCteReturnFactDto::class.java), any(), anyOrNull(), any())
        executor.doJob(jobContext)
    }
}
