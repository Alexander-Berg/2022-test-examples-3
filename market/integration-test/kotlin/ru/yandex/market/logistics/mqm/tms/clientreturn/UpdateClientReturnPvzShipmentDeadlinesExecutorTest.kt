package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService

@DisplayName("Тест джобы простановки фактов у дедлайнов клиентских возвратов для сегмента забора из ПВЗ")
internal class UpdateClientReturnPvzShipmentDeadlinesExecutorTest : AbstractContextualTest() {
    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: UpdateClientReturnPvzShipmentDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    private val selectStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @BeforeEach
    fun setup() {
        doReturn(
            mapOf(
                10L to "2021-03-30T10:00:00".toInstant(),
                20L to "2021-03-29T09:00:00".toInstant(),
                30L to "2021-03-30T15:00:00".toInstant()
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnPvzShipmentDeadlinesExecutor/before/setup_client_return_deadlines.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnPvzShipmentDeadlinesExecutor"
                + "/after/processed_client_return_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выставление факта")
    fun successProcessing() {
        executor.doJob(jobContext)
        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        softly.assertThat(selectStringCaptor.firstValue).contains("'10'", "'20'", "'30'")
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/updateClientReturnPvzShipmentDeadlinesExecutor/before/skip_plan_facts.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnPvzShipmentDeadlinesExecutor/after/skip_plan_facts.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Ничего не делать с планфактами, для которых нет факта из yt или уже проставлен факт")
    fun skipPlanFacts() {
        executor.doJob(jobContext)
    }
}
