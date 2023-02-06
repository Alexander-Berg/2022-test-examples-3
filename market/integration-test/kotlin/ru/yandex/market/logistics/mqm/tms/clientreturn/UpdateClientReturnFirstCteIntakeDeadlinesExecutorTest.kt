package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
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
import ru.yandex.market.logistics.mqm.service.yt.dto.YtFirstCteIntakeRegistryDto
import ru.yandex.market.logistics.mqm.service.yt.dto.YtFirstCteIntakeRegistryItemDto
import java.time.Instant

@DisplayName("Тест джобы простановки фактов у дедлайнов клиентских возвратов для первичной приемки в ЦТЭ")
internal class UpdateClientReturnFirstCteIntakeDeadlinesExecutorTest : AbstractContextualTest() {

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: UpdateClientReturnFirstCteIntakeDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    @BeforeEach
    fun setup() {
        doReturn(100L)
            .whenever(ytService)
            .getRowCount(any())
        doReturn(
            listOf(
                YtFirstCteIntakeRegistryItemDto(true, "BARCODE10", 1001L),
                YtFirstCteIntakeRegistryItemDto(false, "BARCODE20", 1002L),
                YtFirstCteIntakeRegistryItemDto(true, "BARCODE30", 1003L),
                YtFirstCteIntakeRegistryItemDto(true, "BARCODE40", 1003L),
                YtFirstCteIntakeRegistryItemDto(true, "BARCODE50", 1003L),
                YtFirstCteIntakeRegistryItemDto(true, "BARCODE60", 1004L)
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow(any(), eq(YtFirstCteIntakeRegistryItemDto::class.java), any(), any(), any())
        doReturn(
            listOf(
                YtFirstCteIntakeRegistryDto(1001L, "2021-04-23 22:25:11.877513"),
                YtFirstCteIntakeRegistryDto(1002L, "2021-04-23 22:25:12.877513"),
                YtFirstCteIntakeRegistryDto(1003L, "2021-04-23 22:25:13.877513"),
                YtFirstCteIntakeRegistryDto(1099L, "2021-04-23 22:25:16.877513")
            )
        )
            .whenever(ytService)
            .readTable(any(), eq(YtFirstCteIntakeRegistryDto::class.java), any())
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnFirstCteIntakeDeadlinesExecutor/before/setup_client_return_deadlines.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnFirstCteIntakeDeadlinesExecutor"
                + "/after/processed_client_return_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выставление факта")
    fun successProcessing() {
        clock.setFixed(Instant.parse("2021-08-01T20:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnFirstCteIntakeDeadlinesExecutor/before/skip_plan_facts.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnFirstCteIntakeDeadlinesExecutor/after/skip_plan_facts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ничего не делать с планфактами, для которых нет факта из yt или уже проставлен факт")
    fun skipPlanFacts() {
        executor.doJob(jobContext)
    }
}
