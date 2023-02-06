package ru.yandex.travel.orders.services.finances.billing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.integration.balance.BillingApiClient;
import ru.yandex.travel.orders.entities.finances.BillingTransaction;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;
import ru.yandex.travel.orders.entities.finances.ProcessingTasksInfo;
import ru.yandex.travel.orders.repository.BillingTransactionRepository;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BillingTransactionActCommitterTest {
    private BillingTransactionRepository txRepository;
    private BillingApiClient billingApiClient;

    @Before
    public void init() {
        txRepository = Mockito.mock(BillingTransactionRepository.class);
        billingApiClient = Mockito.mock(BillingApiClient.class);
    }

    @Test
    public void fetchTransactionIdsWaitingForCommit() {
        BillingTransactionActCommitter committer = actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H17M");
        AtomicReference<Instant> payoutAtLimit = new AtomicReference<>();
        AtomicReference<Instant> actAtLimit = new AtomicReference<>();
        when(txRepository.findIdsReadyForActCommit(any(), any(), any(), any())).thenAnswer(invocation -> {
            payoutAtLimit.set(invocation.getArgument(0));
            actAtLimit.set(invocation.getArgument(1));
            return List.of();
        });

        committer.getTransactionIdsWaitingForCommit(Set.of(), 1);
        // transactions payed out 2 hours and 17 minutes ago or earlier
        assertThat(payoutAtLimit.get()).isEqualTo("2019-12-17T09:16:30Z");
        assertThat(actAtLimit.get()).isEqualTo("2019-12-17T11:33:30Z");
    }

    @Test
    public void countTransactionsWaitingForCommit() {
        BillingTransactionActCommitter committer = actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H17M");
        AtomicReference<Instant> payoutAtLimit = new AtomicReference<>();
        AtomicReference<Instant> actAtLimit = new AtomicReference<>();
        when(txRepository.countReadyForActCommit(any(), any(), any())).thenAnswer(invocation -> {
            payoutAtLimit.set(invocation.getArgument(0));
            actAtLimit.set(invocation.getArgument(1));
            return -1L;
        });

        committer.countTransactionsWaitingForCommit(Set.of());
        // transactions payed out 2 hours and 17 minutes ago or earlier
        assertThat(payoutAtLimit.get()).isEqualTo("2019-12-17T09:16:30Z");
        assertThat(actAtLimit.get()).isEqualTo("2019-12-17T11:33:30Z");
    }

    @Test
    public void processTransactionWaitingForCommit() {
        BillingTransaction tx = defaultTxBuilder(1, 234)
                .serviceId(641L)
                .sourceFinancialEvent(Mockito.mock(FinancialEvent.class, RETURNS_DEEP_STUBS))
                .build();
        when(txRepository.getOne(1L)).thenReturn(tx);
        assertThat(tx.isActCommitted()).isFalse();

        Instant now = Instant.parse("2019-12-17T11:33:30Z");
        actCommitter(now, "PT2H").processTransactionWaitingForCommit(1L);
        verify(billingApiClient, times(1)).updatePayment(eq(641L), eq(234L), any());
        assertThat(tx.isActCommitted()).isTrue();
        assertThat(tx.getActCommittedAt()).isEqualTo(now);
    }

    @Test
    public void processTransactionWaitingForCommit_alreadyProcessed() {
        BillingTransaction tx = Mockito.mock(BillingTransaction.class, RETURNS_DEEP_STUBS);
        when(tx.isActCommitted()).thenReturn(true);
        when(txRepository.getOne(1L)).thenReturn(tx);
        actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H").processTransactionWaitingForCommit(1L);
        verify(billingApiClient, times(0)).updatePayment(anyLong(), anyLong(), any());
    }

    @Test
    public void processTransactionWaitingForCommit_actInThePast_tooOld() {
        BillingTransaction tx = defaultTxBuilder(1, 927342423)
                .payoutAt(Instant.parse("2021-06-21T00:00:00Z"))
                .accountingActAt(Instant.parse("2021-06-21T02:00:00Z"))
                .build();
        when(txRepository.getOne(tx.getId())).thenReturn(tx);

        actCommitter(Instant.parse("2021-06-22T19:00:00Z"), "PT2H").processTransactionWaitingForCommit(tx.getId());
        verify(billingApiClient, times(1)).updatePayment(anyLong(), anyLong(), any());

        // payout_at isn't touched, accounting_at is updated
        assertThat(tx.getPayoutAt()).isEqualTo("2021-06-21T00:00:00Z");
        assertThat(tx.getAccountingActAt()).isEqualTo("2021-06-22T19:00:00Z");
    }

    @Test
    public void processTransactionWaitingForCommit_actInThePast_notTooOld() {
        BillingTransaction tx = defaultTxBuilder(1, 342432)
                .payoutAt(Instant.parse("2021-06-21T00:00:00Z"))
                .accountingActAt(Instant.parse("2021-06-21T22:00:00Z"))
                .build();
        when(txRepository.getOne(tx.getId())).thenReturn(tx);

        actCommitter(Instant.parse("2021-06-22T19:00:00Z"), "PT2H").processTransactionWaitingForCommit(tx.getId());
        verify(billingApiClient, times(1)).updatePayment(anyLong(), anyLong(), any());

        // payout_at and accounting_at aren't touched
        assertThat(tx.getPayoutAt()).isEqualTo("2021-06-21T00:00:00Z");
        assertThat(tx.getAccountingActAt()).isEqualTo("2021-06-21T22:00:00Z");
    }

    @Test
    public void getCurrentProcessingDelay() {
        when(txRepository.findMaxDelaySecondsOfTxReadyForActCommit(any(), any()))
                .thenReturn(new ProcessingTasksInfo(null, 0L));
        assertThat(actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H").getCurrentProcessingDelay())
                .isEqualTo(Duration.ZERO);

        when(txRepository.findMaxDelaySecondsOfTxReadyForActCommit(any(), any()))
                .thenReturn(new ProcessingTasksInfo(null, 1L));
        assertThat(actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H").getCurrentProcessingDelay())
                .isEqualTo(Duration.ofHours(24));

        when(txRepository.findMaxDelaySecondsOfTxReadyForActCommit(any(), any()))
                .thenReturn(new ProcessingTasksInfo(Instant.ofEpochSecond(90L), 1L));
        assertThat(actCommitter(Instant.parse("2019-12-17T11:33:30Z"), "PT2H").getCurrentProcessingDelay())
                .isEqualTo(Duration.ofSeconds(90));
    }

    private BillingTransactionActCommitter actCommitter(Instant now, String commitDelay) {
        ClockService fixedClockService = ClockService.create(Clock.fixed(now, ZoneId.of("UTC")));
        return new BillingTransactionActCommitter(txRepository, billingApiClient, fixedClockService,
                Duration.parse(commitDelay));
    }

    private BillingTransaction.BillingTransactionBuilder defaultTxBuilder(long id, long ytId) {
        return BillingTransaction.builder()
                .id(id)
                .serviceId(6324624L)
                .ytId(ytId)
                .accountingActAt(Instant.now())
                ;
    }
}
