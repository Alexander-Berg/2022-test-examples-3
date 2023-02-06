package ru.yandex.travel.orders.services.finances.providers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.services.finances.OverallServiceBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.fullSplit;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.getOnlyEvent;
import static ru.yandex.travel.orders.services.finances.providers.ProviderTestHelper.sourcesSplit;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class ServiceBalanceTest {
    public static final BigDecimal REFUND_RATE = BigDecimal.valueOf(0.14);
    private long refundEventId;
    private final FullMoneySplitCalculator calculator = new FullMoneySplitCalculator();

    @Before
    public void resetIdSequence() {
        refundEventId = 1000L;
    }

    @Test
    public void testNoEventsBalance() {
        ServiceBalance balance = new ServiceBalance(Collections.emptyList(), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getBestRefundableEvent()).isNull();
    }

    @Test
    public void testSingleEventBalance() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .promoCodePartnerAmount(rub(8600))
                .feeAmount(rub(1000))
                .promoCodeFeeAmount(rub(400))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(event), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(10000));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(1400));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(8600));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(1000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(9000));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1()).isEqualTo(event);
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(10000));
    }

    @Test
    public void testPaymentPlusRefundWithPenaltyBalance() {
        var payment = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .promoCodePartnerAmount(rub(8600))
                .feeAmount(rub(1000))
                .promoCodeFeeAmount(rub(400))
                .build();
        var refund = FinancialEvent.builder()
                .id(1L)
                .type(FinancialEventType.REFUND)
                .originalEvent(payment)
                .promoCodePartnerRefundAmount(rub(8600))
                .promoCodeFeeRefundAmount(rub(400))
                .partnerRefundAmount(rub(-430))
                .feeRefundAmount(rub(930))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(payment, refund), ProtoCurrencyUnit.RUB);
        FullMoneySplit fullSplit = balance.getOverallBalance().getFullSplit();
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(500));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(0));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(500));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(430));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(70));
        assertThat(fullSplit.getUserMoney().getPartner()).isEqualByComparingTo(rub(430));
        assertThat(fullSplit.getUserMoney().getFee()).isEqualByComparingTo(rub(70));
        assertThat(fullSplit.getPlusMoney().getPartner()).isEqualByComparingTo(rub(0));
        assertThat(fullSplit.getPlusMoney().getFee()).isEqualByComparingTo(rub(0));
        assertThat(fullSplit.getPromoMoney().getPartner()).isEqualByComparingTo(rub(0));
        assertThat(fullSplit.getPromoMoney().getFee()).isEqualByComparingTo(rub(0));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1()).isEqualTo(payment);
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(500));
    }

    @Test
    public void testPaymentPlusRefundWithPenaltyPlusFullRefundBalance() {
        var payment = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .promoCodePartnerAmount(rub(8600))
                .feeAmount(rub(1000))
                .promoCodeFeeAmount(rub(400))
                .build();
        var refund1 = FinancialEvent.builder()
                .id(1L)
                .type(FinancialEventType.REFUND)
                .originalEvent(payment)
                .promoCodePartnerRefundAmount(rub(8600))
                .promoCodeFeeRefundAmount(rub(400))
                .partnerRefundAmount(rub(-430))
                .feeRefundAmount(rub(930))
                .build();
        var refund2 = FinancialEvent.builder()
                .id(2L)
                .type(FinancialEventType.REFUND)
                .originalEvent(payment)
                .promoCodePartnerRefundAmount(rub(0))
                .promoCodeFeeRefundAmount(rub(0))
                .partnerRefundAmount(rub(430))
                .feeRefundAmount(rub(70))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(payment, refund1, refund2), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(Money.zero(ProtoCurrencyUnit.RUB));
        assertThat(balance.getBestRefundableEvent()).isNull();
    }

    @Test
    public void testTwoPaymentsWithPromoMoney() {
        ServiceBalance balance = new ServiceBalance(List.of(
                FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, 100 rub promo code
                        .id(0L)
                        .type(FinancialEventType.PAYMENT)
                        .promoCodePartnerAmount(rub(100))
                        .promoCodeFeeAmount(rub(0))
                        .partnerAmount(rub(760))
                        .feeAmount(rub(140))
                        .promoCodeFeeAmount(rub(0))
                        .build(),
                FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, 200 rub promo code
                        .id(1L)
                        .type(FinancialEventType.PAYMENT)
                        .promoCodePartnerAmount(rub(200))
                        .promoCodeFeeAmount(rub(0))
                        .partnerAmount(rub(660))
                        .feeAmount(rub(140))
                        .promoCodeFeeAmount(rub(0))
                        .build()
        ), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(2000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(300));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(1700));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(1720));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(280));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1().getId()).isEqualTo(1L); // event with higher promo balance
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(1000));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPromo()).isEqualByComparingTo(rub(200));
        assertThat(balance.getBestRefundableEvent().get2().getTotalUser()).isEqualByComparingTo(rub(800));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPartner()).isEqualByComparingTo(rub(860));
        assertThat(balance.getBestRefundableEvent().get2().getTotalFee()).isEqualByComparingTo(rub(140));
    }

    @Test
    public void testTwoPaymentsOnlyOneWithPromoMoney() {
        ServiceBalance balance = new ServiceBalance(List.of(
                FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, 100 rub promo code
                        .id(0L)
                        .type(FinancialEventType.PAYMENT)
                        .promoCodePartnerAmount(rub(100))
                        .promoCodeFeeAmount(rub(0))
                        .partnerAmount(rub(760))
                        .feeAmount(rub(140))
                        .promoCodeFeeAmount(rub(0))
                        .build(),
                FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, no promo code
                        .id(1L)
                        .type(FinancialEventType.PAYMENT)
                        .promoCodePartnerAmount(rub(0))
                        .promoCodeFeeAmount(rub(0))
                        .partnerAmount(rub(860))
                        .feeAmount(rub(140))
                        .promoCodeFeeAmount(rub(0))
                        .build()
        ), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(2000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(100));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(1900));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(1720));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(280));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1().getId()).isEqualTo(0L); // event with promo balance
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(1000));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPromo()).isEqualByComparingTo(rub(100));
        assertThat(balance.getBestRefundableEvent().get2().getTotalUser()).isEqualByComparingTo(rub(900));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPartner()).isEqualByComparingTo(rub(860));
        assertThat(balance.getBestRefundableEvent().get2().getTotalFee()).isEqualByComparingTo(rub(140));
    }

    @Test
    public void testTwoPaymentsOneWithPromoMoneyPartiallyRefunded() {
        FinancialEvent payment = FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, 100 rub promo code
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .promoCodePartnerAmount(rub(100))
                .promoCodeFeeAmount(rub(0))
                .partnerAmount(rub(760))
                .feeAmount(rub(140))
                .promoCodeFeeAmount(rub(
                        0))
                .build();
        FinancialEvent extra = FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, no promo code
                .id(1L)
                .type(FinancialEventType.PAYMENT)
                .promoCodePartnerAmount(rub(0))
                .promoCodeFeeAmount(rub(0))
                .partnerAmount(rub(860))
                .feeAmount(rub(140))
                .promoCodeFeeAmount(rub(0))
                .build();
        FinancialEvent refund = FinancialEvent.builder() // refund of 50 rub of promo code (main refund)
                .id(2L)
                .originalEvent(payment)
                .type(FinancialEventType.REFUND)
                .promoCodePartnerRefundAmount(rub(50))
                .partnerRefundAmount(rub(0))
                .feeRefundAmount(rub(7))
                .build();
        FinancialEvent correction = FinancialEvent.builder() // refund of 50 rub of promo code (correction payment)
                .id(3L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(7))
                .feeAmount(rub(0))
                .build();

        ServiceBalance balance = new ServiceBalance(List.of(payment, extra, refund, correction), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(1950));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(50));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(1900));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(1677));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(273));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1().getId()).isEqualTo(0L); // event with promo balance
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(943));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPromo()).isEqualByComparingTo(rub(50));
        assertThat(balance.getBestRefundableEvent().get2().getTotalUser()).isEqualByComparingTo(rub(893));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPartner()).isEqualByComparingTo(rub(810));
        assertThat(balance.getBestRefundableEvent().get2().getTotalFee()).isEqualByComparingTo(rub(133));
    }

    @Test
    public void testTwoPaymentsOneWithPromoMoneyFullyRefunded() {
        FinancialEvent payment = FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, 100 rub promo code
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .promoCodePartnerAmount(rub(100))
                .promoCodeFeeAmount(rub(0))
                .partnerAmount(rub(760))
                .feeAmount(rub(140))
                .promoCodeFeeAmount(rub(
                        0))
                .build();
        FinancialEvent extra = FinancialEvent.builder()  // payment: order for 1000 rub, fee 14%, no promo code
                .id(1L)
                .type(FinancialEventType.PAYMENT)
                .promoCodePartnerAmount(rub(0))
                .promoCodeFeeAmount(rub(0))
                .partnerAmount(rub(860))
                .feeAmount(rub(140))
                .promoCodeFeeAmount(rub(0))
                .build();
        FinancialEvent refund = FinancialEvent.builder() // refund of 100 rub of promo code (main refund)
                .id(2L)
                .originalEvent(payment)
                .type(FinancialEventType.REFUND)
                .promoCodePartnerRefundAmount(rub(100))
                .partnerRefundAmount(rub(0))
                .feeRefundAmount(rub(14))
                .build();
        FinancialEvent correction = FinancialEvent.builder() // refund of 50 rub of promo code (correction payment)
                .id(3L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(14))
                .feeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(payment, extra, refund, correction), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(1900));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(0));
        assertThat(balance.getOverallBalance().getTotalUser()).isEqualByComparingTo(rub(1900));
        assertThat(balance.getOverallBalance().getTotalPartner()).isEqualByComparingTo(rub(1634));
        assertThat(balance.getOverallBalance().getTotalFee()).isEqualByComparingTo(rub(266));
        assertThat(balance.getBestRefundableEvent()).isNotNull();
        assertThat(balance.getBestRefundableEvent().get1().getId()).isEqualTo(1L); // event with maximum balance
        assertThat(balance.getBestRefundableEvent().get2().getTotal()).isEqualByComparingTo(rub(1000));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPromo()).isEqualByComparingTo(rub(0));
        assertThat(balance.getBestRefundableEvent().get2().getTotalUser()).isEqualByComparingTo(rub(1000));
        assertThat(balance.getBestRefundableEvent().get2().getTotalPartner()).isEqualByComparingTo(rub(860));
        assertThat(balance.getBestRefundableEvent().get2().getTotalFee()).isEqualByComparingTo(rub(140));
    }

    @Test
    public void generateFullRefundForSingleEventBalance() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents = balance.calculateRefunds(rub(0), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(1);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(0));
    }


    @Test
    public void generateNoRefundForSingleEventBalance() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents = balance.calculateRefunds(rub(10000), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(1);
        assertThat(refundEvents.get(0).getTotalAmount().isZero()).isTrue();
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(10000));
    }


    @Test
    public void generatePartialRefundForSingleEventBalance() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents = balance.calculateRefunds(rub(2000), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(1);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(2000));
    }


    @Test
    public void generatePartialRefundForSingleEventBalanceThanReturnAll() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents1 = balance.calculateRefunds(rub(2000), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents1).hasSize(1);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(2000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualTo(rub(0));
        var refundEvents2 = balance.calculateRefunds(rub(0), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents2).hasSize(1);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(0));
    }

    @Test
    public void generatePartialRefundForSingleEventBalanceWithCorrection() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .promoCodePartnerAmount(rub(8600))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents = balance.calculateRefunds(rub(2000), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(2);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(2000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualTo(rub(600));
    }

    @Test
    public void generatePartialRefundForSingleEventBalanceWithCorrectionThanReturnSomeMoreThanAll() {
        FinancialEvent event = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .promoCodePartnerAmount(rub(8600))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(event)), ProtoCurrencyUnit.RUB);
        var refundEvents1 = balance.calculateRefunds(rub(2000), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents1).hasSize(2);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(2000));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualTo(rub(600));
        var refundEvents2 = balance.calculateRefunds(rub(1400), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents2).hasSize(2);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(1400));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualTo(rub(0));
        var refundEvents3 = balance.calculateRefunds(rub(0), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents3).hasSize(3);
        assertThat(balance.getOverallBalance().getTotalRefundable()).isEqualTo(rub(0));
    }

    @Test
    public void generateFullRefundForMultiEventBalance() {
        FinancialEvent initialPayment = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        FinancialEvent extraPayment1 = FinancialEvent.builder()
                .id(1L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(86))
                .feeAmount(rub(14))
                .build();
        FinancialEvent extraPayment2 = FinancialEvent.builder()
                .id(2L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(172))
                .feeAmount(rub(28))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(initialPayment, extraPayment1,
                extraPayment2)), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(10300));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(100));
        var refundEvents = balance.calculateRefunds(rub(0), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(3);
        assertThat(refundEvents).extracting(e -> e.getOriginalEvent().getId()).containsExactlyInAnyOrder(0L, 1L, 2L);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(0));
    }

    @Test
    public void generateSmallRefundForMultiEventBalance() {
        FinancialEvent initialPayment = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        FinancialEvent extraPayment1 = FinancialEvent.builder()
                .id(1L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(86))
                .feeAmount(rub(14))
                .build();
        FinancialEvent extraPayment2 = FinancialEvent.builder()
                .id(2L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(172))
                .feeAmount(rub(28))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(initialPayment, extraPayment1,
                extraPayment2)), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(10300));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(100));
        var refundEvents = balance.calculateRefunds(rub(9800), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(1);
        assertThat(refundEvents.get(0).getOriginalEvent().getId()).isEqualTo(0L); // largest payment is returned first
        assertThat(refundEvents.get(0).getTotalAmount()).isEqualByComparingTo(rub(500));
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(9800));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(0));
    }

    @Test
    public void generateLargeRefundForMultiEventBalance() {
        FinancialEvent initialPayment = FinancialEvent.builder()
                .id(0L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(8500))
                .promoCodePartnerAmount(rub(100))
                .feeAmount(rub(1400))
                .promoCodeFeeAmount(rub(0))
                .build();
        FinancialEvent extraPayment1 = FinancialEvent.builder()
                .id(1L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(86))
                .feeAmount(rub(14))
                .build();
        FinancialEvent extraPayment2 = FinancialEvent.builder()
                .id(2L)
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(172))
                .feeAmount(rub(28))
                .build();
        ServiceBalance balance = new ServiceBalance(new ArrayList<>(List.of(initialPayment, extraPayment1,
                extraPayment2)), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(10300));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(100));
        var refundEvents = balance.calculateRefunds(rub(50), REFUND_RATE, this::createRefundEvent, calculator);
        assertThat(refundEvents).hasSize(3);
        assertThat(refundEvents).extracting(e -> e.getOriginalEvent().getId()).containsExactlyInAnyOrder(0L, 1L, 2L);
        assertThat(balance.getOverallBalance().getTotal()).isEqualByComparingTo(rub(50));
        assertThat(balance.getOverallBalance().getTotalPromo()).isEqualByComparingTo(rub(0));
        // first payment is processed first and thus is fully refunded:
        assertThat(balance.getPaymentBalances().get(initialPayment).getTotal()).isEqualByComparingTo(rub(0));
        // largest extra payment is processed second and thus is fully refunded:
        assertThat(balance.getPaymentBalances().get(extraPayment2).getTotal()).isEqualByComparingTo(rub(0));
        // smallest extra payment is processed last and thus holds the remainder penalty:
        assertThat(balance.getPaymentBalances().get(extraPayment1).getTotal()).isEqualByComparingTo(rub(50));
    }

    @Test
    public void increaseBalanceTo() {
        ServiceBalance balance = new ServiceBalance(List.of(), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualTo(rub(0));

        List<FinancialEvent> events = balance.increaseBalanceTo(
                sourcesSplit(1_000, 7_000, 2_000),
                BigDecimal.valueOf(0.2),
                this::createPaymentEvent,
                calculator,
                false);
        assertThat(events).hasSize(1);
        FinancialEvent fe = events.get(0);
        assertThat(fe.getTotalAmount()).isEqualTo(rub(10_000));
        assertThat(fe.getPartnerAmount()).isEqualTo(rub(0));
        assertThat(fe.getFeeAmount()).isEqualTo(rub(1_000));
        assertThat(fe.getPlusPartnerAmount()).isEqualTo(rub(6_000));
        assertThat(fe.getPlusFeeAmount()).isEqualTo(rub(1_000));
        assertThat(fe.getPromoCodePartnerAmount()).isEqualTo(rub(2_000));

        assertThat(balance.getOverallBalance().getTotal()).isEqualTo(rub(10_000));
    }

    @Test
    public void decreaseBalanceTo() {
        FinancialEvent initialPayment = FinancialEvent.builder()
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(0))
                .feeAmount(rub(1_000))
                .plusPartnerAmount(rub(6_000))
                .plusFeeAmount(rub(1_000))
                .promoCodePartnerAmount(rub(2_000))
                .build();
        ServiceBalance balance = new ServiceBalance(List.of(initialPayment), ProtoCurrencyUnit.RUB);
        assertThat(balance.getOverallBalance().getTotal()).isEqualTo(rub(10_000));

        List<FinancialEvent> events = balance.decreaseBalanceTo(
                sourcesSplit(1_000, 4_000, 0),
                BigDecimal.valueOf(0.1),
                this::createRefundEvent,
                calculator,
                false
        );
        assertThat(events).hasSize(2);

        FinancialEvent refundEvent = getOnlyEvent(events, FinancialEventType.REFUND);
        assertThat(refundEvent.getTotalAmount()).isEqualTo(rub(5_500));
        assertThat(refundEvent.getPartnerRefundAmount()).isEqualTo(rub(0));
        assertThat(refundEvent.getFeeRefundAmount()).isEqualTo(rub(500));
        assertThat(refundEvent.getPlusPartnerRefundAmount()).isEqualTo(rub(2_000));
        assertThat(refundEvent.getPlusFeeRefundAmount()).isEqualTo(rub(1_000));
        assertThat(refundEvent.getPromoCodePartnerRefundAmount()).isEqualTo(rub(2_000));

        FinancialEvent correctionEvent = getOnlyEvent(events, FinancialEventType.PAYMENT);
        assertThat(correctionEvent.getTotalAmount()).isEqualTo(rub(500));
        assertThat(correctionEvent.getPartnerAmount()).isEqualTo(rub(500));

        assertThat(balance.getOverallBalance().getTotal()).isEqualTo(rub(5_000));
    }

    @Test
    public void allFieldsSupport() {
        FinancialEvent initialPayment = FinancialEvent.builder()
                .type(FinancialEventType.PAYMENT)
                .partnerAmount(rub(1))
                .feeAmount(rub(2))
                .plusPartnerAmount(rub(3))
                .plusFeeAmount(rub(4))
                .promoCodePartnerAmount(rub(5))
                .promoCodeFeeAmount(rub(6))
                // not supported
                //.partnerFeeAmount(rub(7))
                .build();
        ServiceBalance balance1 = new ServiceBalance(List.of(initialPayment), ProtoCurrencyUnit.RUB);
        assertThat(balance1.getOverallBalance().getTotal()).isEqualTo(rub(21));

        ServiceBalance.PaymentBalance overall1 = balance1.getOverallBalance();
        assertThat(overall1.getFullSplit()).isEqualTo(fullSplit(1, 2, 3, 4, 5, 6));
        //assertThat(overall1.getPartnerFeeAmount()).isEqualTo(rub(7));
        assertThat(overall1.convertToOverall()).isEqualTo(OverallServiceBalance.builder()
                .userPartner(rub(1))
                .userFee(rub(2))
                .plusPartner(rub(3))
                .plusFee(rub(4))
                .promoPartner(rub(5))
                .promoFee(rub(6))
                //.techFee(rub(7))
                .techFee(rub(0))
                .build());
        assertThat(overall1.getTotalUser()).isEqualTo(rub(3));
        assertThat(overall1.getTotalPlus()).isEqualTo(rub(7));
        assertThat(overall1.getTotalPromo()).isEqualTo(rub(11));
        assertThat(overall1.getTotalPartner()).isEqualTo(rub(9));
        assertThat(overall1.getTotalFee()).isEqualTo(rub(12));

        FinancialEvent refundEvent = FinancialEvent.builder()
                .type(FinancialEventType.REFUND)
                .originalEvent(initialPayment)
                .partnerRefundAmount(rub(2))
                .feeRefundAmount(rub(4))
                .plusPartnerRefundAmount(rub(6))
                .plusFeeRefundAmount(rub(8))
                .promoCodePartnerRefundAmount(rub(10))
                .promoCodeFeeRefundAmount(rub(12))
                //.partnerFeeRefundAmount(rub(14))
                .build();
        ServiceBalance balance2 = new ServiceBalance(List.of(initialPayment, refundEvent), ProtoCurrencyUnit.RUB);
        assertThat(balance2.getOverallBalance().getTotal()).isEqualTo(rub(-21));

        ServiceBalance.PaymentBalance overall2 = balance2.getOverallBalance();
        assertThat(overall2.getFullSplit()).isEqualTo(fullSplit(-1, -2, -3, -4, -5, -6));
        //assertThat(overall2.getPartnerFeeAmount()).isEqualTo(rub(-7));
    }

    private FinancialEvent createPaymentEvent() {
        return FinancialEvent.builder()
                .type(FinancialEventType.PAYMENT)
                .build();
    }

    private FinancialEvent createRefundEvent() {
        return FinancialEvent.builder()
                .id(refundEventId++)
                .type(FinancialEventType.REFUND)
                .build();
    }
}
