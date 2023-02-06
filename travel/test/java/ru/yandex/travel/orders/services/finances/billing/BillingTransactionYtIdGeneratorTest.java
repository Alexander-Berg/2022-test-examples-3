package ru.yandex.travel.orders.services.finances.billing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.orders.entities.finances.ProcessingTasksInfo;
import ru.yandex.travel.orders.repository.BillingTransactionRepository;
import ru.yandex.travel.utils.ClockService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.services.finances.billing.BillingTransactionYtIdGenerator.SINGLETON_BILLING_TX_YT_ID_GENERATOR_TASK;

public class BillingTransactionYtIdGeneratorTest {
    private BillingTransactionRepository txRepository;
    private BillingTransactionYtIdGenerator idGenerator;

    @Before
    public void init() {
        txRepository = Mockito.mock(BillingTransactionRepository.class);
        idGenerator = idGenerator(new ClockService());
    }

    private BillingTransactionYtIdGenerator idGenerator(ClockService clockService) {
        return new BillingTransactionYtIdGenerator(txRepository, clockService);
    }

    @Test
    public void getYtIdBulkGenerationTaskIds() {
        assertThat(idGenerator.getYtIdBulkGenerationTaskIds(Set.of())).isEmpty();

        when(txRepository.countReadyTransactionsWithoutYtId(any())).thenReturn(5L);
        assertThat(idGenerator.getYtIdBulkGenerationTaskIds(Set.of())).hasSize(1)
                .containsExactly(SINGLETON_BILLING_TX_YT_ID_GENERATOR_TASK);

        when(txRepository.countReadyTransactionsWithoutYtId(any())).thenReturn(5L);
        assertThat(idGenerator.getYtIdBulkGenerationTaskIds(Set.of(SINGLETON_BILLING_TX_YT_ID_GENERATOR_TASK))).isEmpty();
    }

    @Test
    public void bulkGenerateNewYtIds() {
        idGenerator.bulkGenerateNewYtIds(SINGLETON_BILLING_TX_YT_ID_GENERATOR_TASK);
        verify(txRepository).generateNewYtIdsForReadyTransactions(any());
    }

    @Test
    public void getCurrentProcessingDelay() {
        Clock fixedClock = Clock.fixed(Instant.parse("2019-12-27T13:35:00Z"), ZoneId.of("UTC"));
        BillingTransactionYtIdGenerator generator = idGenerator(ClockService.create(fixedClock));
        when(txRepository.findOldestTimestampReadyForYtIdGeneration(any()))
                .thenReturn(new ProcessingTasksInfo(null, 0L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.ZERO);

        when(txRepository.findOldestTimestampReadyForYtIdGeneration(any()))
                .thenReturn(new ProcessingTasksInfo(null, 1L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.ofHours(24));

        when(txRepository.findOldestTimestampReadyForYtIdGeneration(any()))
                .thenReturn(new ProcessingTasksInfo(Instant.parse("2019-12-27T13:20:00Z"), 1L));
        assertThat(generator.getCurrentProcessingDelay()).isEqualTo(Duration.ofMinutes(15));
    }
}
