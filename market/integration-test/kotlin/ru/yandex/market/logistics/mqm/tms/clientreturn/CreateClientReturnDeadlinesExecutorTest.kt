package ru.yandex.market.logistics.mqm.tms.clientreturn

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.yt.YtService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtClientReturnOrderDto
import ru.yandex.market.logistics.mqm.service.yt.dto.YtCourierWithdrawEventDto
import java.time.LocalDateTime

@DisplayName("Тест джобы создания план-фактов для возвратных заказов на сегменте от ПВЗ до СЦ")
class CreateClientReturnDeadlinesExecutorTest : AbstractContextualTest() {

    private val selectStringCaptor: KArgumentCaptor<String> =
        KArgumentCaptor(ArgumentCaptor.forClass(String::class.java), String::class)

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @Autowired
    private lateinit var executor: CreateClientReturnDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    @Test
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnDeadlinesExecutor/after/return_client_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание план-фактов")
    fun successPlanFactCreation() {
       doReturn(
            listOf(
                getYtLockerEvent(
                    10L,
                    LocalDateTime.of(2021, 3, 29, 13, 0, 0),
                    "VOZVRAT_1",
                    "LOCKER"
                ),
                getYtLockerEvent(
                    20L,
                    LocalDateTime.of(2021, 3, 29, 12, 0, 0),
                    "VOZVRAT_2",
                    "PVZ"
                )
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<YtCourierWithdrawEventDto>(startsWith(LOCKER_DELIVERY_INFO_QUERY_PREFIX), any(), any())

        doReturn(
            mapOf(
                Pair("VOZVRAT_1", getYtClientReturnOrder(10203L)),
                Pair("VOZVRAT_2", getYtClientReturnOrder(10204L))
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<Map<String, YtClientReturnOrderDto>>(
                startsWith(CLIENT_RETURN_ORDERS_INFO_QUERY_PREFIX),
                any(),
                any()
            )

        executor.doJob(jobContext)

        verify(ytService, times(2))
            .selectRowsFromTable<Any>(selectStringCaptor.capture(), any(), any())
        softly.assertThat(selectStringCaptor.allValues[0]).contains("1970-01-01T00:00:00+03:00")
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/createClientReturnDeadlinesExecutor/before/setup_plan_facts.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnDeadlinesExecutor/after/old_client_deadlines_with_new_one.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не запрашивать из YT и не пересоздавать уже существующие план-факты")
    fun doNotRecreateExistingPlanFacts() {
        doReturn(
            listOf(
                getYtLockerEvent(
                    30L,
                    LocalDateTime.of(2021, 3, 29, 11, 0, 0),
                    "VOZVRAT_3",
                    "LOCKER"
                )
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<YtClientReturnOrderDto>(startsWith(LOCKER_DELIVERY_INFO_QUERY_PREFIX), any(), any())
        doReturn(mapOf(Pair("VOZVRAT_3", getYtClientReturnOrder(10205L))))
            .whenever(ytService)
            .selectRowsFromTable<YtClientReturnOrderDto>(
                startsWith(CLIENT_RETURN_ORDERS_INFO_QUERY_PREFIX),
                any(),
                any()
            )

        executor.doJob(jobContext)

        verify(ytService, times(2))
            .selectRowsFromTable<Any>(selectStringCaptor.capture(), any(), any())
        softly.assertThat(selectStringCaptor.allValues[0]).contains(
            "AND sld.finished_at > '2021-03-29T13:00:00.123456+03:00'"
        )
    }

    private fun getYtLockerEvent(
        externalReturnId: Long,
        eventTime: LocalDateTime,
        barcode: String,
        pickupPointType: String
    ) = YtCourierWithdrawEventDto(
        externalReturnId,
        eventTime,
        barcode,
        "incorrect choice",
        "Wrong size",
        pickupPointType
    )

    private fun getYtClientReturnOrder(sortingCenterId: Long) =
        YtClientReturnOrderDto(sortingCenterId, "Some courier", "Some sorting center")

    companion object {
        private const val LOCKER_DELIVERY_INFO_QUERY_PREFIX = "cr.external_return_id"

        private const val CLIENT_RETURN_ORDERS_INFO_QUERY_PREFIX = "o.external_id"
    }
}
