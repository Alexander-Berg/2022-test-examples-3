package ru.yandex.travel.orders.services.finances.providers

import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit
import ru.yandex.travel.hotels.common.orders.BaseRate
import ru.yandex.travel.hotels.common.orders.BronevikHotelItinerary
import ru.yandex.travel.hotels.common.orders.OrderDetails
import ru.yandex.travel.hotels.common.orders.RefundInfo
import ru.yandex.travel.orders.entities.BronevikOrderItem
import ru.yandex.travel.orders.entities.FiscalItem
import ru.yandex.travel.orders.entities.HotelOrder
import ru.yandex.travel.orders.entities.PaymentSchedule
import ru.yandex.travel.orders.entities.PaymentScheduleItem
import ru.yandex.travel.orders.entities.PendingInvoice
import ru.yandex.travel.orders.entities.PendingInvoiceItem
import ru.yandex.travel.orders.entities.finances.FinancialEvent
import ru.yandex.travel.orders.entities.finances.FinancialEventType.PAYMENT
import ru.yandex.travel.orders.entities.finances.FinancialEventType.REFUND
import ru.yandex.travel.orders.entities.partners.BronevikBillingPartnerAgreement
import ru.yandex.travel.orders.repository.FinancialEventRepository
import ru.yandex.travel.orders.services.finances.FinancialEventService
import ru.yandex.travel.orders.workflow.payments.proto.EPaymentState.PS_FULLY_PAID
import ru.yandex.travel.orders.workflow.payments.proto.EPaymentState.PS_PARTIALLY_PAID
import ru.yandex.travel.orders.workflows.orderitem.bronevik.BronevikProperties
import ru.yandex.travel.testing.misc.TestBaseObjects.rub
import ru.yandex.travel.utils.ClockService
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit.DAYS
import javax.money.Monetary
import javax.money.NumberValue

@RunWith(SpringRunner::class)
class BronevikFinancialDataProviderTest {
    @Mock
    private lateinit var financialEventRepository: FinancialEventRepository

    private fun mockNoEvents() {
        `when`(financialEventRepository.findAllByOrderItem(ArgumentMatchers.any())).thenReturn(emptyList())
    }

    private fun mockWithList(list: List<FinancialEvent>) {
        `when`(financialEventRepository.findAllByOrderItem(ArgumentMatchers.any())).thenReturn(list)
    }

    private fun mockPaid(amountPaid: Money, enablePromoFee: Boolean, promoMoney: Money = rub(0)) {
        val calculator = FullMoneySplitCalculator()

        `when`(financialEventRepository.findAllByOrderItem(ArgumentMatchers.any()))
            .thenAnswer {
                val split = ProviderHelper.splitMoney(amountPaid, BigDecimal.valueOf(0.13))
                val fullSplit = calculator.calculatePaymentWithPromoMoney(split, promoMoney)
                val event = FinancialEvent.builder()
                    .type(PAYMENT)
                    .build()
                ProviderHelper.setPaymentMoney(event, fullSplit, enablePromoFee)
                println(event)
                listOf(event)
            }
    }

    private fun provider(currentTimeUtc: String): BronevikFinancialDataProvider {
        val clock = Clock.fixed(Instant.parse(currentTimeUtc), ZoneId.of("UTC"))

        return BronevikFinancialDataProvider(financialEventRepository, FullMoneySplitCalculator(),
            ClockService.create(clock), BronevikProperties())
    }

    private fun getFullyPaidSchedule(paymentMoment: Instant): PaymentSchedule {
        return PaymentSchedule.builder()
            .state(PS_FULLY_PAID)
            .items(listOf(
                PaymentScheduleItem.builder()
                    .pendingInvoice(
                        PendingInvoice.builder()
                            .state(PS_FULLY_PAID)
                            .closedAt(paymentMoment)
                            .build()
                    )
                    .build()
            ))
            .build()
    }

    @Test
    fun onConfirmationEventStructure() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(price = 10000.0, checkOut = "2022-04-07").get()

        val events: List<FinancialEvent> = provider("2022-04-01T13:15:30.00Z")
            .onConfirmation(orderItem, enablePromoFee)

        assertThat(events.size).isEqualTo(1)
        val event = events[0]

        assertThat(event.orderItem).isNotNull()
        assertThat(event.order).isNotNull()
        assertThat(event.orderPrettyId).isEqualTo("YA-PRETTY-ID")
        assertThat(event.type).isEqualTo(PAYMENT)
        assertThat(event.billingClientId).isEqualTo(110146753)
        assertThat(event.billingContractId).isEqualTo(4358521L)
        assertThat(event.accrualAt).isEqualTo(Instant.parse("2022-04-01T13:15:30.00Z"))
        assertThat(event.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.partnerAmount).isEqualTo(rub(8700))
        assertThat(event.feeAmount).isEqualTo(rub(1300))
        assertThat(event.promoCodePartnerAmount).isNull()
        assertThat(event.promoCodeFeeAmount).isNull()
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
        assertThat(event.promoCodePartnerRefundAmount).isNull()
        assertThat(event.promoCodeFeeRefundAmount).isNull()
    }

    @Test
    fun onConfirmation_rounding() {
        mockNoEvents()
        val enablePromoFee = false
        val provider = provider("2019-11-12T13:15:30.00Z")
        val e1 = provider.onConfirmation(OrderItemFactory(price = 10000.005).get(), enablePromoFee)[0]
        assertThat(e1.partnerAmount).isEqualTo(rub(8700.01))
        assertThat(e1.feeAmount).isEqualTo(rub(1300.00))
        val e2 = provider.onConfirmation(OrderItemFactory(price = 10000.004).get(), enablePromoFee)[0]
        assertThat(e2.partnerAmount).isEqualTo(rub(8700.00))
        assertThat(e2.feeAmount).isEqualTo(rub(1300.00))
        val e3 = provider.onConfirmation(OrderItemFactory(price = 10000.03).get(), enablePromoFee)[0]
        assertThat(e3.partnerAmount).isEqualTo(rub(8700.03))
        assertThat(e3.feeAmount).isEqualTo(rub(1300.00))
        val e4 = provider.onConfirmation(OrderItemFactory(price = 10000.04).get(), enablePromoFee)[0]
        assertThat(e4.partnerAmount).isEqualTo(rub(8700.03))
        assertThat(e4.feeAmount).isEqualTo(rub(1300.01))
    }

    @Test
    fun onConfirmation_payoutAfterCheckout() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04").get()
        val events = provider("2022-04-01T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee)
        assertThat(events.size).isEqualTo(1)
        val e1 = events[0]
        assertThat(e1.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(e1.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))

        val orderItem2 = OrderItemFactory(checkOut = "2022-04-10").get()
        val events2 = provider("2022-04-01T13:15:30.00Z").onConfirmation(orderItem2, enablePromoFee)
        assertThat(events2.size).isEqualTo(1)
        val e2 = events2[0]
        assertThat(e2.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(e2.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
    }

    @Test
    fun onConfirmation_promoMoney() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", discount = 3000.0, price = 10000.0).get()
        val events: List<FinancialEvent> = provider("2022-04-01T13:15:30.00Z")
            .onConfirmation(orderItem, enablePromoFee)
        assertThat(events.size).isEqualTo(1)
        val event = events[0]
        assertThat(event.type).isEqualTo(PAYMENT)
        assertThat(event.billingClientId).isEqualTo(OrderItemFactory.agreement.billingClientId)
        assertThat(event.partnerAmount).isEqualTo(rub(5700))
        assertThat(event.feeAmount).isEqualTo(rub(1300))
        assertThat(event.promoCodePartnerAmount).isEqualTo(rub(3000))
        assertThat(event.promoCodeFeeAmount).isNull()
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
        assertThat(event.promoCodePartnerRefundAmount).isNull()
        assertThat(event.promoCodeFeeRefundAmount).isNull()
    }

    @Test
    fun onConfirmation_promoMoney100Percent() {
        val enablePromoFee = true
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", discount = 9999.0, price = 10000.0).get()
        val events: List<FinancialEvent> = provider("2022-04-01T13:15:30.00Z")
            .onConfirmation(orderItem, enablePromoFee)
        assertThat(events.size).isEqualTo(1)
        val event = events[0]
        assertThat(event.type).isEqualTo(PAYMENT)
        assertThat(event.billingClientId).isEqualTo(OrderItemFactory.agreement.billingClientId)
        assertThat(event.partnerAmount).isEqualTo(rub(0))
        assertThat(event.feeAmount).isEqualTo(rub(1))
        assertThat(event.promoCodePartnerAmount).isEqualTo(rub(8700))
        assertThat(event.promoCodeFeeAmount).isEqualTo(rub(1299))
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
        assertThat(event.promoCodePartnerRefundAmount).isNull()
        assertThat(event.promoCodeFeeRefundAmount).isNull()
    }

    @Test
    fun onConfirmation_deferredPayment() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", discount = 1000.0, price = 10000.0).get()
        val paymentMoment = Instant.parse("2022-04-01T13:15:30.00Z").plus(1, DAYS)
        orderItem.order.paymentSchedule = PaymentSchedule.builder().state(PS_PARTIALLY_PAID).build()
        val provider = provider("2022-04-01T13:15:30.00Z")
        var events = provider.onConfirmation(orderItem, enablePromoFee)
        assertThat(events).hasSize(0)
        val schedule = getFullyPaidSchedule(paymentMoment)
        events = provider.onPaymentScheduleFullyPaid(orderItem, schedule, enablePromoFee)
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event.accrualAt).isEqualTo(Instant.parse("2022-04-01T13:15:30.00Z"))
        assertThat(event.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.totalAmount).isEqualTo(Money.of(10000, "RUB"))
        assertThat(event.partnerAmount).isEqualTo(Money.of(7700, "RUB"))
        assertThat(event.promoCodePartnerAmount).isEqualTo(Money.of(1000, "RUB"))
        assertThat(event.feeAmount).isEqualTo(Money.of(1300, "RUB"))
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
    }

    @Test
    fun onRefundEventStructure() {
        val enablePromoFee = false
        mockPaid(rub(10000), enablePromoFee)
        val orderItem = OrderItemFactory(checkOut = "2022-04-07", price = 10000.0, penalty = 2000.0).get()
        val events = provider("2022-04-01T13:15:30.00Z")
            .onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event.orderItem).isNotNull()
        assertThat(event.order).isNotNull()
        assertThat(event.orderPrettyId).isEqualTo("YA-PRETTY-ID")
        assertThat(event.type).isEqualTo(REFUND)
        assertThat(event.billingClientId).isEqualTo(110146753)
        assertThat(event.billingContractId).isEqualTo(4358521L)
        assertThat(event.accrualAt).isEqualTo(Instant.parse("2022-04-01T13:15:30.00Z"))
        assertThat(event.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(event.partnerAmount).isNull()
        assertThat(event.feeAmount).isNull()
        assertThat(event.partnerRefundAmount).isEqualTo(rub(6960))
        assertThat(event.feeRefundAmount).isEqualTo(rub(1040))
    }

    @Test
    fun onRefund() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0).get()
        val paymentEvents = provider("2022-04-01T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee)
        assertThat(paymentEvents.size).isEqualTo(1)
        val paymentEvent = paymentEvents[0]
        assertThat(paymentEvent.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(paymentEvent.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))

        mockPaid(rub(10000), enablePromoFee)

        val refundEvents = provider("2022-04-08T13:15:30.00Z")
            .onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(refundEvents.size).isEqualTo(1)
        val refundEvent = refundEvents[0]
        assertThat(refundEvent.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        assertThat(refundEvent.accountingActAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
    }

    @Test
    fun onRefund_promoMoneyWithCorrection() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 7000.0, penalty = 2000.0).get()

        mockPaid(rub(10000), enablePromoFee, rub(7000))

        val events = provider("2022-04-01T13:15:30.00Z")
            .onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(events).hasSize(2)
        val refundEvent = events.firstOrNull { e -> e.type == REFUND }
        assertThat(refundEvent).isNotNull()
        assertThat(refundEvent!!.type).isEqualTo(REFUND)
        assertThat(refundEvent.billingClientId).isEqualTo(OrderItemFactory.agreement.billingClientId)
        assertThat(refundEvent.partnerAmount).isNull()
        assertThat(refundEvent.feeAmount).isNull()
        assertThat(refundEvent.promoCodePartnerAmount).isNull()
        assertThat(refundEvent.promoCodeFeeAmount).isNull()
        assertThat(refundEvent.partnerRefundAmount).isEqualTo(rub(0))
        assertThat(refundEvent.feeRefundAmount).isEqualTo(rub(1040))
        assertThat(refundEvent.promoCodePartnerRefundAmount).isEqualTo(rub(7000))
        assertThat(refundEvent.promoCodeFeeRefundAmount).isNull()
        val correctionEvent = events.firstOrNull { e -> e.type == PAYMENT }
        assertThat(correctionEvent).isNotNull()
        assertThat(correctionEvent!!.type).isEqualTo(PAYMENT)
        assertThat(correctionEvent.billingClientId).isEqualTo(refundEvent.billingClientId)
        assertThat(correctionEvent.payoutAt).isEqualTo(refundEvent.payoutAt)
        assertThat(correctionEvent.accountingActAt).isEqualTo(refundEvent.accountingActAt)
        assertThat(correctionEvent.partnerAmount).isEqualTo(rub(40))
        assertThat(correctionEvent.feeAmount).isEqualTo(rub(0))
        assertThat(correctionEvent.promoCodePartnerAmount).isNull()
        assertThat(correctionEvent.promoCodeFeeAmount).isNull()
        assertThat(correctionEvent.partnerRefundAmount).isNull()
        assertThat(correctionEvent.feeRefundAmount).isNull()
        assertThat(correctionEvent.promoCodePartnerRefundAmount).isNull()
        assertThat(correctionEvent.promoCodeFeeRefundAmount).isNull()
    }

    @Test
    fun onRefund_promoMoneyPenalty() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 7000.0, penalty = 5000.0).get()

        mockPaid(rub(10000), enablePromoFee, rub(7000))

        val events = provider("2022-04-01T13:15:30.00Z")
            .onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(events).hasSize(2)
        val refundEvent = events.firstOrNull { e -> e.type == REFUND }
        assertThat(refundEvent).isNotNull()
        assertThat(refundEvent!!.feeRefundAmount).isEqualTo(rub(650))
        assertThat(refundEvent.promoCodePartnerRefundAmount).isEqualTo(rub(5000))
        assertThat(refundEvent.totalAmount).isEqualTo(rub(5650))
        refundEvent.ensureNoNegativeValues()
        val correctionEvent = events.firstOrNull { e -> e.type == PAYMENT }
        assertThat(correctionEvent).isNotNull()
        assertThat(correctionEvent!!.partnerAmount).isEqualTo(rub(650))
        assertThat(correctionEvent.totalAmount).isEqualTo(rub(650))
        correctionEvent.ensureNoNegativeValues()
    }

    @Test
    fun onRefund_DeferredPaymentWithoutPaid() {
        val enablePromoFee = false
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 1000.0, penalty = 5000.0).get()
        orderItem.order.paymentSchedule = PaymentSchedule.builder().state(PS_PARTIALLY_PAID).build()
        val provider = provider("2022-04-01T13:15:30.00Z")
        val events = provider.onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(events).hasSize(1)
        val event = events[0]
        assertThat(event.type).isEqualTo(PAYMENT)
        assertThat(event.totalAmount).isEqualTo(rub(5000))
        assertThat(event.promoCodePartnerAmount).isNull()
        assertThat(event.feeAmount).isEqualTo(rub(650))
        assertThat(event.partnerAmount).isEqualTo(rub(4350))
        assertThat(event.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
    }

    @Test
    fun onRefund_deferredPaymentWithFullyPaid() {
        val enablePromoFee = false
        val events = mutableListOf<FinancialEvent>()
        mockWithList(events)
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 1000.0, penalty = 2000.0).get()
        orderItem.order.paymentSchedule = PaymentSchedule.builder().state(PS_PARTIALLY_PAID).build()
        val provider = provider("2022-04-01T13:15:30.00Z")
        val paymentEvents = provider.onPaymentScheduleFullyPaid(
            orderItem,
            getFullyPaidSchedule(Instant.parse("2022-04-01T13:15:30.00Z")),
            enablePromoFee
        )
        assertThat(paymentEvents).hasSize(1)
        events.addAll(paymentEvents)
        var balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(8700))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(1300))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(1000))
        val refundEvents = provider.onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(refundEvents).hasSize(1)
        val refundEvent = refundEvents[0]
        assertThat(refundEvent.type).isEqualTo(REFUND)
        assertThat(refundEvent.totalAmount).isEqualTo(rub(8000))
        assertThat(refundEvent.payoutAt).isEqualTo(Instant.parse("2022-04-10T21:00:00.00Z"))
        events.addAll(refundEvents)
        balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(1740))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(260))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(0))
    }

    @Test
    fun onConfirm_fullyPaidWithExtraWithRefund() {
        val enablePromoFee = false
        val events = mutableListOf<FinancialEvent>()
        mockWithList(events)
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 1000.0, penalty = 2000.0).get()
        val provider = provider("2022-04-01T13:15:30.00Z")
        val confirmationEvents = provider.onConfirmation(orderItem, enablePromoFee)
        events.addAll(confirmationEvents)

        var balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(8700))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(1300))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(1000))

        val extraInvoice = PendingInvoice.builder()
            .order(orderItem.order)
            .pendingInvoiceItems(listOf(PendingInvoiceItem.builder().price(rub(800)).build()))
            .state(PS_FULLY_PAID)
            .closedAt(Instant.parse("2022-04-02T13:15:30.00Z"))
            .build()
        orderItem.addFiscalItem(FiscalItem.builder().moneyAmount(extraInvoice.totalAmount).build())

        orderItem.hotelItinerary.addExtra(extraInvoice.totalAmount)
        val extraEvents = provider.onExtraPayment(orderItem, extraInvoice, enablePromoFee)
        assertThat(extraEvents).hasSize(1)
        events.addAll(extraEvents)

        balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.total).isEqualTo(rub(10800))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(9396))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(1404))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(1000))

        val refundEvents = provider.onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(refundEvents).hasSize(1)
        events.addAll(refundEvents)
        balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.total).isEqualTo(rub(2000))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(1740))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(260))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(0))
    }

    @Test
    fun onPaymentScheduleFullyPaid_refundWithExtra() {
        val enablePromoFee = false
        val events = mutableListOf<FinancialEvent>()
        mockWithList(events)
        val orderItem = OrderItemFactory(checkOut = "2022-04-04", price = 10000.0, discount = 1000.0, penalty = 2000.0).get()
        val paymentSchedule = PaymentSchedule.builder().state(PS_PARTIALLY_PAID).build()
        orderItem.order.paymentSchedule = paymentSchedule
        val provider = provider("2022-04-01T13:15:30.00Z")
        val confirmationEvents = provider.onConfirmation(orderItem, false)
        assertThat(confirmationEvents).hasSize(0)

        val fullyPaidEvents = provider.onPaymentScheduleFullyPaid(
            orderItem,
            getFullyPaidSchedule(Instant.parse("2022-04-03T13:15:30.00Z")),
            false
        )
        paymentSchedule.state = PS_FULLY_PAID
        assertThat(fullyPaidEvents).hasSize(1)
        events.addAll(fullyPaidEvents)

        var balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.total).isEqualTo(rub(10000))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(8700))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(1300))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(1000))

        val extraInvoice = PendingInvoice.builder()
            .order(orderItem.order)
            .pendingInvoiceItems(listOf(PendingInvoiceItem.builder().price(Money.of(800, "RUB")).build()))
            .state(PS_FULLY_PAID)
            .closedAt(Instant.parse("2022-04-02T13:15:30.00Z"))
            .build()
        orderItem.addFiscalItem(FiscalItem.builder().moneyAmount(extraInvoice.totalAmount).build())
        orderItem.hotelItinerary.addExtra(extraInvoice.totalAmount)
        val extraEvents = provider.onExtraPayment(orderItem, extraInvoice, enablePromoFee)
        assertThat(extraEvents).hasSize(1)
        events.addAll(extraEvents)

        balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.total).isEqualTo(rub(10800))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(9396))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(1404))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(1000))

        val refundEvents = provider.onRefund(orderItem, FinancialEventService.DEFAULT_MONEY_REFUND_MODE, enablePromoFee)
        assertThat(refundEvents).hasSize(1)
        events.addAll(refundEvents)
        balance = ServiceBalance(events, Monetary.getCurrency("RUB"))
        assertThat(balance.overallBalance.total).isEqualTo(rub(2000))
        assertThat(balance.overallBalance.totalPartner).isEqualTo(rub(1740))
        assertThat(balance.overallBalance.totalFee).isEqualTo(rub(260))
        assertThat(balance.overallBalance.totalPromo).isEqualTo(rub(0))
    }
}

class OrderItemFactory(
    private val price: Double = 100.0,
    private val discount: Double? = null,
    private val checkOut: String = LocalDate.now().plusYears(1000).toString(),
    private val schedule: PaymentSchedule? = null,
    private val refundInfo: RefundInfo? = null,
    private val penalty: Double = 0.0
) {
    companion object {
        val agreement = BronevikBillingPartnerAgreement.builder()
            .billingClientId(110146753L)
            .billingContractId(4358521L)
            .confirmRate(BigDecimal.valueOf(0.13))
            .build()
    }

    fun get(): BronevikOrderItem {
        val orderItem = BronevikOrderItem()
        val order = HotelOrder()
        order.prettyId = "YA-PRETTY-ID"
        order.currency = ProtoCurrencyUnit.RUB
        order.paymentSchedule = schedule
        orderItem.order = order

        val itinerary = BronevikHotelItinerary()
        val totalPrice = rub(price)
        itinerary.fiscalPrice = totalPrice
        itinerary.actualPrice = totalPrice
        itinerary.refundInfo = refundInfo
        itinerary.orderDetails = OrderDetails.builder()
            .checkoutDate(LocalDate.parse(checkOut))
            .build()
        orderItem.itinerary = itinerary

        orderItem.billingPartnerAgreement = agreement

        val fiscalItem = FiscalItem.builder()
            .moneyAmount(totalPrice)
            .build()
        orderItem.addFiscalItem(fiscalItem)

        if (discount != null) {
            orderItem.fiscalItems[0].applyDiscount(rub(discount))
        }

        val refund = itinerary.fiscalPrice.subtract(Money.of(penalty, "RUB")).getNumber()
        val refundInfo = RefundInfo()
        refundInfo.penalty = BaseRate(penalty.toString(), "RUB")
        refundInfo.refund = BaseRate(refund.toString(), "RUB")
        refundInfo.penalty = BaseRate(penalty.toString(), "RUB")
        itinerary.refundInfo = refundInfo

        return orderItem
    }
}
