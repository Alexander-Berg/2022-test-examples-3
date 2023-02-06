package ru.yandex.travel.orders.services.finances.providers

import org.assertj.core.api.Assertions.assertThat
import org.javamoney.moneta.Money
import org.junit.Test
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit
import ru.yandex.travel.orders.entities.TrainOrder
import ru.yandex.travel.orders.entities.TrainOrderItem
import ru.yandex.travel.orders.entities.TrainOrderUserRefund
import ru.yandex.travel.orders.entities.TrainTicketRefund
import ru.yandex.travel.orders.entities.finances.FinancialEvent
import ru.yandex.travel.orders.entities.finances.FinancialEventType
import ru.yandex.travel.orders.entities.partners.TrainBillingPartnerAgreement
import ru.yandex.travel.orders.entities.setTrainTicketRefunds
import ru.yandex.travel.orders.proto.EOrderRefundState
import ru.yandex.travel.orders.workflows.orderitem.train.TrainWorkflowProperties
import ru.yandex.travel.train.model.Insurance
import ru.yandex.travel.train.model.InsuranceStatus
import ru.yandex.travel.train.model.TrainPassenger
import ru.yandex.travel.train.model.TrainReservation
import ru.yandex.travel.train.model.TrainTicket
import ru.yandex.travel.train.model.refund.PassengerRefundInfo
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus
import ru.yandex.travel.workflow.entities.Workflow
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

class TrainFinancialDataProviderTest {
    private val TICKET_CLIENT_ID = 1L
    private val INSURANCE_CLIENT_ID = 2L
    private val PRETTY_ID = "YA-IS-PRETTY-ID"
    private val CURRENT_MOMENT = Instant.parse("2021-01-10T13:00:00.00Z")

    private fun providerWithClockFixed(currentTimeUtc: String): TrainFinancialDataProvider {
        val clock = Clock.fixed(Instant.parse(currentTimeUtc), ZoneId.of("UTC"))
        return TrainFinancialDataProvider(clock, TrainWorkflowProperties())
    }

    @Test
    fun onConfirmationEventStructure() {
        val orderItem = createOrderItem(CURRENT_MOMENT.toString(), InsuranceStatus.CHECKOUT_FAILED)
        val finEvents: List<FinancialEvent> = providerWithClockFixed("2021-01-10T12:00:00.00Z").onConfirmation(orderItem, false)
        assertThat(finEvents.size).isEqualTo(1)
        val event = finEvents[0]
        assertThat(event.orderItem).isNotNull
        assertThat(event.order).isNotNull
        assertThat(event.orderPrettyId).isEqualTo(PRETTY_ID)
        assertThat(event.type).isEqualTo(FinancialEventType.PAYMENT)
        assertThat(event.billingClientId).isEqualTo(TICKET_CLIENT_ID)
        assertThat(event.accrualAt).isEqualTo(Instant.parse("2021-01-10T12:00:00.00Z"))
        assertThat(event.payoutAt).isEqualTo(mskDtToUtcInstant("2021-01-10T00:00:00"))
        assertThat(event.accountingActAt).isEqualTo(mskDtToUtcInstant("2021-01-10T00:00:00"))
        // @ MSK
        assertThat(event.partnerAmount).isEqualTo(Money.of(120, "RUB"))
        assertThat(event.feeAmount).isEqualTo(Money.of(60, "RUB"))
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
        assertThat(event.partnerFeeAmount).isEqualTo(Money.of(30, ProtoCurrencyUnit.RUB))
    }

    @Test
    fun onConfirmationWithInsurance() {
        val orderItem = createOrderItem(CURRENT_MOMENT.toString(), InsuranceStatus.CHECKED_OUT)
        val finEvents: List<FinancialEvent> = providerWithClockFixed("2021-01-10T12:00:00.00Z").onConfirmation(orderItem, false)
        assertThat(finEvents.size).isEqualTo(2)
        val event = finEvents[0]
        assertThat(event.type).isEqualTo(FinancialEventType.PAYMENT)
        assertThat(event.billingClientId).isEqualTo(TICKET_CLIENT_ID)
        // @ MSK
        assertThat(event.partnerAmount).isEqualTo(Money.of(120, "RUB"))
        assertThat(event.feeAmount).isEqualTo(Money.of(60, "RUB"))
        assertThat(event.partnerRefundAmount).isNull()
        assertThat(event.feeRefundAmount).isNull()
        assertThat(event.partnerFeeAmount).isEqualTo(Money.of(30, ProtoCurrencyUnit.RUB))
        val insEvent = finEvents[1]
        assertThat(insEvent.type).isEqualTo(FinancialEventType.INSURANCE_PAYMENT)
        assertThat(insEvent.billingClientId).isEqualTo(INSURANCE_CLIENT_ID)
        // @ MSK
        assertThat(insEvent.partnerAmount).isEqualTo(Money.of(21, "RUB"))
        assertThat(insEvent.feeAmount).isEqualTo(Money.of(39, "RUB"))
        assertThat(insEvent.partnerRefundAmount).isNull()
        assertThat(insEvent.feeRefundAmount).isNull()
        assertThat(insEvent.partnerFeeAmount).isNull()
    }

    @Test
    fun onCompleteRefund() {
        val enablePromoFee = false;
        val orderRefund = TrainOrderUserRefund().also {
            it.id = UUID.randomUUID()
            it.state = EOrderRefundState.RS_REFUNDED
        }
        val orderItem = createOrderItem(CURRENT_MOMENT.toString(), InsuranceStatus.CHECKED_OUT).also {
            val passengerRefundInfo: List<PassengerRefundInfo> = it.reservation.passengers.map {passenger ->
                val info = PassengerRefundInfo()
                info.refundOperationStatus = ImOperationStatus.OK
                info.customerId = passenger.customerId
                info.actualRefundTicketAmount = passenger.ticket.tariffAmount.add(passenger.ticket.serviceAmount)
                info.calculatedRefundFeeAmount = passenger.ticket.calculateRefundFeeAmount()
                info.calculatedRefundInsuranceAmount = passenger.insurance.amount
                info.buyOperationId = passenger.ticket.partnerBuyOperationId
                info
            }
            setTrainTicketRefunds(it, listOf(TrainTicketRefund.createRefund(it, passengerRefundInfo, orderRefund)))
        }
        val finEvents: List<FinancialEvent> = providerWithClockFixed("2021-01-10T12:00:00.00Z")
            .onPartialRefund(orderItem, orderRefund, enablePromoFee)
        assertThat(finEvents.size).isEqualTo(2)
        val event = finEvents[0]
        assertThat(event.orderItem).isNotNull
        assertThat(event.order).isNotNull
        assertThat(event.orderPrettyId).isEqualTo(PRETTY_ID)
        assertThat(event.type).isEqualTo(FinancialEventType.REFUND)
        assertThat(event.billingClientId).isEqualTo(TICKET_CLIENT_ID)
        assertThat(event.accrualAt).isEqualTo(Instant.parse("2021-01-10T12:00:00.00Z"))
        assertThat(event.payoutAt).isEqualTo(Instant.parse("2021-01-10T13:00:20.00Z"))
        assertThat(event.accountingActAt).isEqualTo(Instant.parse("2021-01-10T13:00:20.00Z"))
        // @ MSK
        assertThat(event.partnerAmount).isNull()
        assertThat(event.feeAmount).isNull()
        assertThat(event.partnerRefundAmount).isEqualTo(Money.of(120, "RUB"))
        assertThat(event.feeRefundAmount).isEqualTo(Money.of(0, "RUB"))
        assertThat(event.partnerFeeAmount).isEqualTo(Money.of(30, ProtoCurrencyUnit.RUB))
        val insuranceEvent = finEvents[1]
        assertThat(insuranceEvent.type).isEqualTo(FinancialEventType.INSURANCE_REFUND)
        assertThat(insuranceEvent.billingClientId).isEqualTo(INSURANCE_CLIENT_ID)
        // @ MSK
        assertThat(insuranceEvent.partnerAmount).isNull()
        assertThat(insuranceEvent.feeAmount).isNull()
        assertThat(insuranceEvent.partnerRefundAmount).isEqualTo(Money.of(21, "RUB"))
        assertThat(insuranceEvent.feeRefundAmount).isEqualTo(Money.of(39, "RUB"))
        assertThat(insuranceEvent.partnerFeeAmount).isNull()
    }

    private fun createOrderItem(nowStr: String, insuranceStatus: InsuranceStatus): TrainOrderItem {
        val now = Instant.parse(nowStr)
        val orderItem = TrainOrderItem()
        orderItem.workflow = Workflow().also { it.id = UUID.randomUUID() }
        val order = TrainOrder()
        order.prettyId = PRETTY_ID
        orderItem.order = order
        val trainReservation = TrainReservation()

        trainReservation.passengers = listOf(1L, 2L, 3L).map { createPassenger(it) }
        trainReservation.insuranceStatus = insuranceStatus

        orderItem.reservation = trainReservation
        orderItem.confirmedAt = now.plusSeconds(10)
        orderItem.refundedAt = now.plusSeconds(20)
        orderItem.billingPartnerAgreement = getAgreement()

        return orderItem
    }

    private fun createPassenger(number: Long): TrainPassenger {
        val moneyAmount = BigDecimal.TEN.multiply(BigDecimal.valueOf(number))
        return TrainPassenger.builder()
            .customerId(number.toInt())
            .firstName(number.toString())
            .lastName(number.toString())
            .ticket(TrainTicket().also {
                it.tariffAmount = Money.of(moneyAmount, ProtoCurrencyUnit.RUB)
                it.serviceAmount = Money.of(moneyAmount, ProtoCurrencyUnit.RUB)
                it.feeAmount = Money.of(moneyAmount, ProtoCurrencyUnit.RUB)
                it.partnerFee = Money.of(BigDecimal.TEN, ProtoCurrencyUnit.RUB)
                it.partnerRefundFee = Money.of(BigDecimal.TEN, ProtoCurrencyUnit.RUB)
            })
            .insurance(Insurance().also {
                it.amount = Money.of(moneyAmount, ProtoCurrencyUnit.RUB)
            })
            .build()
    }

    private fun getAgreement() = TrainBillingPartnerAgreement.builder()
        .billingClientId(TICKET_CLIENT_ID)
        .insuranceClientId(INSURANCE_CLIENT_ID)
        .insuranceFeeCoefficient(BigDecimal.valueOf(0.65))
        .build()

    private fun mskDtToUtcInstant(mskDt: String): Instant {
        return LocalDateTime.parse(mskDt).atZone(ZoneId.of("Europe/Moscow")).toInstant()
    }
}
