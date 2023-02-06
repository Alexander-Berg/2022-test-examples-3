package ru.yandex.direct.jobs.campdaybudgethistory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.campdaybudgethistory.repository.CampDayBudgetStopHistoryRepository;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.utils.TimeProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты на джобу {@link ru.yandex.direct.jobs.campdaybudgethistory.ClearCampDayBudgetStopHistoryJob}.
 */
@JobsTest
class ClearCampDayBudgetStopHistoryJobTest {

    private static final int SHARD = 2;
    private static final int SELECT_LIMIT = 6;
    private static final int DELETE_LIMIT = 3;
    private static final LocalDateTime NOW = LocalDateTime.of(2018, 9, 1, 0, 0);

    private CampDayBudgetStopHistoryRepository campDayBudgetStopHistoryRepository;

    private ClearCampDayBudgetStopHistoryJob clearCampDayBudgetStopHistoryJob;

    @BeforeEach
    void initMocks() {
        TimeProvider timeProvider = mock(TimeProvider.class);
        campDayBudgetStopHistoryRepository = mock(CampDayBudgetStopHistoryRepository.class);
        clearCampDayBudgetStopHistoryJob = new ClearCampDayBudgetStopHistoryJob(SHARD,
                campDayBudgetStopHistoryRepository, timeProvider, SELECT_LIMIT, DELETE_LIMIT);
        when(timeProvider.now()).thenReturn(NOW);
    }

    /**
     * Тестируем случай, когда джоба не получает ключей для удаления. Метод удаления не должен вызываться.
     */
    @Test
    void execute_noExpiredRecords_noDelete()  {
        when(campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(eq(SHARD),
                any(LocalDateTime.class), anyInt())).thenReturn(Collections.emptyList());

        clearCampDayBudgetStopHistoryJob.execute();

        verify(campDayBudgetStopHistoryRepository, never()).delete(eq(SHARD), anyList());
    }

    /**
     * Тестируем случай, когда джоба получает ключи на удаление и вызывает метод удаления с ними.
     */
    @Test
    void execute_expiredRecords_deleteCalledWithExpectedIds()  {
        when(campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(eq(SHARD),
                any(LocalDateTime.class), anyInt())).thenReturn(List.of(1L, 2L, 3L));

        clearCampDayBudgetStopHistoryJob.execute();

        verify(campDayBudgetStopHistoryRepository).delete(eq(SHARD), eq(List.of(1L, 2L, 3L)));
    }

    /**
     * Тестируем, что джоба вызовет метод для получения записей еще раз, если в первый раз получила максимальное
     * число записей
     */
    @Test
    void execute_recordsMoreThenSelectLimit_getMethodCalledAgain()  {
        List<Long> ids = new ArrayList<>(Collections.nCopies(SELECT_LIMIT, 0L));
        when(campDayBudgetStopHistoryRepository.getIdsOfExpiredRecords(eq(SHARD),
                any(LocalDateTime.class), anyInt()))
                .thenReturn(ids)
                .thenReturn(List.of(1L, 2L, 3L));

        clearCampDayBudgetStopHistoryJob.execute();

        verify(campDayBudgetStopHistoryRepository, times(3)).delete(eq(SHARD), anyCollection());
    }
}
