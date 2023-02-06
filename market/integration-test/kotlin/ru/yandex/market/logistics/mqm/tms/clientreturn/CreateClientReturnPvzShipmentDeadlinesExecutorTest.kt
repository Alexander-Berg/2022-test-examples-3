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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.PickupPointType
import ru.yandex.market.logistics.mqm.service.yt.dto.YtPvzReturnEventDto
import java.time.LocalDateTime

@DisplayName("Тест джобы создания план-фактов для возвратных заказов лежащих в ПВЗ")
internal class CreateClientReturnPvzShipmentDeadlinesExecutorTest : AbstractContextualTest() {

    private val selectStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: CreateClientReturnPvzShipmentDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    @Test
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnPvzShipmentDeadlinesExecutor/after/return_client_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание план-фактов")
    fun successPlanFactCreation() {
        doReturn(
            listOf(
                generatePvzReturnEvent(1, 11, true),
                generatePvzReturnEvent(2, 12, false),
                generatePvzReturnEvent(3, 12, false)
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<YtPvzReturnEventDto>(any(), anyOrNull(), any())
        executor.doJob(jobContext)
        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        val selectString: String = selectStringCaptor.firstValue
        softly.assertThat(selectString.endsWith("'1970-01-01T00:00:00+03:00'")).isTrue
    }

    @Test
    @DatabaseSetup(
        "/tms/clientreturn/createClientReturnPvzShipmentDeadlinesExecutor/before/setup_plan_facts.xml"
    )
    @DisplayName("Запрашивать только новые события из ПВЗ")
    fun loadOnlyNewEvents() {
        doReturn(listOf<Any>())
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
        executor.doJob(jobContext)
        verify(ytService).selectRowsFromTable<Any>(selectStringCaptor.capture(), anyOrNull(), any())
        val selectString: String = selectStringCaptor.firstValue
        softly.assertThat(selectString.endsWith("2021-03-01T12:34:56.123123+03:00'")).isTrue
    }

    private fun generatePvzReturnEvent(key: Int, hour: Int, generatePickupPointInfo: Boolean): YtPvzReturnEventDto {
        val dto = YtPvzReturnEventDto(
            recordId = key * 10L,
            returnId = key * 10L + 1,
            arrivedAt = LocalDateTime.of(2021, 3, 1, hour, 0, 0),
            pickupPointType = PickupPointType.PVZ,
            orderId = key * 10L + 2,
            barcode = "BARCODE" + key * 10
        )
        if (generatePickupPointInfo) {
            dto.pickupPointId = 1000L + key
            dto.pickupPointName = "ПВЗ$key"
            dto.pickupPointAddress = "Адрес$key"
            dto.pickupPointRegion = "Регион$key"
        }
        return dto
    }
}
