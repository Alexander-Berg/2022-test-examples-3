package ru.yandex.market.logistics.mqm.tms.claim

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.AbstractMonitoringEventPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.service.monitoringevent.MonitoringEventService
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.math.BigDecimal
import java.time.Instant

@DisplayName("Тест джобы создания тикетов в очередь MQMCLAIM для Экспресс заказов")
class CreateExpressClaimExecutorTest : AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @SpyBean
    private lateinit var monitoringEventService: MonitoringEventService<AbstractMonitoringEventPayload>

    @Autowired
    private lateinit var executor: CreateExpressClaimExecutor

    private val payloadCaptor: KArgumentCaptor<CreateStartrekIssueForClaimPayload> = KArgumentCaptor(
        ArgumentCaptor.forClass(CreateStartrekIssueForClaimPayload::class.java),
        CreateStartrekIssueForClaimPayload::class
    )

    @Test
    @DisplayName("Успешный сценарий")
    @DatabaseSetup("/tms/claim/createExpressClaimExecutor/before/setup.xml")
    fun successTicketCreation() {
        clock.setFixed(Instant.parse("2021-12-20T20:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        executor.doJob(null)
        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )
        val check = "level=INFO\t" +
            "format=plain\t" +
            "code=CREATE_STARTREK_ISSUE_FOR_CLAIM\t" +
            "payload=Triggered ticket creation in MQMTESTCLAIM queue for partner for Express orders\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=MONITORING_EVENT\t" +
            "extra_keys=partnerId\t" +
            "extra_values=302\n"

        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBeEqualToComparingFields createPayload()
            log shouldContain check
        }
    }

    private fun createPayload(): CreateStartrekIssueForClaimPayload = CreateStartrekIssueForClaimPayload(
        "MQMTESTCLAIM",
        "Партнеру Экспресс СД будет выставлена претензия за просрочки по Экспресс-заказам",
        null,
        mapOf("deliveryName" to "ООО Экспресс СД", "amountClaimed" to BigDecimal.valueOf(1234), "components" to 101714),
        setOf(
            BaseCreateStartrekIssuePayload.CsvAttachment(
                "2021-12-20_302.csv",
                listOf(
                    mapOf(
                        "orderId" to "2011",
                        "track" to "ext102",
                        "shipmentDate" to "",
                        "cost" to BigDecimal(1234),
                        "previousStatus" to "заказ в претензионной работе",
                        "deliveryService" to "Экспресс СД"
                    )
                )
            )
        )
    )
}
