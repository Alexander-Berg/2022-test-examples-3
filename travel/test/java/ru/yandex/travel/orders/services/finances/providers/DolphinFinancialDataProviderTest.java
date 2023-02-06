package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.javamoney.moneta.Money;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.hotels.common.orders.BaseRate;
import ru.yandex.travel.hotels.common.orders.DolphinHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.hotels.common.orders.RefundInfo;
import ru.yandex.travel.hotels.common.orders.RefundReason;
import ru.yandex.travel.hotels.common.partners.dolphin.model.AverageNightStayFeeRefundParams;
import ru.yandex.travel.orders.entities.DolphinOrderItem;
import ru.yandex.travel.orders.entities.FiscalItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.PaymentSchedule;
import ru.yandex.travel.orders.entities.PendingInvoice;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.entities.partners.DolphinBillingPartnerAgreement;
import ru.yandex.travel.orders.entities.promo.FiscalItemDiscount;
import ru.yandex.travel.orders.repository.FinancialEventRepository;
import ru.yandex.travel.orders.workflow.payments.proto.EPaymentState;
import ru.yandex.travel.orders.workflows.orderitem.dolphin.DolphinProperties;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.PAYMENT;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.REFUND;

@RunWith(SpringRunner.class)
public class DolphinFinancialDataProviderTest {
    private final static String CUR_RUB = "RUB";
    private final static DolphinBillingPartnerAgreement TEST_AGREEMENT = DolphinBillingPartnerAgreement.builder()
            .billingClientId(110146753L)
            .billingContractId(4358521L)
            .confirmRate(BigDecimal.valueOf(0.13))
            .build();
    private final static Map<Integer, BigDecimal> REFUND_PENALTIES = Map.of(
            0, BigDecimal.valueOf(0.8),
            5, BigDecimal.ZERO
    );

    @Mock
    private FinancialEventRepository financialEventRepository;

    private static Order order() {
        return new Order();
    }

    private void mockNoEvents() {
        when(financialEventRepository.findAllByOrderItem(any())).thenReturn(Collections.emptyList());
    }

    private void mockWithList(List<FinancialEvent> list) {
        when(financialEventRepository.findAllByOrderItem(any())).thenReturn(list);
    }

    private void mockPaid(Money amountPaid, Money promoMoney, Boolean enablePromoFee) {
        FullMoneySplitCalculator calculator = new FullMoneySplitCalculator();
        when(financialEventRepository.findAllByOrderItem(any())).thenAnswer(a -> {
            List<FinancialEvent> result = new ArrayList<>(1);
            MoneySplit split = ProviderHelper.splitMoney(amountPaid,
                    BigDecimal.valueOf(0.13));
            FullMoneySplit fullSplit = calculator.calculatePaymentWithPromoMoney(split,
                    promoMoney);
            FinancialEvent event = FinancialEvent.builder()
                    .type(PAYMENT)
                    .build();
            ProviderHelper.setPaymentMoney(event, fullSplit, enablePromoFee);
            result.add(event);
            return result;
        });
    }

    private void mockPaid(Money amountPaid, Boolean enablePromoFee) {
        mockPaid(amountPaid, Money.of(0, CUR_RUB), enablePromoFee);
    }

    @Test
    public void onConfirmationEventStructure() {
        mockNoEvents();
        var enablePromoFee = false;
        var oi = order().price(10_000).checkInTs("2019-12-18T13:15:30.00Z").checkOut("2019-12-23").get();
        List<FinancialEvent> events = provider("2019-11-12T13:15:30.00Z").onConfirmation(oi, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getOrderItem()).isNotNull();
        assertThat(event.getOrder()).isNotNull();
        assertThat(event.getOrderPrettyId()).isEqualTo("YA-PRETTY-ID");
        assertThat(event.getType()).isEqualTo(FinancialEventType.PAYMENT);
        assertThat(event.getBillingClientId()).isEqualTo(110146753);
        assertThat(event.getBillingContractId()).isEqualTo(4358521L);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-12T13:15:30.00Z"));
        assertThat(event.getPayoutAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // Dec 31, 00:00 @ MSC
        assertThat(event.getAccountingActAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // same as payout
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(8_700, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1_300, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isNull();
        assertThat(event.getPromoCodeFeeAmount()).isNull();
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
        assertThat(event.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(event.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onConfirmationPayoutAndSettlementDates() {
        mockNoEvents();
        Boolean enablePromoFee = false;
        DolphinFinancialDataProvider provider = provider("2019-11-12T13:15:30.00Z");

        List<FinancialEvent> es1 = provider.onConfirmation(order().checkOut("2019-11-30").get(), enablePromoFee);
        assertThat(es1.size()).isEqualTo(1);
        var e1 = es1.get(0);
        assertThat(e1.getPayoutAt())
                .isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")) // Nov 30, 00:00 @ MSC
                .isEqualTo(e1.getAccountingActAt());

        List<FinancialEvent> es2 = provider.onConfirmation(order().checkOut("2019-12-01").get(), enablePromoFee);
        assertThat(es2.size()).isEqualTo(1);
        var e2 = es2.get(0);
        assertThat(e2.getPayoutAt())
                .isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")) // Dec 31, 00:00 @ MSC
                .isEqualTo(e2.getAccountingActAt());
    }

    @Test
    public void onConfirmationRounding() {
        mockNoEvents();
        var enablePromoFee = false;

        DolphinFinancialDataProvider provider = provider("2019-11-12T13:15:30.00Z");

        FinancialEvent e1 = provider.onConfirmation(order().price(10_000.005).get(), enablePromoFee).get(0);
        assertThat(e1.getPartnerAmount()).isEqualTo(Money.of(8_700.01, "RUB"));
        assertThat(e1.getFeeAmount()).isEqualTo(Money.of(1_300.00, "RUB"));

        FinancialEvent e2 = provider.onConfirmation(order().price(10_000.004).get(), enablePromoFee).get(0);
        assertThat(e2.getPartnerAmount()).isEqualTo(Money.of(8_700.00, "RUB"));
        assertThat(e2.getFeeAmount()).isEqualTo(Money.of(1_300.00, "RUB"));

        FinancialEvent e3 = provider.onConfirmation(order().price(10_000.03).get(), enablePromoFee).get(0);
        assertThat(e3.getPartnerAmount()).isEqualTo(Money.of(8_700.03, "RUB"));
        assertThat(e3.getFeeAmount()).isEqualTo(Money.of(1_300.00, "RUB"));

        FinancialEvent e4 = provider.onConfirmation(order().price(10_000.04).get(), enablePromoFee).get(0);
        assertThat(e4.getPartnerAmount()).isEqualTo(Money.of(8_700.03, "RUB"));
        assertThat(e4.getFeeAmount()).isEqualTo(Money.of(1_300.01, "RUB"));
    }

    @Test
    public void onCancellationEventStructure() {
        var enablePromoFee = false;
        mockPaid(Money.of(10000, "RUB"), enablePromoFee);
        FinancialEvent event = getOnlyEvent(provider("2019-11-13T10:15:30.00Z").onRefund(
                order().price(10_000).checkInTs("2019-11-17T13:15:30.00Z").checkOut("2019-12-23").get()));
        assertThat(event.getOrderItem()).isNotNull();
        assertThat(event.getOrder()).isNotNull();
        assertThat(event.getOrderPrettyId()).isEqualTo("YA-PRETTY-ID");
        assertThat(event.getType()).isEqualTo(REFUND);
        assertThat(event.getBillingClientId()).isEqualTo(110146753);
        assertThat(event.getBillingContractId()).isEqualTo(4358521L);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-13T10:15:30.00Z"));
        assertThat(event.getPayoutAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // Dec 31, 00:00 @ MSC
        assertThat(event.getAccountingActAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // same as payout
        assertThat(event.getPartnerAmount()).isNull();
        assertThat(event.getFeeAmount()).isNull();
        assertThat(event.getPartnerRefundAmount()).isEqualTo(Money.of(1740, "RUB"));
        assertThat(event.getFeeRefundAmount()).isEqualTo(Money.of(260, "RUB"));
    }

    @Test
    public void onCancellationPayoutAndSettlementDates() {
        var enablePromoFee = false;
        mockPaid(Money.of(10000, "RUB"), enablePromoFee);
        FinancialEvent e1 = getOnlyEvent(provider("2019-11-13T20:15:30.00Z")
                .onRefund(order().price(10000).checkInTs("2019-11-19T11:00:00.00Z").checkOut("2019-11-30").get()));
        assertThat(e1.getPayoutAt())
                .isEqualTo(Instant.parse("2019-11-29T21:00:00.00Z")) // Nov 30, 00:00 @ MSC
                .isEqualTo(e1.getAccountingActAt());

        FinancialEvent e2 = getOnlyEvent(provider("2019-11-13T20:00:00.00Z")
                .onRefund(order().price(10000).checkInTs("2019-11-19T11:00:00.00Z").checkOut("2019-12-01").get()));
        assertThat(e2.getPayoutAt())
                .isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")) // Dec 31, 00:00 @ MSC
                .isEqualTo(e2.getAccountingActAt());
    }

    @Test
    public void onCancellationPenalties() {
        var enablePromoFee = false;
        mockPaid(Money.of(10000, "RUB"), enablePromoFee);
        DolphinOrderItem order = order().price(10_000).checkInTs("2019-11-19T11:00:00.00Z").get();

        // no penalties
        FinancialEvent e1 = getOnlyEvent(provider("2019-11-13T20:15:30.00Z").onRefund(order));
        assertThat(e1.getPartnerRefundAmount()).isEqualTo(Money.of(8_700, "RUB"));
        assertThat(e1.getFeeRefundAmount()).isEqualTo(Money.of(1_300, "RUB"));

        // 80% penalty
        FinancialEvent e2 = getOnlyEvent(provider("2019-11-14T11:00:00.00Z").onRefund(order));
        assertThat(e2.getPartnerRefundAmount()).isEqualTo(Money.of(1740, "RUB"));
        assertThat(e2.getFeeRefundAmount()).isEqualTo(Money.of(260, "RUB"));

        // no actual refund
        List<FinancialEvent> e3 = provider("2019-11-19T11:00:00.00Z").onRefund(order);
        assertThat(e3).isEmpty();
    }

    @Test
    public void onNewCancellationPenalties() {
        var enablePromoFee = false;
        DolphinOrderItem order = order().price(10_000).refundRulesVersion(1).checkInTs("2019-11-19T11:00:00.00Z").get();
        mockPaid(Money.of(10000, "RUB"), enablePromoFee);
        // no penalties
        FinancialEvent e1 = getOnlyEvent(provider("2019-11-15T20:15:30.00Z").onRefund(order));
        assertThat(e1.getPartnerRefundAmount()).isEqualTo(Money.of(8_700, "RUB"));
        assertThat(e1.getFeeRefundAmount()).isEqualTo(Money.of(1_300, "RUB"));

        // one night penalty
        FinancialEvent e2 = getOnlyEvent(provider("2019-11-16T11:00:00.00Z").onRefund(order));
        assertThat(e2.getPartnerRefundAmount()).isEqualTo(Money.of(6_960, "RUB"));
        assertThat(e2.getFeeRefundAmount()).isEqualTo(Money.of(1_040, "RUB"));

        // no actual refund
        List<FinancialEvent> e3 = provider("2019-11-19T11:00:00.00Z").onRefund(order);
        assertThat(e3).isEmpty();
    }

    @Test
    public void onCancellationRounding() {
        var enablePromoFee = false;
        // total refund sum rounding: total refund = round(total * 0.2, 2)
        mockPaid(Money.of(10000.02, "RUB"), enablePromoFee);
        FinancialEvent e1 = getOnlyEvent(provider("2019-11-14T11:00:00.00Z")
                .onRefund(order().price(10_000.02).checkInTs("2019-11-19T11:00:00.00Z").get()));
        assertThat(e1.getPartnerRefundAmount().add(e1.getFeeRefundAmount()))
                .isEqualTo(Money.of(2_000.00, "RUB"));

        mockPaid(Money.of(10000.03, "RUB"), enablePromoFee);
        FinancialEvent e2 = getOnlyEvent(provider("2019-11-14T11:00:00.00Z")
                .onRefund(order().price(10_000.03).checkInTs("2019-11-19T11:00:00.00Z").get()));
        assertThat(e2.getPartnerRefundAmount().add(e2.getFeeRefundAmount()))
                .isEqualTo(Money.of(2_000.01, "RUB"));

        // partner/fee rounding
        mockPaid(Money.of(10002.5, "RUB"), enablePromoFee);
        FinancialEvent e3 = getOnlyEvent(provider("2019-11-14T11:00:00.00Z")
                .onRefund(order().price(10_002.5).checkInTs("2019-11-19T11:00:00.00Z").get()));
        // fee refund = round(total refund * 0.13, 2), partner refund = total refund - fee refund
        assertThat(e3.getPartnerRefundAmount()).isEqualTo(Money.of(1740.43, "RUB"));
        assertThat(e3.getFeeRefundAmount()).isEqualTo(Money.of(260.07, "RUB"));
    }

    @Test
    public void onConfirmation_promoCodeCost() {
        var enablePromoFee = false;
        List<FinancialEvent> events = provider("2019-11-12T13:15:30.00Z").onConfirmation(
                order().price(10_000).discount(3_000.0).checkInTs("2019-12-18T13:15:30.00Z").checkOut("2019-12-23").get(),
                enablePromoFee
        );
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(5_700, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1_300, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(3_000, "RUB"));
        assertThat(event.getPromoCodeFeeAmount()).isNull();
        assertThat(event.getPartnerRefundAmount()).isNull();
        assertThat(event.getFeeRefundAmount()).isNull();
        assertThat(event.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(event.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onConfirmation_promoCodeRewardDisabled() {
        var enablePromoFee = false;
        assertThatThrownBy(() -> provider("2019-11-12T13:15:30.00Z").onConfirmation(
                order().price(10_000)
                        .discount(9_000.0)
                        .checkInTs("2019-12-18T13:15:30.00Z")
                        .checkOut("2019-12-23")
                        .get(),
                enablePromoFee))
                .hasMessageContaining("Promo code money can not be used with the reward payment type");
    }

    @Test
    public void onConfirmation_promoCodeRewardEnabled() {
        var enablePromoFee = true;
        List<FinancialEvent> events = provider("2019-11-12T13:15:30.00Z").onConfirmation(
                order().price(10_000)
                        .discount(9_999.0)
                        .checkInTs("2019-12-18T13:15:30.00Z")
                        .checkOut("2019-12-23")
                        .get(),
                enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        FinancialEvent event = events.get(0);
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(0, CUR_RUB));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(1, CUR_RUB));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(8_700, CUR_RUB));
        assertThat(event.getPromoCodeFeeAmount()).isEqualTo(Money.of(1_299, CUR_RUB));
    }

    @Test
    public void onConfirmation_promoCodeExceedsActualPrice() {
        var enablePromoFee = false;
        DolphinOrderItem orderItem = order().price(10_000).discount(10_000.0)
                .checkInTs("2019-12-18T13:15:30.00Z").checkOut("2019-12-23").get();
        orderItem.getItinerary().setActualPrice(Money.of(9_990, "RUB"));
        // promo_reward is disabled at the moment, to use the 100% promo code we need to disable our reward too
        orderItem.setBillingPartnerAgreement(TEST_AGREEMENT.toBuilder().confirmRate(BigDecimal.ZERO).build());

        List<FinancialEvent> events = provider("2019-11-12T13:15:30.00Z").onConfirmation(orderItem, enablePromoFee);
        assertThat(events.size()).isEqualTo(1);
        var event = events.get(0);
        assertThat(event.getPartnerAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(event.getFeeAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(event.getPromoCodePartnerAmount()).isEqualTo(Money.of(9_990, "RUB"));
        assertThat(event.getPromoCodeFeeAmount()).isNull();
    }

    @Test
    public void onRefund_promoCodeCostNoCorrection() {
        var enablePromoFee = false;
        // 1500 promo code money, total refund - 2000 (1500 promo code, 500 user)
        mockPaid(Money.of(10000, "RUB"), Money.of(1500, "RUB"), enablePromoFee);
        List<FinancialEvent> events = provider("2019-12-15T00:00:00.00Z").onRefund(order()
                .price(10_000).discount(1_500.0).checkInTs("2019-12-18T13:15:30.00Z").checkOut("2019-12-23").get());

        assertThat(events).hasSize(1);

        FinancialEvent refundEvent = events.get(0);
        assertThat(refundEvent.getPartnerAmount()).isNull();
        assertThat(refundEvent.getFeeAmount()).isNull();
        assertThat(refundEvent.getPromoCodePartnerAmount()).isNull();
        assertThat(refundEvent.getPromoCodeFeeAmount()).isNull();
        assertThat(refundEvent.getPartnerRefundAmount()).isEqualTo(Money.of(240, "RUB"));
        assertThat(refundEvent.getFeeRefundAmount()).isEqualTo(Money.of(260, "RUB"));
        assertThat(refundEvent.getPromoCodePartnerRefundAmount()).isEqualTo(Money.of(1_500, "RUB"));
        assertThat(refundEvent.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void onRefund_promoCodeCostWithCorrection() {
        var enablePromoFee = false;
        // 2500 promo code money, total refund - 2000 (2000 promo code, 0 user)
        mockPaid(Money.of(10000, "RUB"), Money.of(2500, "RUB"), enablePromoFee);
        List<FinancialEvent> events = provider("2019-12-15T00:00:00.00Z").onRefund(order()
                .price(10_000).discount(2_500.0).checkInTs("2019-12-18T13:15:30.00Z").checkOut("2019-12-23").get());

        FinancialEvent refundEvent = events.stream().filter(e -> e.getType() == REFUND).findAny().orElse(null);
        assertThat(refundEvent).isNotNull();
        assertThat(refundEvent.getOrderPrettyId()).isEqualTo("YA-PRETTY-ID");
        assertThat(refundEvent.getPartnerAmount()).isNull();
        assertThat(refundEvent.getFeeAmount()).isNull();
        assertThat(refundEvent.getPromoCodePartnerAmount()).isNull();
        assertThat(refundEvent.getPromoCodeFeeAmount()).isNull();
        // partnerRefundAmount is calculated as -260 at first, then it is separated as a correction
        assertThat(refundEvent.getPartnerRefundAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(refundEvent.getFeeRefundAmount()).isEqualTo(Money.of(260, "RUB"));
        assertThat(refundEvent.getPromoCodePartnerRefundAmount()).isEqualTo(Money.of(2_000, "RUB"));
        assertThat(refundEvent.getPromoCodeFeeRefundAmount()).isNull();

        FinancialEvent correctionEvent = events.stream().filter(e -> e.getType() == PAYMENT).findAny().orElse(null);
        assertThat(correctionEvent).isNotNull();
        assertThat(correctionEvent.getOrderPrettyId()).isEqualTo(refundEvent.getOrderPrettyId());
        assertThat(correctionEvent.getBillingClientId()).isEqualTo(refundEvent.getBillingClientId());
        assertThat(correctionEvent.getBillingContractId()).isEqualTo(refundEvent.getBillingContractId());
        assertThat(correctionEvent.getPayoutAt()).isEqualTo(refundEvent.getPayoutAt());
        assertThat(correctionEvent.getAccountingActAt()).isEqualTo(refundEvent.getAccountingActAt());
        assertThat(correctionEvent.getPartnerAmount()).isEqualTo(Money.of(260, "RUB"));
        assertThat(correctionEvent.getFeeAmount()).isEqualTo(Money.of(0, "RUB"));
        assertThat(correctionEvent.getPromoCodePartnerAmount()).isNull();
        assertThat(correctionEvent.getPromoCodeFeeAmount()).isNull();
        assertThat(correctionEvent.getPartnerRefundAmount()).isNull();
        assertThat(correctionEvent.getFeeRefundAmount()).isNull();
        assertThat(correctionEvent.getPromoCodePartnerRefundAmount()).isNull();
        assertThat(correctionEvent.getPromoCodeFeeRefundAmount()).isNull();
    }

    @Test
    public void prepaymentThenFullPaymentThenExtraThenRefund() {
        var enablePromoFee = false;
        List<FinancialEvent> events = new ArrayList<>();
        mockWithList(events);
        var o = order()
                .price(10_000).discount(2_500.0)
                .checkInTs("2019-12-18T13:15:30.00Z")
                .checkOut("2019-12-23")
                .schedule(PaymentSchedule.builder()
                        .build())
                .get();
        DolphinFinancialDataProvider provider = provider("2019-12-15T00:00:00.00Z");

        var finEvents = provider.onConfirmation(o, enablePromoFee);
        assertThat(finEvents).hasSize(0);

        finEvents = provider.onPaymentScheduleFullyPaid(o, o.getOrder().getPaymentSchedule(), enablePromoFee);
        assertThat(finEvents).hasSize(1);
        events.addAll(finEvents);

        o.getOrder().getPaymentSchedule().setState(EPaymentState.PS_FULLY_PAID);
        o.addFiscalItem(FiscalItem.builder()
                .moneyAmount(Money.of(200, "RUB"))
                .build());
        o.getHotelItinerary().addExtra(Money.of(200, "RUB"));
        var extraEvent = getOnlyEvent(provider.onExtraPayment(o, PendingInvoice.builder().build(), enablePromoFee));
        assertThat(extraEvent.getType()).isEqualTo(PAYMENT);
        assertThat(extraEvent.getPartnerAmount()).isEqualTo(Money.of(174, "RUB"));
        assertThat(extraEvent.getFeeAmount()).isEqualTo(Money.of(26, "RUB"));
        events.add(extraEvent);

        List<FinancialEvent> refundEvents = provider("2019-11-15T20:15:30.00Z").onRefund(o);
        assertThat(refundEvents).hasSize(2);
        events.addAll(refundEvents);
        ServiceBalance finalBalance = new ServiceBalance(events, ProtoCurrencyUnit.RUB);
        assertThat(finalBalance.getOverallBalance().getTotal()).isEqualTo(Money.of(0, "RUB"));
    }

    @Test
    public void onCancellationWithOperatorRefund() {
        var enablePromoFee = false;
        RefundInfo ri = new RefundInfo();
        ri.setPenalty(new BaseRate("1000", "RUB"));
        ri.setRefund(new BaseRate("9000", "RUB"));
        ri.setReason(RefundReason.OPERATOR);
        var o = order().price(10000)
                .checkInTs("2019-11-17T13:15:30.00Z")
                .checkOut("2019-12-23")
                .refundInfo(ri)
                .get();
        mockPaid(Money.of(10000, "RUB"), enablePromoFee);
        FinancialEvent event = getOnlyEvent(provider("2019-11-13T10:15:30.00Z")
                .onRefund(o));
        assertThat(event.getOrderItem()).isNotNull();
        assertThat(event.getOrder()).isNotNull();
        assertThat(event.getOrderPrettyId()).isEqualTo("YA-PRETTY-ID");
        assertThat(event.getType()).isEqualTo(REFUND);
        assertThat(event.getBillingClientId()).isEqualTo(110146753);
        assertThat(event.getBillingContractId()).isEqualTo(4358521L);
        assertThat(event.getAccrualAt()).isEqualTo(Instant.parse("2019-11-13T10:15:30Z"));
        assertThat(event.getPayoutAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // Dec 31, 00:00 @ MSC
        assertThat(event.getAccountingActAt()).isEqualTo(Instant.parse("2019-12-30T21:00:00.00Z")); // same as payout
        assertThat(event.getPartnerAmount()).isNull();
        assertThat(event.getFeeAmount()).isNull();
        assertThat(event.getPartnerRefundAmount()).isEqualTo(Money.of(7830, "RUB"));
        assertThat(event.getFeeRefundAmount()).isEqualTo(Money.of(1170, "RUB"));
    }

    private DolphinFinancialDataProvider provider(String currentTimeUtc) {
        Clock clock = Clock.fixed(Instant.parse(currentTimeUtc), ZoneId.of("UTC"));
        DolphinFinancialDataProviderProperties dolphinProps = DolphinFinancialDataProviderProperties.builder()
                .refundPenalties(REFUND_PENALTIES)
                .build();
        var promoCodeCalculator = new FullMoneySplitCalculator();
        return new DolphinFinancialDataProvider(dolphinProps, ClockService.create(clock), promoCodeCalculator,
                financialEventRepository, new DolphinProperties());
    }

    private FinancialEvent getOnlyEvent(List<FinancialEvent> events) {
        Preconditions.checkArgument(events.size() == 1, "Exactly one event is expected but got %s", events);
        return events.get(0);
    }

    @Setter
    @Accessors(chain = true, fluent = true)
    private static class Order {
        private double price = 100.0;

        private Double discount;

        /**
         * Instant
         */
        private String checkInTs = Instant.ofEpochMilli(0).toString();

        /**
         * LocalDate
         */
        private String checkOut = LocalDate.now().plusYears(1000).toString();

        private PaymentSchedule schedule = null;


        private int nights = 5;

        private int refundRulesVersion = 0;

        private RefundInfo refundInfo = null;

        private DolphinOrderItem get() {
            DolphinOrderItem orderItem = new DolphinOrderItem();
            HotelOrder order = new HotelOrder();
            order.setPrettyId("YA-PRETTY-ID");
            order.setCurrency(ProtoCurrencyUnit.RUB);
            order.setPaymentSchedule(schedule);
            orderItem.setOrder(order);

            DolphinHotelItinerary itinerary = new DolphinHotelItinerary();
            Money totalPrice = Money.of(price, "RUB");
            itinerary.setCheckInMoment(checkInTs != null ? Instant.parse(checkInTs) : null);
            itinerary.setFiscalPrice(totalPrice);
            itinerary.setActualPrice(totalPrice);
            itinerary.setNights(nights);
            itinerary.setRefundInfo(refundInfo);
            itinerary.setOrderDetails(OrderDetails.builder()
                    .checkoutDate(checkOut != null ? LocalDate.parse(checkOut) : null)
                    .build());
            if (refundRulesVersion == 1) {
                var newStrategy = new AverageNightStayFeeRefundParams(nights, 3);
                itinerary.setRefundParams(newStrategy);
            }
            orderItem.setItinerary(itinerary);
            orderItem.setBillingPartnerAgreement(TEST_AGREEMENT);

            FiscalItem fiscalItem = FiscalItem.builder()
                    .moneyAmount(totalPrice)
                    .build();
            orderItem.addFiscalItem(fiscalItem);
            if (discount != null) {
                FiscalItemDiscount itemDiscount = new FiscalItemDiscount();
                itemDiscount.setDiscount(Money.of(discount, "RUB"));
                fiscalItem.setFiscalItemDiscounts(List.of(itemDiscount));
            }

            return orderItem;
        }
    }
}
