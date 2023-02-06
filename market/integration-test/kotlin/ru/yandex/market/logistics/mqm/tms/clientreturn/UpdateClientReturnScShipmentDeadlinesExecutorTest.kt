package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtReturnAndOrderIdDto
import java.time.Instant
import kotlin.reflect.KClass

@DisplayName("Тест джобы простановки фактов у дедлайнов клиентских возвратов для сегмента забора из СЦ")
internal class UpdateClientReturnScShipmentDeadlinesExecutorTest : AbstractContextualTest() {

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: UpdateClientReturnScShipmentDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    private val selectStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/updateClientReturnScShipmentDeadlinesExecutor/before/setup_client_return_deadlines.xml"
    )
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnScShipmentDeadlinesExecutor"
                + "/after/processed_client_return_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выставление факта")
    fun successProcessing() {
        clock.setFixed(Instant.parse("2021-07-07T11:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        doReturn(
            mapOf(
                "VOZVRAT_SF_PVZ_11" to "2021-03-30T10:00:00".toInstant(),
                "VOZVRAT_SF_PVZ_21" to "2021-03-29T09:00:00".toInstant(),
                "VOZVRAT_SF_PVZ_31" to "2021-03-04T11:00:00".toInstant()
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
        doReturn(listOf(YtReturnAndOrderIdDto(11L, 12L)))
            .whenever(ytService)
            .readTableExactIds<Any>(any(), any(), any(), any())
        doReturn(100L).whenever(ytService).getChunkRowCount(any())

        executor.doJob(jobContext)

        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        softly.assertThat(selectStringCaptor.firstValue)
            .contains("'VOZVRAT_SF_PVZ_11'", "'VOZVRAT_SF_PVZ_21'", "'VOZVRAT_SF_PVZ_31'")
        softly.assertThat(selectStringCaptor.firstValue).contains("oh.id >= 10", "oh.id < 1000010")
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/updateClientReturnScShipmentDeadlinesExecutor/before/set_order_id.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/updateClientReturnScShipmentDeadlinesExecutor/after/set_order_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Проставление orderId даже если нет факта")
    fun setOrderId() {
        clock.setFixed(Instant.parse("2021-07-07T11:00:01.00Z"), DateTimeUtils.MOSCOW_ZONE)
        doReturn(
            mapOf(
                "VOZVRAT_SF_PVZ_11" to "2021-03-30T10:00:00".toInstant()
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
        doReturn(
            listOf(
                YtReturnAndOrderIdDto(11L, 12L),
                YtReturnAndOrderIdDto(21L, 22L)
            )
        )
            .whenever(ytService)
            .readTableExactIds<Any>(any(), any(), any(), any())
        doReturn(100L)
            .whenever(ytService)
            .getChunkRowCount(any())

        executor.doJob(jobContext)

        val listCaptor = ArgumentCaptor.forClass((List::class as KClass<List<Long>>).java)
        verify(ytService).readTableExactIds<Any>(any(), any(), listCaptor.capture() ?: emptyList(), any())
        softly.assertThat(listCaptor.firstValue).containsExactly(11L, 21L)
    }

}
