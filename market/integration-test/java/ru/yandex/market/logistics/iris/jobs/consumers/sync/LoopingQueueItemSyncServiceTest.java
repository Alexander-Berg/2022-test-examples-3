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
import ru.yandex.market.logistics.iris.jobs.model.LoopingQueueItemPayload;
import ru.yandex.market.logistics.iris.util.ExecutionResult;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class LoopingQueueItemSyncServiceTest {

    private static final int PAGE_SIZE = 10;
    private static final Source SOURCE = new Source("145", SourceType.WAREHOUSE);
    private static final LoopingQueueItemPayload payload = new LoopingQueueItemPayload("", PAGE_SIZE, SOURCE);

    @Mock
    private PageableSyncStrategy<Integer> strategyMock;

    private LoopingQueueItemSyncService<Integer> syncService;

    @Before
    public void setUp() {
        this.syncService = new LoopingQueueItemSyncService<>(3, strategyMock);
    }

    /**
     * Первая итерация возвращает пустой ответ -> завершаем цикл.
     */
    @Test
    public void singleEmptyIteration() {
        List<Integer> emptyCollection = Collections.emptyList();

        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquire(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(emptyCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Первая итерация возвращает полный ответ -> переходим ко второй.
     * Вторая итерация возвращает пустой ответ -> завершаем цикл.
     */
    @Test
    public void singleNonEmptyIteration() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3);
        List<Integer> emptyCollection = Collections.emptyList();

        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);
        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquire(PAGE_SIZE, 1, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(emptyCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(filledCollection, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 1, SOURCE);
        verify(strategyMock, times(1)).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Первая итерация возвращает ошибку -> переходим ко второй.
     * Вторая итерация возвращает ошибку -> переходим к третьей.
     * Третья итерация возвращает пустой список  -> завершаем цикл.
     */
    @Test
    public void twoFailuresThenEmptyIteration() {
        List<Integer> emptyCollection = Collections.emptyList();

        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquire(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquire(PAGE_SIZE, 1, SOURCE);
        doReturn(ExecutionResult.ok(emptyCollection)).when(strategyMock).acquire(PAGE_SIZE, 2, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(emptyCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 1, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 2, SOURCE);
        verify(strategyMock, times(1)).process(emptyCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Первая итерация возвращает полный ответ -> переходим ко второй.
     * Вторая итерация возвращает ошибку -> переходим к третьей.
     * Третья итерация возвращает ошибку -> переходим к четвертой.
     * Чертвертая итерация возвращает ошибку -> завершаем цикл (исчерпано кол-во попыток).
     * Все ошибки происходят на этапе acquire.
     */
    @Test
    public void singleFilledThenThreeFailuresDuringAcquireIteration() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3);

        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok()).when(strategyMock).process(filledCollection, SOURCE);

        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquire(PAGE_SIZE, 1, SOURCE);
        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquire(PAGE_SIZE, 2, SOURCE);
        doReturn(ExecutionResult.fail(new Throwable())).when(strategyMock).acquire(PAGE_SIZE, 3, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).process(filledCollection, SOURCE);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 1, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 2, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 3, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }

    /**
     * Первая итерация возвращает полный ответ -> переходим ко второй.
     * Вторая итерация возвращает ошибку -> переходим к третьей.
     * Третья итерация возвращает ошибку -> переходим к четвертой.
     * Чертвертая итерация возвращает ошибку -> завершаем цикл (исчерпано кол-во попыток).
     * Все ошибки происходят на этапе process.
     */
    @Test
    public void singleFilledThenThreeFailuresDuringProcessIteration() {
        List<Integer> filledCollection = ImmutableList.of(1, 2, 3);

        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 0, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 1, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 2, SOURCE);
        doReturn(ExecutionResult.ok(filledCollection)).when(strategyMock).acquire(PAGE_SIZE, 3, SOURCE);

        doReturn(
            ExecutionResult.ok(),
            ExecutionResult.fail(null),
            ExecutionResult.fail(null),
            ExecutionResult.fail(null)
        ).when(strategyMock).process(filledCollection, SOURCE);

        syncService.processPayload(payload);

        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 0, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 1, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 2, SOURCE);
        verify(strategyMock, times(1)).acquire(PAGE_SIZE, 3, SOURCE);
        verify(strategyMock, times(4)).process(filledCollection, SOURCE);

        verifyNoMoreInteractions(strategyMock);
    }
}