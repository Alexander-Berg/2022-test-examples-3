package ru.yandex.travel.orders.services.finances.billing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.javamoney.moneta.Money;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.hotels.common.orders.DolphinHotelItinerary;
import ru.yandex.travel.orders.entities.DolphinOrderItem;
import ru.yandex.travel.orders.entities.GenericOrder;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.Invoice;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.TrainOrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.finances.BillingTransaction;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType;
import ru.yandex.travel.orders.entities.finances.BillingTransactionType;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme;
import ru.yandex.travel.orders.entities.finances.FinancialEventType;
import ru.yandex.travel.orders.entities.finances.ProcessingTasksInfo;
import ru.yandex.travel.orders.repository.BillingTransactionRepository;
import ru.yandex.travel.orders.repository.FinancialEventRepository;
import ru.yandex.travel.train.model.TrainReservation;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.commons.streams.CustomCollectors.exactlyOne;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType.POSTPAY;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType.PROMO_CODE;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType.SBERBANK;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType.YANDEX;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentSystemType.YANDEX_MONEY;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.COST;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.FEE;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.REWARD;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.YANDEX_ACCOUNT_COST_WITHDRAW;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.YANDEX_ACCOUNT_REWARD_WITHDRAW;
import static ru.yandex.travel.orders.entities.finances.BillingTransactionPaymentType.YANDEX_ACCOUNT_TOPUP;
import static ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme.HOTELS;
import static ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme.HOTELS_POSTPAY;
import static ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme.SUBURBAN;
import static ru.yandex.travel.orders.entities.finances.FinancialEventPaymentScheme.TRAINS;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.MANUAL_REFUND;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.PAYMENT;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.REFUND;
import static ru.yandex.travel.orders.entities.finances.FinancialEventType.YANDEX_ACCOUNT_TOPUP_PAYMENT;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class BillingTransactionGeneratorTest {
    private final FinancialEventRepository financialEventRepository = Mockito.mock(FinancialEventRepository.class);
    private final BillingTransactionRepository billingTransactionRepository =
            Mockito.mock(BillingTransactionRepository.class);

    @Test
    public void createBillingTransactions() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(PAYMENT)
                .originalEvent(event(1, ts("2019-01-01"))) // doesn't make any sense but shouldn't break anything
                .order(testHotelOrder())
                .partnerAmount(Money.of(8700, "RUB"))
                .feeAmount(Money.of(1300, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(2);

        BillingTransaction costTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(costTx).isNotNull();
        assertThat(costTx.getId()).isNull();
        assertThat(costTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(costTx.getOriginalTransaction()).isNull();
        assertThat(costTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(costTx.getPaymentType()).isEqualTo(COST);
        assertThat(costTx.getValue()).isEqualTo(Money.of(8700, "RUB"));
        assertThat(costTx.getPayoutAt()).isEqualTo("2019-11-24T21:45:38.790Z");
        assertThat(costTx.getAccountingActAt()).isEqualTo("2019-12-20T15:00:12.340Z");

        BillingTransaction rewardTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(rewardTx.getOriginalTransaction()).isNull();
        assertThat(rewardTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(rewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(1300, "RUB"));
        assertThat(rewardTx.getPayoutAt()).isEqualTo("2019-11-24T21:45:38.790Z");
        assertThat(rewardTx.getAccountingActAt()).isEqualTo("2019-12-20T15:00:12.340Z");
    }

    @Test
    public void createBillingTransactionsForRefund() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourcePaymentEvent = event(1, ts("2019-01-01"));
        FinancialEvent sourceRefundEvent = hotelsEventBuilder()
                .type(REFUND)
                .originalEvent(sourcePaymentEvent)
                .order(testHotelOrder())
                .partnerRefundAmount(Money.of(1740, "RUB"))
                .feeRefundAmount(Money.of(260, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, COST, YANDEX_MONEY))
                .thenReturn(transaction(101, PAYMENT, COST, YANDEX_MONEY, 8600));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, YANDEX_MONEY))
                .thenReturn(transaction(102, PAYMENT, REWARD, YANDEX_MONEY, 1400));

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceRefundEvent);
        assertThat(transactions).hasSize(2);

        BillingTransaction costTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(costTx).isNotNull();
        assertThat(costTx.getId()).isNull();
        assertThat(costTx.getSourceFinancialEvent()).isEqualTo(sourceRefundEvent);
        assertThat(costTx.getOriginalTransaction()).isNotNull().matches(tx -> tx.getId() == 101);
        assertThat(costTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(costTx.getPaymentType()).isEqualTo(COST);
        assertThat(costTx.getValue()).isEqualTo(Money.of(1740, "RUB"));
        assertThat(costTx.getPayoutAt()).isEqualTo("2019-11-24T21:45:38.790Z");

        BillingTransaction rewardTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getSourceFinancialEvent()).isEqualTo(sourceRefundEvent);
        assertThat(rewardTx.getOriginalTransaction()).isNotNull().matches(tx -> tx.getId() == 102);
        assertThat(rewardTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(rewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(260, "RUB"));
        assertThat(rewardTx.getPayoutAt()).isEqualTo("2019-11-24T21:45:38.790Z");
    }

    @Test
    public void createBillingTransactions_promoCodePayment() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(PAYMENT)
                .originalEvent(event(1, ts("2019-01-01"))) // doesn't make any sense but shouldn't break anything
                .order(testHotelOrder())
                .partnerAmount(Money.of(0, "RUB"))
                .feeAmount(Money.of(1000, "RUB"))
                .promoCodePartnerAmount(Money.of(8700, "RUB"))
                .promoCodeFeeAmount(Money.of(300, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(3);

        BillingTransaction rewardTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == REWARD && tx.getPaymentSystemType() == YANDEX_MONEY)
                .findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(1000, "RUB"));

        BillingTransaction promoCostTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == COST && tx.getPaymentSystemType() == PROMO_CODE)
                .findFirst().orElse(null);
        assertThat(promoCostTx).isNotNull();
        assertThat(promoCostTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(promoCostTx.getValue()).isEqualTo(Money.of(8700, "RUB"));

        BillingTransaction promoRewardTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == REWARD && tx.getPaymentSystemType() == PROMO_CODE)
                .findFirst().orElse(null);
        assertThat(promoRewardTx).isNotNull();
        assertThat(promoRewardTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(promoRewardTx.getValue()).isEqualTo(Money.of(300, "RUB"));
    }

    @Test
    public void createBillingTransactions_promoCodeRefund() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourcePaymentEvent = event(1, ts("2019-01-01"));
        FinancialEvent sourceRefundEvent = hotelsEventBuilder()
                .type(REFUND)
                .originalEvent(sourcePaymentEvent)
                .order(testHotelOrder())
                .partnerRefundAmount(Money.of(0, "RUB"))
                .feeRefundAmount(Money.of(200, "RUB"))
                .promoCodePartnerRefundAmount(Money.of(1740, "RUB"))
                .promoCodeFeeRefundAmount(Money.of(60, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, COST, YANDEX_MONEY))
                .thenReturn(transaction(101, PAYMENT, COST, YANDEX_MONEY, 0));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, YANDEX_MONEY))
                .thenReturn(transaction(102, PAYMENT, REWARD, YANDEX_MONEY, 400));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, COST, PROMO_CODE))
                .thenReturn(transaction(103, PAYMENT, COST, PROMO_CODE, 1740));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, PROMO_CODE))
                .thenReturn(transaction(104, PAYMENT, REWARD, PROMO_CODE, 60));

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceRefundEvent);
        assertThat(transactions).hasSize(3);

        BillingTransaction rewardTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == REWARD && tx.getPaymentSystemType() == YANDEX_MONEY)
                .findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(200, "RUB"));

        BillingTransaction promoCostTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == COST && tx.getPaymentSystemType() == PROMO_CODE)
                .findFirst().orElse(null);
        assertThat(promoCostTx).isNotNull();
        assertThat(promoCostTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(promoCostTx.getValue()).isEqualTo(Money.of(1740, "RUB"));
        assertThat(promoCostTx.getOriginalTransaction()).isNotNull()
                .satisfies(tx -> assertThat(tx.getId()).isEqualTo(103));

        BillingTransaction promoRewardTx = transactions.stream()
                .filter(tx -> tx.getPaymentType() == REWARD && tx.getPaymentSystemType() == PROMO_CODE)
                .findFirst().orElse(null);
        assertThat(promoRewardTx).isNotNull();
        assertThat(promoRewardTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(promoRewardTx.getValue()).isEqualTo(Money.of(60, "RUB"));
        assertThat(promoRewardTx.getOriginalTransaction()).isNotNull()
                .satisfies(tx -> assertThat(tx.getId()).isEqualTo(104));
    }

    @Test
    public void createBillingTransactionsIllegalInput() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourcePaymentEvent = event(1, ts("2019-01-01"));
        FinancialEvent sourceRefundEvent = hotelsEventBuilder()
                .type(REFUND)
                .order(testHotelOrder())
                .originalEvent(sourcePaymentEvent)
                .partnerRefundAmount(Money.of(1740, "RUB"))
                .feeRefundAmount(Money.of(260, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, COST, YANDEX_MONEY))
                .thenReturn(transaction(101, PAYMENT, COST, YANDEX_MONEY, 8600));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, YANDEX_MONEY))
                .thenReturn(transaction(102, PAYMENT, REWARD, YANDEX_MONEY, 1400));
        BillingTransactionGenerator task = fixedClockGenerator(now);

        // valid data
        assertThat(task.createBillingTransactions(sourceRefundEvent)).isNotEmpty();

        // invalid data
        // no type
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent.toBuilder().type(null).build()))
                .isExactlyInstanceOf(NullPointerException.class);
        // illegal money value combinations
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent.toBuilder().feeAmount(Money.of(100,
                "RUB")).build()))
                .hasMessageContaining("Illegal cost breakdown");
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent.toBuilder().partnerRefundAmount(null).build()))
                .hasMessageContaining("Partner refund amount shouldn't be empty for refund events");
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent.toBuilder().feeRefundAmount(null).build()))
                .hasMessageContaining("Fee refund amount shouldn't be empty for refund events");
        // no original event or transaction for refunds
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent.toBuilder().originalEvent(null).build()))
                .hasMessageContaining("Refund financial event must have an associated payment event");
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(any(), any(), any()))
                .thenReturn(null);
        assertThatThrownBy(() -> task.createBillingTransactions(sourceRefundEvent))
                .hasMessageContaining("No billing transaction for a previous COST payment");
    }

    @Test
    public void createTransactionStructure() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(REFUND)
                .originalEvent(event(2, ts("2019-01-01"))) // doesn't make any sense but shouldn't break anything
                .order(testHotelOrder())
                .orderPrettyId("YA-1-2-3")
                .billingClientId(783642934L)
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .build();
        BillingTransaction prevTx = transaction(101, PAYMENT, COST, YANDEX_MONEY, 8600);
        BillingTransaction newTx = fixedClockGenerator(now).createTransaction(sourceEvent,
                BillingTransactionType.REFUND, COST, YANDEX_MONEY,
                Money.of(123, "RUB"), "t-p-id", prevTx);

        assertThat(newTx.getId()).isNull();
        assertThat(newTx.getServiceId()).isEqualTo(641);
        assertThat(newTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(newTx.getOriginalTransaction()).isEqualTo(prevTx);
        assertThat(newTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(newTx.getPaymentType()).isEqualTo(COST);
        assertThat(newTx.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(newTx.getPartnerId()).isEqualTo(783642934L);
        assertThat(newTx.getClientId()).isEqualTo(0);
        assertThat(newTx.getValue()).isEqualTo(Money.of(123, "RUB"));
        assertThat(newTx.getTrustPaymentId()).isEqualTo("t-p-id");
        assertThat(newTx.getServiceOrderId()).isEqualTo("YA-1-2-3");
        assertThat(newTx.getCreatedAt()).isEqualTo("2019-11-20T00:00:00Z");
        assertThat(newTx.getPayoutAt()).isEqualTo("2019-11-24T21:45:38.790Z");
        assertThat(newTx.getAccountingActAt()).isEqualTo("2019-12-20T15:00:12.340Z");
        assertThat(newTx.getVersion()).isNull();
        assertThat(newTx.isExportedToYt()).isFalse();
        assertThat(newTx.getExportedToYtAt()).isNull();
        assertThat(newTx.isActCommitted()).isFalse();
        assertThat(newTx.getActCommittedAt()).isNull();
    }

    @Test
    public void createTransactionBadInputRefund() {
        FinancialEvent fe = hotelsEventBuilder()
                .type(MANUAL_REFUND)
                .orderPrettyId("YA-1-2-3")
                .billingClientId(783642934L)
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .build();
        Instant now = ts("2019-11-23");
        BillingTransaction prevTx = transaction(100, PAYMENT, REWARD, YANDEX_MONEY, 1000);
        Money total = Money.of(123, "RUB");
        BillingTransactionType txType = BillingTransactionType.REFUND;
        BillingTransactionType txTypePayment = BillingTransactionType.PAYMENT;
        String trustId = "t-p-id";
        BillingTransactionGenerator task = fixedClockGenerator(now);

        // make sure the full input is valid
        assertThat(task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId, prevTx)).isNotNull();

        // missing data should fail the processing
        //noinspection ConstantConditions
        assertThatThrownBy(() -> task.createTransaction(null, txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No financialEvent");
        assertThatThrownBy(() -> task.createTransaction(fe, null, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No transactionType");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, null, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No paymentType");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, null, total, trustId, prevTx))
                .hasMessageContaining("No paymentSystemType");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, null, trustId, prevTx))
                .hasMessageContaining("No value");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY,
                Money.of(-100, "RUB"), trustId, prevTx))
                .hasMessageContaining("value can't be negative");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, null, prevTx))
                .hasMessageContaining("No trustPaymentId");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId, null))
                .hasMessageContaining("No originalTransaction");
        assertThatThrownBy(() -> task.createTransaction(fe.toBuilder().billingClientId(null).build(),
                txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No billingClientId");
        assertThatThrownBy(() -> task.createTransaction(fe.toBuilder().orderPrettyId(null).build(),
                txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No orderPrettyId");
        assertThatThrownBy(() -> task.createTransaction(fe.toBuilder().payoutAt(null).build(),
                txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No payoutAt");
        assertThatThrownBy(() -> task.createTransaction(fe.toBuilder().accountingActAt(null).build(),
                txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No accountingActAt");

        assertThatThrownBy(() -> task.createTransaction(fe, txTypePayment, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("Event transactionType mismatch");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId,
                prevTx.toBuilder().transactionType(BillingTransactionType.REFUND).build()))
                .hasMessageContaining("Original event transactionType mismatch");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId,
                prevTx.toBuilder().paymentType(COST).build()))
                .hasMessageContaining("Original event paymentType mismatch");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId,
                prevTx.toBuilder().paymentSystemType(PROMO_CODE).build()))
                .hasMessageContaining("Original event paymentSystemType mismatch");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId,
                prevTx.toBuilder().value(Money.of(100, "RUB")).build()))
                .hasMessageContaining("Refund value RUB 123 can't exceed original tx value RUB 100");
    }

    @Test
    public void createTransactionBadInputPayment() {
        FinancialEvent fe = hotelsEventBuilder()
                .type(PAYMENT)
                .orderPrettyId("YA-1-2-3")
                .billingClientId(783642934L)
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .build();
        Instant now = ts("2019-11-23");
        BillingTransaction prevTx = BillingTransaction.builder().build();
        Money total = Money.of(123, "RUB");
        BillingTransactionType txType = BillingTransactionType.PAYMENT;
        BillingTransactionType txTypeRefund = BillingTransactionType.REFUND;
        String trustId = "t-p-id";
        BillingTransactionGenerator task = fixedClockGenerator(now);

        // make sure the full input is valid
        assertThat(task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId, null)).isNotNull();

        assertThatThrownBy(() -> task.createTransaction(fe, txTypeRefund, REWARD, YANDEX_MONEY, total, trustId, null))
                .hasMessageContaining("transactionType mismatch");
        assertThatThrownBy(() -> task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId, prevTx))
                .hasMessageContaining("No originalTransaction expected for payments");
    }

    @Test
    public void createTransactionPastDatesCorrection() {
        FinancialEvent fe = hotelsEventBuilder()
                .type(PAYMENT)
                .orderPrettyId("YA-1-2-3")
                .billingClientId(783642934L)
                .payoutAt(ts("2019-09-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-10-20T15:00:12.34Z"))
                .build();
        Instant now = ts("2019-11-23");
        Money total = Money.of(123, "RUB");
        BillingTransactionType txType = BillingTransactionType.PAYMENT;
        BillingTransactionType txTypeRefund = BillingTransactionType.REFUND;
        String trustId = "t-p-id";
        BillingTransactionGenerator task = fixedClockGenerator(now);

        BillingTransaction tx = task.createTransaction(fe, txType, REWARD, YANDEX_MONEY, total, trustId, null);
        assertThat(tx.getPayoutAt()).isEqualTo(now);
        assertThat(tx.getAccountingActAt()).isEqualTo(now);
    }

    @Test
    public void precessPendingEventGroupForEarlyRefund() {
        Order order = testHotelOrder();
        FinancialEvent e1 = hotelsEventBuilder()
                .id(1L)
                .type(PAYMENT)
                .order(order)
                .orderItem(order.getOrderItems().get(0))
                .partnerAmount(Money.of(8700, "RUB"))
                .feeAmount(Money.of(1300, "RUB"))
                .payoutAt(ts("2020-01-31T21:00:00.00Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-1")
                .build();
        FinancialEvent e2 = hotelsEventBuilder()
                .id(2L)
                .type(REFUND)
                .order(order)
                .orderItem(order.getOrderItems().get(0))
                .originalEvent(e1)
                .partnerRefundAmount(Money.of(1740, "RUB"))
                .feeRefundAmount(Money.of(260, "RUB"))
                .payoutAt(ts("2019-11-30T21:00:00.00Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-1")
                .build();

        Instant now = ts("2019-11-29T21:00:00.00Z");
        BillingTransactionGenerator task = fixedClockGenerator(now);
        when(financialEventRepository.getOne(e1.getId())).thenReturn(e1);
        when(financialEventRepository.getOne(e2.getId())).thenReturn(e2);
        when(financialEventRepository.findFirstByOrderItemAndTypeOrderByIdAsc(e2.getOrderItem(), PAYMENT)).thenReturn(e1);

        assertThat(e1.isProcessed()).isFalse();
        assertThat(e2.isProcessed()).isFalse();

        List<BillingTransaction> generatedTx = new ArrayList<>();
        AtomicLong idSeq = new AtomicLong(0);
        when(billingTransactionRepository.save(any())).then(call -> {
            BillingTransaction tx = call.getArgument(0);
            tx.setId(idSeq.incrementAndGet());
            generatedTx.add(tx);
            return null;
        });
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(any(), any(), any()))
                .then(call -> generatedTx.stream().filter(tx ->
                        tx.getSourceFinancialEvent() == call.getArgument(0)
                                && tx.getPaymentType() == call.getArgument(1)
                ).findFirst().orElse(null));
        task.processPendingEvent(e1.getId());
        assertThat(e1.isProcessed()).isTrue();
        assertThat(e2.isProcessed()).isFalse();
        task.processPendingEvent(e2.getId());
        assertThat(e1.isProcessed()).isTrue();
        assertThat(e2.isProcessed()).isTrue();

        assertThat(generatedTx).hasSize(4);
        assertThat(generatedTx.get(0)).satisfies(tx -> {
            assertThat(tx.getSourceFinancialEvent()).isEqualTo(e1);
            assertThat(tx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
            assertThat(tx.getOriginalTransaction()).isNull();
            assertThat(tx.getPayoutAt()).isEqualTo("2020-01-31T21:00:00Z");
        });
        assertThat(generatedTx.get(1)).satisfies(tx -> {
            assertThat(tx.getSourceFinancialEvent()).isEqualTo(e1);
            assertThat(tx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
            assertThat(tx.getOriginalTransaction()).isNull();
            assertThat(tx.getPayoutAt()).isEqualTo("2020-01-31T21:00:00Z");
        });
        assertThat(generatedTx.get(2)).satisfies(tx -> {
            assertThat(tx.getSourceFinancialEvent()).isEqualTo(e2);
            assertThat(tx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
            assertThat(tx.getOriginalTransaction()).isEqualTo(generatedTx.get(0));
            assertThat(tx.getPayoutAt()).isEqualTo("2019-11-30T21:00:00Z");
        });
        assertThat(generatedTx.get(3)).satisfies(tx -> {
            assertThat(tx.getSourceFinancialEvent()).isEqualTo(e2);
            assertThat(tx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
            assertThat(tx.getOriginalTransaction()).isEqualTo(generatedTx.get(1));
            assertThat(tx.getPayoutAt()).isEqualTo("2019-11-30T21:00:00Z");
        });
    }

    @Test
    public void getCurrentProcessingDelay() {
        BillingTransactionGenerator generator = fixedClockGenerator(Instant.parse("2019-12-27T12:15:00Z"));
        when(financialEventRepository.findOldestUnprocessedTimestamp()).thenReturn(new ProcessingTasksInfo(null, 0L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.ZERO);

        when(financialEventRepository.findOldestUnprocessedTimestamp()).thenReturn(new ProcessingTasksInfo(null, 1L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.ofHours(24));

        when(financialEventRepository.findOldestUnprocessedTimestamp())
                .thenReturn(new ProcessingTasksInfo(Instant.parse("2019-12-27T11:53:34Z"), 1L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.parse("PT21M26S"));
    }

    @Test
    public void testTransactionWithPartnerFee() {
        Instant now = ts("2021-01-20T10:00:00.00Z");
        FinancialEvent sourceEvent = trainsEventBuilder()
                .type(PAYMENT)
                .order(testTrainOrder())
                .partnerAmount(Money.of(8700, "RUB"))
                .feeAmount(Money.of(1300, "RUB"))
                .partnerFeeAmount(Money.of(60, "RUB"))
                .payoutAt(ts("2021-01-20T15:00:38.79Z"))
                .accountingActAt(ts("2021-01-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-ID")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(3);

        BillingTransaction costTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(costTx).isNotNull();
        assertThat(costTx.getId()).isNull();
        assertThat(costTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(costTx.getOriginalTransaction()).isNull();
        assertThat(costTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(costTx.getPaymentType()).isEqualTo(COST);
        assertThat(costTx.getPaymentSystemType()).isEqualTo(SBERBANK);
        assertThat(costTx.getValue()).isEqualTo(Money.of(8700, "RUB"));
        assertThat(costTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(costTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");

        BillingTransaction rewardTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(rewardTx.getOriginalTransaction()).isNull();
        assertThat(rewardTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(rewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(rewardTx.getPaymentSystemType()).isEqualTo(SBERBANK);
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(1300, "RUB"));
        assertThat(rewardTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(rewardTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");

        BillingTransaction feeTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == FEE).findFirst().orElse(null);
        assertThat(feeTx).isNotNull();
        assertThat(feeTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(feeTx.getOriginalTransaction()).isNull();
        assertThat(feeTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(feeTx.getPaymentType()).isEqualTo(FEE);
        assertThat(feeTx.getPaymentSystemType()).isEqualTo(YANDEX);
        assertThat(feeTx.getValue()).isEqualTo(Money.of(60, "RUB"));
        assertThat(feeTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(feeTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");


        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(any(), any(), any()))
                .then(call -> transactions.stream().filter(tx ->
                        tx.getSourceFinancialEvent() == call.getArgument(0)
                                && tx.getPaymentType() == call.getArgument(1)
                ).findFirst().orElse(null));

        FinancialEvent refundEvent = trainsEventBuilder()
                .type(REFUND)
                .originalEvent(sourceEvent)
                .order(testTrainOrder())
                .partnerRefundAmount(Money.of(8700, "RUB"))
                .feeRefundAmount(Money.of(1300, "RUB"))
                .partnerFeeAmount(Money.of(60, "RUB"))
                .payoutAt(ts("2021-01-20T15:00:38.79Z"))
                .accountingActAt(ts("2021-01-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-ID")
                .build();
        List<BillingTransaction> refundTransactions = fixedClockGenerator(now).createBillingTransactions(refundEvent);
        assertThat(refundTransactions).hasSize(3);

        BillingTransaction refundCostTx =
                refundTransactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(refundCostTx).isNotNull();
        assertThat(refundCostTx.getSourceFinancialEvent()).isEqualTo(refundEvent);
        assertThat(refundCostTx.getOriginalTransaction()).isEqualTo(costTx);
        assertThat(refundCostTx.getId()).isNull();
        assertThat(refundCostTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(refundCostTx.getPaymentType()).isEqualTo(COST);
        assertThat(refundCostTx.getPaymentSystemType()).isEqualTo(SBERBANK);
        assertThat(refundCostTx.getValue()).isEqualTo(Money.of(8700, "RUB"));
        assertThat(refundCostTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(refundCostTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");

        BillingTransaction refundRewardTx =
                refundTransactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(refundRewardTx).isNotNull();
        assertThat(refundRewardTx.getSourceFinancialEvent()).isEqualTo(refundEvent);
        assertThat(refundRewardTx.getOriginalTransaction()).isEqualTo(rewardTx);
        assertThat(refundRewardTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(refundRewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(refundRewardTx.getPaymentSystemType()).isEqualTo(SBERBANK);
        assertThat(refundRewardTx.getValue()).isEqualTo(Money.of(1300, "RUB"));
        assertThat(refundRewardTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(refundRewardTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");

        BillingTransaction refundFeeTx =
                refundTransactions.stream().filter(tx -> tx.getPaymentType() == FEE).findFirst().orElse(null);
        assertThat(refundFeeTx).isNotNull();
        assertThat(refundFeeTx.getSourceFinancialEvent()).isEqualTo(refundEvent);
        assertThat(refundFeeTx.getOriginalTransaction()).isNull();
        assertThat(refundFeeTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(refundFeeTx.getPaymentType()).isEqualTo(FEE);
        assertThat(refundFeeTx.getPaymentSystemType()).isEqualTo(YANDEX);
        assertThat(refundFeeTx.getValue()).isEqualTo(Money.of(60, "RUB"));
        assertThat(refundFeeTx.getPayoutAt()).isEqualTo("2021-01-20T15:00:38.790Z");
        assertThat(refundFeeTx.getAccountingActAt()).isEqualTo("2021-01-20T15:00:12.340Z");

        for (BillingTransaction tx : List.of(costTx, rewardTx, feeTx, refundCostTx, refundRewardTx, refundFeeTx)) {
            assertThat(tx.getServiceId()).isEqualTo(171);
        }
    }

    @Test
    public void testSuburbanTransactions() {
        Instant now = ts("2021-03-01T15:00:00.00Z");
        FinancialEvent sourceEvent = suburbanEventBuilder()
                .type(PAYMENT)
                .order(testSuburbanOrder())
                .partnerAmount(Money.of(100.2, "RUB"))
                .feeAmount(Money.of(2.04, "RUB"))
                .payoutAt(Instant.parse("2021-03-01T21:00:00.00Z"))
                .accountingActAt(Instant.parse("2021-03-01T21:00:00.00Z"))
                .billingClientId(4242L)
                .orderPrettyId("pretty")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(2);

        Instant expectedPayout = Instant.parse("2021-03-01T21:00:00.00Z");
        BillingTransaction costTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(costTx).isNotNull();
        assertThat(costTx.getId()).isNull();
        assertThat(costTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(costTx.getOriginalTransaction()).isNull();
        assertThat(costTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(costTx.getPaymentType()).isEqualTo(COST);
        assertThat(costTx.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(costTx.getValue()).isEqualTo(Money.of(100.2, "RUB"));
        assertThat(costTx.getPayoutAt()).isEqualTo(expectedPayout);
        assertThat(costTx.getAccountingActAt()).isEqualTo(expectedPayout);

        BillingTransaction rewardTx =
                transactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(rewardTx).isNotNull();
        assertThat(rewardTx.getSourceFinancialEvent()).isEqualTo(sourceEvent);
        assertThat(rewardTx.getOriginalTransaction()).isNull();
        assertThat(rewardTx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(rewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(rewardTx.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(rewardTx.getValue()).isEqualTo(Money.of(2.04, "RUB"));
        assertThat(rewardTx.getPayoutAt()).isEqualTo(expectedPayout);
        assertThat(rewardTx.getAccountingActAt()).isEqualTo(expectedPayout);

        FinancialEvent refundEvent = suburbanEventBuilder()
                .type(REFUND)
                .originalEvent(sourceEvent)
                .order(testSuburbanOrder())
                .partnerRefundAmount(Money.of(100.2, "RUB"))
                .feeRefundAmount(Money.of(0.36, "RUB"))
                .payoutAt(Instant.parse("2021-03-02T21:00:00.00Z"))
                .accountingActAt(Instant.parse("2021-03-02T21:00:00.00Z"))
                .billingClientId(4242L)
                .orderPrettyId("pretty")
                .build();

        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourceEvent, COST, YANDEX_MONEY)).thenReturn(costTx);
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourceEvent, REWARD, YANDEX_MONEY)).thenReturn(rewardTx);

        List<BillingTransaction> refundTransactions = fixedClockGenerator(now).createBillingTransactions(refundEvent);
        assertThat(refundTransactions).hasSize(2);

        Instant expectedRefundPayout = Instant.parse("2021-03-02T21:00:00.00Z");

        BillingTransaction refundCostTx =
                refundTransactions.stream().filter(tx -> tx.getPaymentType() == COST).findFirst().orElse(null);
        assertThat(refundCostTx).isNotNull();
        assertThat(refundCostTx.getSourceFinancialEvent()).isEqualTo(refundEvent);
        assertThat(refundCostTx.getOriginalTransaction()).isEqualTo(costTx);
        assertThat(refundCostTx.getId()).isNull();
        assertThat(refundCostTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(refundCostTx.getPaymentType()).isEqualTo(COST);
        assertThat(refundCostTx.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(refundCostTx.getValue()).isEqualTo(Money.of(100.2, "RUB"));
        assertThat(refundCostTx.getPayoutAt()).isEqualTo(expectedRefundPayout);
        assertThat(refundCostTx.getAccountingActAt()).isEqualTo(expectedRefundPayout);

        BillingTransaction refundRewardTx =
                refundTransactions.stream().filter(tx -> tx.getPaymentType() == REWARD).findFirst().orElse(null);
        assertThat(refundRewardTx).isNotNull();
        assertThat(refundRewardTx.getSourceFinancialEvent()).isEqualTo(refundEvent);
        assertThat(refundRewardTx.getOriginalTransaction()).isEqualTo(rewardTx);
        assertThat(refundRewardTx.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(refundRewardTx.getPaymentType()).isEqualTo(REWARD);
        assertThat(refundRewardTx.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(refundRewardTx.getValue()).isEqualTo(Money.of(0.36, "RUB"));
        assertThat(refundRewardTx.getPayoutAt()).isEqualTo(expectedRefundPayout);
        assertThat(refundRewardTx.getAccountingActAt()).isEqualTo(expectedRefundPayout);

        for (BillingTransaction tx : List.of(costTx, rewardTx, refundCostTx, refundRewardTx)) {
            assertThat(tx.getServiceId()).isEqualTo(716);
        }
    }

    @Test
    public void createBillingTransactions_yandexAccountTopupPayment() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(YANDEX_ACCOUNT_TOPUP_PAYMENT)
                .order(testHotelOrder())
                .trustPaymentId("some-payment-id")
                .plusTopupAmount(Money.of(1234, "RUB"))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(1);

        BillingTransaction tx = transactions.get(0);
        assertThat(tx).isNotNull();
        assertThat(tx.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(tx.getPaymentType()).isEqualTo(YANDEX_ACCOUNT_TOPUP);
        assertThat(tx.getPaymentSystemType()).isEqualTo(PROMO_CODE);
        assertThat(tx.getValue()).isEqualTo(Money.of(1234, "RUB"));
        assertThat(tx.getTrustPaymentId()).isEqualTo("some-payment-id");
        assertThat(tx.getServiceId()).isEqualTo(HOTELS.getServiceId());
    }

    @Test
    public void createBillingTransactions_yandexAccountPayment() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(PAYMENT)
                .order(testHotelOrder())
                .partnerAmount(rub(0))
                .feeAmount(rub(1))
                .plusPartnerAmount(rub(900))
                .plusFeeAmount(rub(99))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(3);

        BillingTransaction txUserReward = getOnlyTransaction(transactions, REWARD);
        assertThat(txUserReward).isNotNull();
        assertThat(txUserReward.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(txUserReward.getPaymentType()).isEqualTo(REWARD);
        assertThat(txUserReward.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(txUserReward.getValue()).isEqualTo(rub(1));
        assertThat(txUserReward.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txUserReward.getServiceId()).isEqualTo(HOTELS.getServiceId());

        BillingTransaction txPlusReward = getOnlyTransaction(transactions, YANDEX_ACCOUNT_COST_WITHDRAW);
        assertThat(txPlusReward).isNotNull();
        assertThat(txPlusReward.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(txPlusReward.getPaymentType()).isEqualTo(YANDEX_ACCOUNT_COST_WITHDRAW);
        assertThat(txPlusReward.getPaymentSystemType()).isEqualTo(PROMO_CODE);
        assertThat(txPlusReward.getValue()).isEqualTo(rub(900));
        assertThat(txPlusReward.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txPlusReward.getServiceId()).isEqualTo(HOTELS.getServiceId());

        BillingTransaction txPlusCost = getOnlyTransaction(transactions, YANDEX_ACCOUNT_REWARD_WITHDRAW);
        assertThat(txPlusCost).isNotNull();
        assertThat(txPlusCost.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(txPlusCost.getPaymentType()).isEqualTo(YANDEX_ACCOUNT_REWARD_WITHDRAW);
        assertThat(txPlusCost.getPaymentSystemType()).isEqualTo(PROMO_CODE);
        assertThat(txPlusCost.getValue()).isEqualTo(rub(99));
        assertThat(txPlusCost.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txPlusCost.getServiceId()).isEqualTo(HOTELS.getServiceId());
    }

    @Test
    public void createBillingTransactions_yandexAccountRefund() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourcePaymentEvent = event(1, ts("2019-01-01"));
        FinancialEvent sourceEvent = hotelsEventBuilder()
                .type(REFUND)
                .originalEvent(sourcePaymentEvent)
                .order(testHotelOrder())
                .partnerRefundAmount(rub(0))
                .feeRefundAmount(rub(1))
                .plusPartnerRefundAmount(rub(900))
                .plusFeeRefundAmount(rub(99))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, COST, YANDEX_MONEY))
                .thenReturn(transaction(101, PAYMENT, COST, YANDEX_MONEY, 0));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, YANDEX_MONEY))
                .thenReturn(transaction(102, PAYMENT, REWARD, YANDEX_MONEY, 1));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, YANDEX_ACCOUNT_COST_WITHDRAW, PROMO_CODE))
                .thenReturn(transaction(103, PAYMENT, YANDEX_ACCOUNT_COST_WITHDRAW, PROMO_CODE, 900));
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, YANDEX_ACCOUNT_REWARD_WITHDRAW, PROMO_CODE))
                .thenReturn(transaction(104, PAYMENT, YANDEX_ACCOUNT_REWARD_WITHDRAW, PROMO_CODE, 99));

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(3);

        BillingTransaction txUserReward = getOnlyTransaction(transactions, REWARD);
        assertThat(txUserReward).isNotNull();
        assertThat(txUserReward.getOriginalTransaction().getId()).isEqualTo(102);
        assertThat(txUserReward.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(txUserReward.getPaymentType()).isEqualTo(REWARD);
        assertThat(txUserReward.getPaymentSystemType()).isEqualTo(YANDEX_MONEY);
        assertThat(txUserReward.getValue()).isEqualTo(rub(1));
        assertThat(txUserReward.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txUserReward.getServiceId()).isEqualTo(HOTELS.getServiceId());

        BillingTransaction txPlusReward = getOnlyTransaction(transactions, YANDEX_ACCOUNT_COST_WITHDRAW);
        assertThat(txPlusReward).isNotNull();
        assertThat(txPlusReward.getOriginalTransaction().getId()).isEqualTo(103);
        assertThat(txPlusReward.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(txPlusReward.getPaymentType()).isEqualTo(YANDEX_ACCOUNT_COST_WITHDRAW);
        assertThat(txPlusReward.getPaymentSystemType()).isEqualTo(PROMO_CODE);
        assertThat(txPlusReward.getValue()).isEqualTo(rub(900));
        assertThat(txPlusReward.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txPlusReward.getServiceId()).isEqualTo(HOTELS.getServiceId());

        BillingTransaction txPlusCost = getOnlyTransaction(transactions, YANDEX_ACCOUNT_REWARD_WITHDRAW);
        assertThat(txPlusCost).isNotNull();
        assertThat(txPlusCost.getOriginalTransaction().getId()).isEqualTo(104);
        assertThat(txPlusCost.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(txPlusCost.getPaymentType()).isEqualTo(YANDEX_ACCOUNT_REWARD_WITHDRAW);
        assertThat(txPlusCost.getPaymentSystemType()).isEqualTo(PROMO_CODE);
        assertThat(txPlusCost.getValue()).isEqualTo(rub(99));
        assertThat(txPlusCost.getTrustPaymentId()).isEqualTo("trust_payment_id");
        assertThat(txPlusCost.getServiceId()).isEqualTo(HOTELS.getServiceId());
    }

    @Test
    public void createBillingTransactions_postPay() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourceEvent = hotelsPostPayEventBuilder()
                .type(PAYMENT)
                .order(testHotelOrder())
                .partnerAmount(rub(0))
                .feeAmount(rub(0))
                .postPayUserAmount(rub(12000))
                .postPayPartnerPayback(rub(1200))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceEvent);
        assertThat(transactions).hasSize(1);

        BillingTransaction txUserReward = getOnlyTransaction(transactions, REWARD);
        assertThat(txUserReward).isNotNull();
        assertThat(txUserReward.getTransactionType()).isEqualTo(BillingTransactionType.PAYMENT);
        assertThat(txUserReward.getPaymentType()).isEqualTo(REWARD);
        assertThat(txUserReward.getPaymentSystemType()).isEqualTo(POSTPAY);
        assertThat(txUserReward.getValue()).isEqualTo(rub(1200));
        assertThat(txUserReward.getServiceId()).isEqualTo(HOTELS_POSTPAY.getServiceId());
    }

    @Test
    public void createBillingTransactions_postPayRefund() {
        Instant now = ts("2019-11-20T00:00:00.00Z");
        FinancialEvent sourcePaymentEvent = hotelsPostPayEventBuilder()
                .type(PAYMENT)
                .order(testHotelOrder())
                .partnerAmount(rub(0))
                .feeAmount(rub(0))
                .postPayUserAmount(rub(12000))
                .postPayPartnerPayback(rub(1200))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();
        when(billingTransactionRepository.findBySourceFinancialEventAndPaymentTypeAndPaymentSystemType(
                sourcePaymentEvent, REWARD, POSTPAY))
                .thenReturn(transaction(101, PAYMENT, REWARD, POSTPAY, 1200, HOTELS_POSTPAY));

        FinancialEvent sourceRefundEvent = hotelsPostPayEventBuilder()
                .type(REFUND)
                .originalEvent(sourcePaymentEvent)
                .order(testHotelOrder())
                .partnerRefundAmount(rub(0))
                .feeRefundAmount(rub(0))
                .postPayUserRefund(rub(12000))
                .postPayPartnerRefund(rub(1200))
                .payoutAt(ts("2019-11-24T21:45:38.79Z"))
                .accountingActAt(ts("2019-12-20T15:00:12.34Z"))
                .billingClientId(8753L)
                .orderPrettyId("YA-...")
                .build();

        List<BillingTransaction> transactions = fixedClockGenerator(now).createBillingTransactions(sourceRefundEvent);
        assertThat(transactions).hasSize(1);

        BillingTransaction txUserReward = getOnlyTransaction(transactions, REWARD);
        assertThat(txUserReward).isNotNull();
        assertThat(txUserReward.getTransactionType()).isEqualTo(BillingTransactionType.REFUND);
        assertThat(txUserReward.getPaymentType()).isEqualTo(REWARD);
        assertThat(txUserReward.getPaymentSystemType()).isEqualTo(POSTPAY);
        assertThat(txUserReward.getValue()).isEqualTo(rub(1200));
        assertThat(txUserReward.getServiceId()).isEqualTo(HOTELS_POSTPAY.getServiceId());
    }

    private FinancialEvent event(long id, Instant payoutAt) {
        return hotelsEventBuilder().id(id).payoutAt(payoutAt).build();
    }

    private FinancialEvent.FinancialEventBuilder hotelsEventBuilder() {
        return FinancialEvent.builder()
                .paymentScheme(HOTELS);
    }

    private FinancialEvent.FinancialEventBuilder hotelsPostPayEventBuilder() {
        return FinancialEvent.builder()
                .paymentScheme(HOTELS_POSTPAY);
    }

    private FinancialEvent.FinancialEventBuilder trainsEventBuilder() {
        return FinancialEvent.builder()
                .paymentScheme(TRAINS);
    }

    private FinancialEvent.FinancialEventBuilder suburbanEventBuilder() {
        return FinancialEvent.builder()
                .paymentScheme(SUBURBAN);
    }

    @SuppressWarnings("SameParameterValue")
    private BillingTransaction transaction(long id, FinancialEventType eventType,
                                           BillingTransactionPaymentType paymentType,
                                           BillingTransactionPaymentSystemType paymentSystemType,
                                           int value) {
        return transaction(id, eventType, paymentType, paymentSystemType, value, HOTELS);
    }

    private BillingTransaction transaction(long id, FinancialEventType eventType,
                                           BillingTransactionPaymentType paymentType,
                                           BillingTransactionPaymentSystemType paymentSystemType,
                                           int value, FinancialEventPaymentScheme scheme) {
        BillingTransactionType transactionType = eventType == PAYMENT ?
                BillingTransactionType.PAYMENT : BillingTransactionType.REFUND;
        return BillingTransaction.builder()
                .serviceId(scheme.getServiceId())
                .id(id)
                .transactionType(transactionType)
                .paymentType(paymentType)
                .paymentSystemType(paymentSystemType)
                .value(Money.of(value, "RUB"))
                .build();
    }

    private Instant ts(String ts) {
        if (ts.matches("\\d{4}-\\d{2}-\\d{2}")) {
            ts += "T00:00:00.000Z";
        }
        return Instant.parse(ts);
    }

    private BillingTransactionGenerator fixedClockGenerator(Instant currentTime) {
        return new BillingTransactionGenerator(
                financialEventRepository,
                billingTransactionRepository,
                ClockService.create(Clock.fixed(currentTime, ZoneId.systemDefault()))
        );
    }

    private Order testHotelOrder() {
        HotelOrder order = new HotelOrder();
        DolphinOrderItem orderItem = new DolphinOrderItem();
        orderItem.setId(UUID.fromString("0-0-0-0-255"));
        orderItem.setItinerary(new DolphinHotelItinerary());
        orderItem.getHotelItinerary().setCheckinDate(LocalDate.now());
        order.addOrderItem(orderItem);
        Invoice invoice = new TrustInvoice();
        invoice.setTrustPaymentId("trust_payment_id");
        order.addInvoice(invoice);
        return order;
    }

    private Order testTrainOrder() {
        GenericOrder order = new GenericOrder();
        TrainOrderItem orderItem = new TrainOrderItem();
        orderItem.setId(UUID.fromString("0-0-0-0-254"));
        orderItem.setReservation(new TrainReservation());
        order.addOrderItem(orderItem);
        Invoice invoice = new TrustInvoice();
        invoice.setTrustPaymentId("trust_payment_id");
        order.addInvoice(invoice);
        return order;
    }

    private Order testSuburbanOrder() {
        var order = new GenericOrder();
        order.setId(UUID.randomUUID());
        order.setPrettyId("pretty");

        Invoice invoice = new TrustInvoice();
        invoice.setTrustPaymentId("trust_payment_id");
        order.addInvoice(invoice);

        return order;
    }

    private BillingTransaction getOnlyTransaction(List<BillingTransaction> transactions,
                                                  BillingTransactionPaymentType type) {
        return transactions.stream()
                .filter(tx -> tx.getPaymentType() == type)
                .collect(exactlyOne());
    }
}
