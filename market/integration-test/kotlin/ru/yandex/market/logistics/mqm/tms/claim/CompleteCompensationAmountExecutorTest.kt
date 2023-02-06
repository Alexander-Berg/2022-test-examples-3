package ru.yandex.market.logistics.mqm.tms.claim

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import org.joda.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.bolts.collection.Cf
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.CreateClaimExecutorProperties
import ru.yandex.market.logistics.mqm.service.OebsCustomerService
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.market.logistics.mqm.service.yt.YtOebsPaymentsService
import ru.yandex.market.logistics.mqm.service.yt.dto.YtOebsPayment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
class CompleteCompensationAmountExecutorTest : AbstractContextualTest() {
    @Autowired
    lateinit var prop: CreateClaimExecutorProperties

    @Mock
    lateinit var oebsCustomerService: OebsCustomerService

    @Mock
    lateinit var ytOebsPaymentsService: YtOebsPaymentsService

    @Mock
    lateinit var startrekService: StartrekService

    private lateinit var compensationAmountExecutor: CompleteCompensationAmountExecutor
    private val issueUpdateCaptor = argumentCaptor<IssueUpdate>()

    @BeforeEach
    fun init() {
        compensationAmountExecutor = CompleteCompensationAmountExecutor(
            oebsCustomerService,startrekService, ytOebsPaymentsService, prop)
    }

    @Test
    fun shouldUpdateSingleTicketWithSinglePayment() {
        val ticketId = 222L
        val partnerId = 321L
        val vendorId = 321321L

        whenever(startrekService.findIssues(any())).thenReturn(listOf(createIssue(ticketId, partnerId)))
        whenever(oebsCustomerService.getVendorIds(anySet(), anyOrNull())).thenReturn(mapOf(partnerId to vendorId))
        whenever(ytOebsPaymentsService.loadPayments(any(), any(), anyOrNull())).thenReturn(listOf(
            YtOebsPayment(vendorId, BigDecimal("11.11"), "СЧЕТ № Б-1231343-1 ОТ 2 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId В ТОМ ЧИСЛЕ НДС 20 % - 11.11 РУБЛЕЙ.")
        ))

        compensationAmountExecutor.run()

        verify(startrekService, times(1)).updateIssue(eq("MQMTESTCLAIM-$ticketId"), issueUpdateCaptor.capture())
        shouldHaveAgreedCompensationSumEqualsTo(issueUpdateCaptor.firstValue, "11.11")

    }

    @Test
    fun shouldSumPaymentsAndUpdateTicket() {
        val ticketId = 222L
        val partnerId = 321L
        val vendorId = 321321L
        whenever(startrekService.findIssues(any())).thenReturn(listOf(createIssue(ticketId, partnerId)))
        whenever(oebsCustomerService.getVendorIds(any(), anyOrNull())).thenReturn(mapOf(partnerId to vendorId))
        whenever(ytOebsPaymentsService.loadPayments(any(), any(), anyOrNull())).thenReturn(listOf(
            YtOebsPayment(vendorId, BigDecimal("11.11"), "СЧЕТ № Б-1231343-1 ОТ 2 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId В ТОМ ЧИСЛЕ НДС 20 % - 11.11 РУБЛЕЙ."),
            YtOebsPayment(vendorId, BigDecimal("22.22"), "СЧЕТ № Б-1231343-2 ОТ 3 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId В ТОМ ЧИСЛЕ НДС 20 % - 22.22 РУБЛЕЙ."),
        ))

        compensationAmountExecutor.run()

        verify(startrekService, times(1)).updateIssue(any(), issueUpdateCaptor.capture())
        shouldHaveAgreedCompensationSumEqualsTo(issueUpdateCaptor.firstValue, "33.33")
    }

    @Test
    fun shouldUpdateOnlyTicketsInIndemnificationWaitStatusAndIgnoreOtherPayments() {
        val ticketId1 = 111L
        val ticketId2 = 222L
        val partnerId1 = 321L
        val partnerId2 = 322L
        val vendorId1 = 321321L
        val vendorId2 = 321322L
        whenever(startrekService.findIssues(any())).thenReturn(listOf(
            createIssue(ticketId1, partnerId1),
            createIssue(ticketId2, partnerId2),
        ))
        whenever(oebsCustomerService.getVendorIds(any(), anyOrNull())).thenReturn(mapOf(partnerId1 to vendorId1, partnerId2 to vendorId2))
        whenever(ytOebsPaymentsService.loadPayments(any(), any(), anyOrNull())).thenReturn(listOf(
            YtOebsPayment(vendorId1, BigDecimal("11.11"),
                "СЧЕТ № Б-123-1 ОТ 2 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId1 В ТОМ ЧИСЛЕ НДС 20 % - 11.11 РУБЛЕЙ."),
            YtOebsPayment(vendorId1, BigDecimal("22.22"),
                "СЧЕТ № Б-123-2 ОТ 3 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId1 В ТОМ ЧИСЛЕ НДС 20 % - 22.22 РУБЛЕЙ."),
            YtOebsPayment(vendorId2, BigDecimal("444.44"),
                "СЧЕТ № Б-123 ОТ 3 ФЕВРАЛЯ 2112 Г. За претензию MC-$ticketId2 В ТОМ ЧИСЛЕ НДС 20 % - 22.22 РУБЛЕЙ."),
            YtOebsPayment(vendorId1, BigDecimal("66.6"),
                "СЧЕТ № Б-1 ОТ 3 ФЕВРАЛЯ 2112 Г. За претензию MC-666 которой не было в исходном запросе: игнорировать"),
            YtOebsPayment(vendorId1, BigDecimal("66.6"), "Здесь вообще нет номера тикета - игнорировать"),
        ))

        compensationAmountExecutor.run()

        verify(startrekService, times(2)).updateIssue(any(), issueUpdateCaptor.capture())
        assertSoftly {
            shouldHaveAgreedCompensationSumEqualsTo(issueUpdateCaptor.firstValue, "33.33")
            shouldHaveAgreedCompensationSumEqualsTo(issueUpdateCaptor.secondValue, "444.44")
            issueUpdateCaptor.allValues.size shouldBeExactly 2
        }
    }

    private fun createIssue(ticketId: Long, partnerId: Long) = Issue(
        null, null, "MQMTESTCLAIM-$ticketId",
        "Партнеру $partnerId будет выставлена претензия за просрочки", 1L, Cf.map("createdAt", Instant.now()), null
    )

    private fun shouldHaveAgreedCompensationSumEqualsTo(actualIssueUpdate: IssueUpdate, sum: String) {
        actualIssueUpdate.values.keys()[0] shouldBe "agreedCompensationSum"
        (actualIssueUpdate.values.getO("agreedCompensationSum").get() as ScalarUpdate).set shouldBe listOf(sum)
    }
}
