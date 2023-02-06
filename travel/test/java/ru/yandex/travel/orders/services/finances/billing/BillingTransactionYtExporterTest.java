package ru.yandex.travel.orders.services.finances.billing;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.orders.entities.finances.BillingTransaction;
import ru.yandex.travel.orders.entities.finances.ProcessingTasksInfo;
import ru.yandex.travel.orders.repository.BillingTransactionRepository;
import ru.yandex.travel.testing.time.SettableClock;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.services.finances.billing.BillingTransactionYtExporter.SINGLETON_BILLING_TX_EXPORT_TASK;

public class BillingTransactionYtExporterTest {
    private BillingTransactionRepository repository;
    private BillingTransactionYtTableClient ytTableClient;
    private BillingTransactionYtExporter exporter;
    private SettableClock clock;

    @Before
    public void init() {
        repository = Mockito.mock(BillingTransactionRepository.class);
        ytTableClient = Mockito.mock(BillingTransactionYtTableClient.class);
        clock = new SettableClock();
        exporter = exporter(ClockService.create(clock));
    }

    private BillingTransactionYtExporter exporter(ClockService clockService) {
        // the batchSize param is ignored in this test
        BillingTransactionYtTableClientProperties properties =
                BillingTransactionYtTableClientProperties.builder().batchSize(1).build();
        return new BillingTransactionYtExporter(properties, repository, ytTableClient, clockService);
    }

    @Test
    public void getExportTasks() {
        assertThat(exporter.getExportTasks(Set.of())).isEmpty();

        when(repository.countReadyForExport()).thenReturn(5L);
        assertThat(exporter.getExportTasks(Set.of())).hasSize(1)
                .containsExactly(SINGLETON_BILLING_TX_EXPORT_TASK);

        when(repository.countReadyForExport()).thenReturn(5L);
        assertThat(exporter.getExportTasks(Set.of(SINGLETON_BILLING_TX_EXPORT_TASK))).isEmpty();
    }

    @Test
    public void startExportTask_unexpectedTask() {
        assertThatThrownBy(() -> exporter.startExportTask("rnd"))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void startExportTask_nothingToExport() {
        when(repository.findReadyForExport(any())).thenReturn(List.of());
        exporter.startExportTask(SINGLETON_BILLING_TX_EXPORT_TASK);
        verify(ytTableClient, times(0)).exportTransactions(any(), any());
    }

    @Test
    public void startExportTask_happyPath() {
        List<BillingTransaction> transactions = List.of(
                txWithPayoutAtAndYtId("2019-12-11T13:34:56.78Z", 1),
                txWithPayoutAtAndYtId("2019-12-11T13:34:56.78Z", 2)
        );
        when(repository.findReadyForExport(any())).thenReturn(transactions);
        exporter.startExportTask(SINGLETON_BILLING_TX_EXPORT_TASK);
        assertThat(transactions)
                .allMatch(BillingTransaction::isExportedToYt)
                .allMatch(tx -> tx.getExportedToYtAt() != null);
        verify(ytTableClient, times(1)).exportTransactions(any(), any());
    }

    @Test
    public void startExportTask_exportsAtMostOneDay() {
        List<BillingTransaction> transactions = List.of(
                txWithPayoutAtAndYtId("2019-12-11T13:34:56.78Z", 1),
                txWithPayoutAtAndYtId("2019-12-12T13:34:56.78Z", 2)
        );
        when(repository.findReadyForExport(any())).thenReturn(transactions);
        exporter.startExportTask(SINGLETON_BILLING_TX_EXPORT_TASK);
        assertThat(transactions.get(0).isExportedToYt()).isTrue();
        assertThat(transactions.get(1).isExportedToYt()).isFalse();
    }

    @Test
    public void startExportTask_pastDates() {
        List<BillingTransaction> transactions = List.of(
                txWithPayoutAtAndYtId("2021-06-21T04:00:00Z", 1),
                txWithPayoutAtAndYtId("2021-06-21T19:00:00Z", 2)
        );
        transactions.forEach(tx -> tx.setAccountingActAt(Instant.parse("2000-06-22T00:00:00Z")));
        clock.setCurrentTime(Instant.parse("2021-06-22T16:00:00Z"));
        when(repository.findReadyForExport(any())).thenReturn(transactions);
        exporter.startExportTask(SINGLETON_BILLING_TX_EXPORT_TASK);

        // payout changed only for the 24h+ delayed tx (1st)
        assertThat(transactions.get(0).getPayoutAt()).isEqualTo("2021-06-22T16:00:00Z");
        assertThat(transactions.get(1).getPayoutAt()).isEqualTo("2021-06-21T19:00:00Z");
        // act isn't changed
        assertThat(transactions).allSatisfy(tx ->
                assertThat(tx.getAccountingActAt()).isEqualTo("2000-06-22T00:00:00Z"));
    }

    @Test
    public void getBatchToUpload() {
        List<BillingTransaction> transactions = List.of(
                txWithPayoutAtAndYtId("2019-12-11T13:34:56.78Z", 1),
                txWithPayoutAtAndYtId("2019-10-02T13:34:56.78Z", 2),
                txWithPayoutAtAndYtId("2019-12-11T14:34:56.78Z", 3),
                txWithPayoutAtAndYtId("2019-12-11T13:34:56.78Z", 4),
                txWithPayoutAtAndYtId("2019-12-11T15:34:56.78Z", 5)
        );

        // only continuous batches are formed
        when(repository.findReadyForExport(any())).thenReturn(transactions);
        List<BillingTransaction> filtered = exporter.getBatchToUpload(1); // batchSize is ignored
        assertThat(filtered).isEqualTo(List.of(
                transactions.get(0)
        ));

        // this batch for 2019-10-02 splits all available transactions for 2019-12-11
        when(repository.findReadyForExport(any())).thenReturn(transactions.subList(1, 5));
        filtered = exporter.getBatchToUpload(1); // batchSize is ignored
        assertThat(filtered).isEqualTo(List.of(
                transactions.get(1)
        ));

        // the rest transactions fall into the same batch
        when(repository.findReadyForExport(any())).thenReturn(transactions.subList(2, 5));
        filtered = exporter.getBatchToUpload(1); // batchSize is ignored
        assertThat(filtered).isEqualTo(List.of(
                transactions.get(2),
                transactions.get(3),
                transactions.get(4)
        ));
    }

    @Test
    public void getCurrentProcessingDelay() {
        clock.setCurrentTime(Instant.parse("2019-12-27T13:35:00Z"));
        when(repository.findOldestTimestampReadyForYtExport()).thenReturn(new ProcessingTasksInfo(null, 0L));
        assertThat(exporter.getCurrentProcessingDelay()).isEqualTo(Duration.ZERO);

        when(repository.findOldestTimestampReadyForYtExport()).thenReturn(new ProcessingTasksInfo(null, 1L));
        assertThat(exporter.getCurrentProcessingDelay()).isEqualTo(Duration.ofHours(24));

        when(repository.findOldestTimestampReadyForYtExport()).thenReturn(
                new ProcessingTasksInfo(Instant.parse("2019-12-27T13:20:00Z"), 1L));
        assertThat(exporter.getCurrentProcessingDelay()).isEqualTo(Duration.ofMinutes(15));
    }

    private BillingTransaction txWithPayoutAtAndYtId(String payoutAt, long ytId) {
        return BillingTransactionTestHelper.mockTransactionBuilder(-1)
                .payoutAt(Instant.parse(payoutAt))
                .ytId(ytId)
                .build();
    }
}
