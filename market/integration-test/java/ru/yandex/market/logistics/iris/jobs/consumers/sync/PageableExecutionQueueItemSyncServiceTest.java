package ru.yandex.market.logistics.iris.jobs.consumers.sync;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.model.PageableExecutionQueueItemPayload;
import ru.yandex.market.logistics.iris.util.ExecutionResult;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class PageableExecutionQueueItemSyncServiceTest {

    private static final int FROM = 0;
    private static final int TO = 10;
    private static final int TOTAL = 9;
    private static final int PAGE_SIZE = TO - FROM;

    private static final Source SOURCE = new Source("145", SourceType.WAREHOUSE);
    private static PageableExecutionQueueItemPayload payload =
        new PageableExecutionQueueItemPayload("", FROM, TO, TOTAL, SOURCE);

    @Mock
    private PageableSyncStrategy<Integer> strategyMock;

    private PageableExecutionQueueItemSyncService<Integer> syncService;

    @Before
    public void setUp() {
        this.syncService = new PageableExecutionQueueItemSyncService<>(strategyMock);
    }

    /**
     * Обрабатываем не последнюю страницу -> не обрабатываем lastBatches.
     */
    @Test
    public void processNotLastPage() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3);
        payload.setTotal(20);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(filledCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Обрабатываем последнюю страницу и получаем пустой результат -> не обрабатываем lastBatches.
     */
    @Test
    public void doNotProcessLastBatchIfLastPageResultIsEmpty() {
        List<Integer> emptyCollection = Collections.emptyList();

        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(emptyCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Обрабатываем последнюю страницу и получаем непустой результат -> обрабатываем lastBatches, который пуст.
     */
    @Test
    public void processEmptyLastBatchIfLastPageResultIsNotEmpty() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3, 1, 2, 3, 1, 2, 3, 1);
        List<Integer> emptyCollection = Collections.emptyList();

        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);
        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(filledCollection, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);
        verify(strategyMock, times(1)).saveLastCount(SOURCE, 10L);
        verify(strategyMock, never()).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Обрабатываем последнюю страницу и получаем непустой результат -> обрабатываем непустой lastBatches.
     */
    @Test
    public void processLastBatchIfLastPageResultIsNotEmpty() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3, 1, 2, 3, 1, 2, 3, 1);
        List<Integer> notEmptyCollection = ImmutableList.of(1, 2, 3);
        List<Integer> emptyCollection = ImmutableList.of();

        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(notEmptyCollection, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok(notEmptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);
        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, PAGE_SIZE + 3, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, PAGE_SIZE + 3, SOURCE);
        verify(strategyMock, times(1)).process(filledCollection, SOURCE);
        verify(strategyMock, times(1)).process(notEmptyCollection, SOURCE);
        verify(strategyMock, times(1)).saveLastCount(SOURCE, 13L);
        verify(strategyMock, never()).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Обрабатываем последнюю страницу и получаем непустой результат несколько раз -> обрабатываем непустые lastBatches.
     */
    @Test
    public void processLastBatchIfLastPageResultIsNotEmptyMultipleTimes() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3, 1, 2, 3, 1, 2, 3, 1);
        List<Integer> notEmptyCollection = ImmutableList.of(1, 2, 3);
        List<Integer> emptyCollection = ImmutableList.of();

        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(notEmptyCollection, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok(notEmptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, PAGE_SIZE + 3, SOURCE);
        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquireWithOffset(PAGE_SIZE, 2 * PAGE_SIZE + 3, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, PAGE_SIZE, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, PAGE_SIZE + 3, SOURCE);
        verify(strategyMock, times(1)).acquireWithOffset(PAGE_SIZE, 2 * PAGE_SIZE + 3, SOURCE);
        verify(strategyMock, times(2)).process(filledCollection, SOURCE);
        verify(strategyMock, times(1)).process(notEmptyCollection, SOURCE);
        verify(strategyMock, times(1)).saveLastCount(SOURCE, 23L);
        verify(strategyMock, never()).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    @Test(expected = RuntimeException.class)
    public void throwExceptionWhenAcquireIsFailedToRetry() {
        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquireWithOffset(PAGE_SIZE, 0, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }
}
