package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.PickupPointType
import ru.yandex.market.logistics.mqm.service.yt.dto.YtFactReturnItemDto
import ru.yandex.market.logistics.mqm.service.yt.dto.YtPvzReturnEventDto
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("Тест джобы создания план-фактов для возвратных заказов на сегменте от клиента до ЦТЭ")
class CreateClientToCteReturnDeadlinesExecutorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var executor: CreateClientToCteReturnDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    private val queryStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Test
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientToCteReturnDeadlinesExecutor/after/return_client_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание план-фактов")
    fun successPlanFactCreation() {
        doReturn(
            listOf(
                generateReturnEvent(1, PickupPointType.PVZ, 1, 11, true, 12L),
                generateReturnEvent(2, PickupPointType.PVZ, 1, 12, false, 22L),
                generateReturnEvent(3, PickupPointType.PVZ, 1, 13, false, 22L)
            ),
            listOf(
                generateReturnEvent(4, PickupPointType.LOCKER, 1, 14, true),
                generateReturnEvent(5, PickupPointType.LOCKER, 1, 15, false)
            ),
            listOf(
                generateReturnEvent(6, PickupPointType.LOCKER_FAIL, 1, 16, false),
                generateReturnEvent(
                    7,
                    PickupPointType.LOCKER_FAIL,
                    1,
                    16,
                    false,
                    locality = "Вологда"
                )
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<YtPvzReturnEventDto>(any(), anyOrNull(), any())

        doReturn(
            listOf(
                YtFactReturnItemDto(11L, 12L, 1000L, 1L),
                YtFactReturnItemDto(21L, 22L, 1000L, 1L),
                YtFactReturnItemDto(21L, 22L, 1000L, 2L),
                YtFactReturnItemDto(21L, 22L, 1001L, 2L),
                YtFactReturnItemDto(31L, 22L, 1000L, 3L),
                YtFactReturnItemDto(41L, 42L, 1000L, 2L),
                YtFactReturnItemDto(41L, 42L, 1001L, 3L),
                YtFactReturnItemDto(61L, 62L, 1001L, 1L),
                YtFactReturnItemDto(61L, 62L, 1001L, 2L),
                YtFactReturnItemDto(71L, 72L, 1001L, 2L)
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow<YtFactReturnItemDto>(any(), any(), any(), isNull(), any())

        doReturn(100L).whenever(ytService).getRowCount(any())

        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/createClientToCteReturnDeadlinesExecutor/before/setup_plan_facts.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientToCteReturnDeadlinesExecutor/after/old_client_deadlines_with_new_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не запрашивать из YT и не пересоздавать уже существующие план-факты")
    fun doNotRecreateExistingPlanFacts() {
        doReturn(
            listOf(
                generateReturnEvent(3, PickupPointType.PVZ, 1, 13, false, 32L)
            ),
            listOf(
                generateReturnEvent(4, PickupPointType.LOCKER, 1, 14, true)
            ),
            listOf(
                generateReturnEvent(6, PickupPointType.LOCKER_FAIL, 1, 16, false)
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<YtPvzReturnEventDto>(any(), anyOrNull(), any())

        doReturn(listOf(YtFactReturnItemDto(31L, 32L, 1000L, 2L)))
            .whenever(ytService)
            .readTableFromRowToRow<YtPvzReturnEventDto>(any(), any(), any(), isNull(), any())
        doReturn(100L).whenever(ytService).getRowCount(any())
        executor.doJob(jobContext)

        verify(ytService, times(3)).selectRowsFromTable<Any>(queryStringCaptor.capture(), anyOrNull(), any())
        softly.assertThat(queryStringCaptor.allValues[0])
            .contains("rr.arrived_at > '2021-03-01T11:00:00+03:00'")
        softly.assertThat(queryStringCaptor.allValues[1]).contains(
            "AND sld.finished_at > '2021-03-01T12:12:12.121212+03:00'"
        )
        softly.assertThat(queryStringCaptor.allValues[2]).contains("WHERE crf.id > 50")
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/createClientToCteReturnDeadlinesExecutor/before/setup_plan_facts.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientToCteReturnDeadlinesExecutor/after/do_not_create_today_events.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не создавать план-факты за сегодняшний день")
    fun doNotCreateTodayPlanFacts() {
        clock.setFixed(Instant.parse("2021-03-03T13:19:00Z"), DateTimeUtils.MOSCOW_ZONE)
        doReturn(
            listOf(
                generateReturnEvent(7, PickupPointType.PVZ, 1, 17, false, 72L),
                generateReturnEvent(3, PickupPointType.PVZ, 3, 13, false, 32L)
            ),
            listOf(
                generateReturnEvent(4, PickupPointType.LOCKER, 3, 14, true)
            ),
            listOf(
                generateReturnEvent(6, PickupPointType.LOCKER_FAIL, 3, 16, false)
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Any>(any(), anyOrNull(), any())
        doReturn(
            listOf(
                YtFactReturnItemDto(71L, 72L, 1000L, 2L)
            )
        )
            .whenever(ytService)
            .readTableFromRowToRow<Any>(any(), any(), any(), isNull(), any())
        doReturn(100L).whenever(ytService).getRowCount(any())
        executor.doJob(jobContext)
        verify(ytService, times(3)).selectRowsFromTable<Any>(any(), anyOrNull(), any())
    }

    private fun generateReturnEvent(
        key: Int,
        pickupPointType: PickupPointType,
        day: Int,
        hour: Int,
        generatePickupPointInfo: Boolean,
        explicitOrderId: Long? = null,
        locality: String? = null
    ): YtPvzReturnEventDto {
        val dto = YtPvzReturnEventDto(
            key * 10L,
            key * 10L + 1,
            LocalDateTime.of(2021, 3, day, hour, 0, 0),
            pickupPointType,
            explicitOrderId,
            "BARCODE" + key * 10,
            locality = locality
        )
        if (generatePickupPointInfo) {
            dto.pickupPointId = 1000L + key
            dto.pickupPointName = "ПВЗ$key"
            dto.pickupPointAddress = ("Адрес$key")
        }
        return dto
    }
}
