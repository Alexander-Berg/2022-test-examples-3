package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtReturnAndOrderIdDto
import ru.yandex.market.logistics.mqm.service.yt.dto.YtScReturnEventDto
import java.time.LocalDateTime

@DisplayName("Тест джобы создания план-фактов для возвратных заказов первичной приемки в ЦТЭ")
internal class CreateClientReturnFirstCteIntakeDeadlinesExecutorTest : AbstractContextualTest() {

    private val selectStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: CreateClientReturnFirstCteIntakeDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    @Test
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnFirstCteIntakeDeadlinesExecutor/after/return_client_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание план-фактов")
    fun successPlanFactCreation() {
        doReturn(
            listOf(
                createScReturnEvent(10L, "VOZVRAT_SF_PVZ_11", LocalDateTime.of(2021, 3, 1, 0, 30, 0), 13L, "СЦ13"),
                createScReturnEvent(20L, "VOZVRAT_SF_PVZ_21", LocalDateTime.of(2021, 3, 1, 7, 30, 0), 23L, "СЦ23"),
                createScReturnEvent(30L, "VOZVRAT_SF_PVZ_31", LocalDateTime.of(2021, 3, 1, 23, 30, 0), 13L, "СЦ13")
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
        doReturn(
            listOf(
                YtReturnAndOrderIdDto(11L, 12L),
                YtReturnAndOrderIdDto(21L, 22L),
                YtReturnAndOrderIdDto(31L, 32L)
            )
        )
            .whenever(ytService)
            .readTableExactIds<Any>(any(), any(), any(), any())
        doReturn(100L)
            .whenever(ytService)
            .getChunkRowCount(any())
        executor.doJob(jobContext)
        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        val selectString: String = selectStringCaptor.firstValue
        softly.assertThat(selectString).contains("RETURNED_ORDER_DELIVERED_TO_IM", "oh.id >= 0", "oh.id < 1000000")
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/createClientReturnFirstCteIntakeDeadlinesExecutor/before/do_not_save_copies.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnFirstCteIntakeDeadlinesExecutor/after/do_not_save_copies.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не сохранять копии событий п-ф которых уже есть в базе")
    fun doNotSaveCopies() {
        doReturn(
            listOf(
                createScReturnEvent(40L, "VOZVRAT_SF_PVZ_11", LocalDateTime.of(2021, 4, 1, 11, 0, 0), 13L, "СЦ13"),
                createScReturnEvent(20L, "VOZVRAT_SF_PVZ_21", LocalDateTime.of(2021, 3, 1, 11, 0, 0), 23L, "СЦ23")
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
        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        val selectString: String = selectStringCaptor.firstValue
        softly.assertThat(selectString).contains("RETURNED_ORDER_DELIVERED_TO_IM", "oh.id >= 10", "oh.id < 1000010")
    }

    private fun createScReturnEvent(
        recordId: Long,
        barcode: String,
        arrivedAt: LocalDateTime,
        sortingCenterId: Long,
        sortingCenterName: String
    ): YtScReturnEventDto {
        return YtScReturnEventDto(
            recordId,
            arrivedAt,
            barcode,
            null,
            null,
            sortingCenterId,
            sortingCenterName
        )
    }
}
