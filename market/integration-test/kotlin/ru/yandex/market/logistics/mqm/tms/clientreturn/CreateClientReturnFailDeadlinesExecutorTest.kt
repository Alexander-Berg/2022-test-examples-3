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
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.additionaldata.ReturnClientPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.yt.YtService
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("Тест джобы создания план-фактов для неудачных вовзратных заказов из локеров")
class CreateClientReturnFailDeadlinesExecutorTest : AbstractContextualTest() {

    @Autowired
    private lateinit var executor: CreateClientReturnFailDeadlinesExecutor

    @Autowired
    private lateinit var ytService: YtService

    @Mock
    private lateinit var jobContext: JobExecutionContext

    @BeforeEach
    fun setup() {
        clock.setFixed(Instant.parse("2021-04-15T13:19:00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnFailDeadlinesExecutor/after/return_client_deadlines.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное создание план-фактов")
    fun successPlanFactCreation() {
        doReturn(
            listOf(
                buildPlanFact(1L, 10L, 10203L),
                buildPlanFact(2L, 20L, 10204L),
                buildPlanFact(3L, 30L, 10203L)
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<PlanFact>(
                eq(String.format(FAIL_CLIENT_RETURN_QUERY_TEMPLATE, 0L)),
                anyOrNull(),
                any()
            )
        executor.doJob(jobContext)
    }

    @Test
    @DatabaseSetup("/tms/clientreturn/createClientReturnFailDeadlinesExecutor/before/return_client_deadlines.xml")
    @ExpectedDatabase(
        value = "/tms/clientreturn/createClientReturnFailDeadlinesExecutor/after/old_deadlines_with_new_ones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Не вычитывать события, для которых уже есть план-факты")
    fun doNotCreateExistingPlanFacts() {
        doReturn(
            listOf(
                buildPlanFact(11L, 40L, 10205L)
            )
        )
            .whenever(ytService)
            .selectRowsFromTable<PlanFact>(
                eq(String.format(FAIL_CLIENT_RETURN_QUERY_TEMPLATE, 10L)),
                anyOrNull(),
                any()
            )
        executor.doJob(jobContext)
    }

    private fun buildPlanFact(failId: Long, returnId: Long, sortingCenterId: Long) =
        PlanFact(
            entityId = returnId,
            entityType = EntityType.CLIENT_RETURN,
            expectedStatus = SegmentStatus.UNKNOWN.name,
            waybillSegmentType = SegmentType.PICKUP,
            expectedStatusDatetime = clock.instant().plusSeconds(returnId),
            producerName = ClientReturnFailFactProcessor::class.simpleName,
        )
            .setData(
                ReturnClientPlanFactAdditionalData(
                    failId = failId,
                    barcode = "VOZVRAT_0$returnId",
                    sortingCenterId = sortingCenterId,
                    returnId = returnId,
                    failReasonId = 4L,
                    failComment = "PACKAGE IS DAMAGED",
                    lockerWithdrawDatetime = LocalDateTime.of(2021, 4, 14, 16, 19, 0),
                    partnerName = "Some SC",
                    courierName = "The best courier name",
                    pickupPointType = "LOCKER"
                )
            )
            .markCreated(clock.instant())

    companion object {
        private const val FAIL_CLIENT_RETURN_QUERY_TEMPLATE = "crf.id AS fail_id, " +
            "crf.return_id AS return_id, " +
            "crf.reason_id AS reason_id, " +
            "crf.reason AS reason, " +
            "crf.created_at AS detected_at, " +
            "crf.barcode AS barcode, " +
            "s.sorting_center_id AS partner_id, " +
            "p.readable_name AS partner_name, " +
            "u.name AS courier_name " +
            "FROM [/] crf " +
            "JOIN [/] t on crf.task_id = t.id " +
            "JOIN [/] rp on t.route_point_id = rp.id " +
            "JOIN [/] us on rp.user_shift_id = us.id " +
            "JOIN [/] s on us.shift_id = s.id " +
            "JOIN [/] u on us.user_id = u.id " +
            "JOIN [/] p on s.sorting_center_id = p.id " +
            "WHERE crf.id > %d"
    }
}
