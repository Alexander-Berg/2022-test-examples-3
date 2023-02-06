package ru.yandex.market.jmf.metainfo.test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ru.yandex.market.jmf.metainfo.MetaInfoInitializer;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageValueChange;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.metainfo.ReloadType;
import ru.yandex.market.jmf.metainfo.impl.EmptyMetaInfoInitializer;
import ru.yandex.market.jmf.metainfo.impl.MetaInfoServiceImpl;
import ru.yandex.market.jmf.tx.AfterCompletionSynchronization;
import ru.yandex.market.jmf.tx.TxService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MetaInfoTestConfiguration.class)
public class MetaInfoServiceImplTest {
    @Inject
    private TxService txService;

    private MetaInfoServiceImpl metaInfoService;
    private AutoCloseable autoCloseable;
    private Object simpleData;
    private MetaInfoInitializer<AutoCloseable> autoCloseableInitializer;

    @BeforeEach
    public void setUp() {
        autoCloseableInitializer = mock(MetaInfoInitializer.class);
        MetaInfoInitializer<Object> simpleDataInitializer = mock(MetaInfoInitializer.class);

        autoCloseable = mock(AutoCloseable.class);
        simpleData = mock(Object.class);

        when(autoCloseableInitializer.build(any(MetaInfoService.class), any(ReloadType.class), any())).thenReturn(autoCloseable,
                mock(AutoCloseable.class));
        when(autoCloseableInitializer.type()).thenReturn(AutoCloseable.class);
        when(autoCloseableInitializer.dependsOn()).thenReturn(Collections.singleton(EmptyMetaInfoInitializer.EmptyData.class));
        when(autoCloseableInitializer.key()).thenReturn("autoClosable");

        when(simpleDataInitializer.build(any(MetaInfoService.class), any(ReloadType.class), any())).thenReturn(simpleData);
        when(simpleDataInitializer.type()).thenReturn(Object.class);
        when(simpleDataInitializer.dependsOn()).thenReturn(Collections.singleton(EmptyMetaInfoInitializer.EmptyData.class));
        when(simpleDataInitializer.key()).thenReturn("simple");

        List<MetaInfoInitializer<?>> initializers = List.of(new EmptyMetaInfoInitializer(),
                simpleDataInitializer,
                autoCloseableInitializer);
        metaInfoService = new MetaInfoServiceImpl(txService, initializers);
    }

    /**
     * Тест на то, что после перезагрузки метаинформации элементы метаданных сразу же закрываются
     *
     * @throws Exception
     */
    @Test
    public void testClosesAutoClosableDataOnReload() throws Exception {
        txService.runInNewTx(() -> metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION));

        verifyNoMoreInteractions(autoCloseable);
        verifyNoMoreInteractions(simpleData);

        txService.runInNewTx(() -> metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION));

        verify(autoCloseable, times(1)).close();
        verifyNoMoreInteractions(simpleData);
    }

    /**
     * Тест проверяет, что metadata не очишается до тех пор, пока есть хотя бы одна транзакция, которая его использует
     */
    @Test
    public void testDoNotClosesAutoClosableDataIfAnyTransactionUsesThatData() throws Exception {
        testParallelTransactionEndWithReload(true);
    }

    /**
     * Тест проверяет, что части метаинформации не закрываются, если после перезагрузки они без изменений перешли в
     * новую метадату
     */
    @Test
    public void testDoNotClosesAutoCloseableIfItIsUsedByNextGenerationOfMetadata() throws Exception {
        when(autoCloseableInitializer.build(any(), any(), any())).thenReturn(autoCloseable);

        testParallelTransactionEndWithReload(false);
    }

    @Test
    public void testReloadTimeOutDoesNotLeadToUseOfOutdatedMetainfo() throws Exception {
        txService.runInNewTx(() -> metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION));

        verifyNoMoreInteractions(autoCloseable);

        when(autoCloseableInitializer.build(any(), any(), any())).then(inv -> {
            Thread.sleep(1100);
            return mock(AutoCloseable.class);
        });

        CompletableFuture.allOf(
                CompletableFuture.runAsync(this::reloadMetaInfo),
                CompletableFuture.runAsync(this::reloadMetaInfo)
        ).get();

        final var autoCloseable = metaInfoService.get(AutoCloseable.class);
        verifyNoMoreInteractions(autoCloseable);
        verifyNoMoreInteractions(this.autoCloseable);
        Assertions.assertEquals(this.autoCloseable, autoCloseable);
    }

    private void reloadMetaInfo() {
        try {
            txService.runInNewTx(Duration.ofSeconds(1),
                    () -> metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION));
        } catch (Exception ignored) {
        }
    }

    @Test
    @Transactional
    public void testReloadPendingChangesBeforeGettingData() {
        metaInfoService.handleMetaInfoStorageValueChange(new MetaInfoStorageValueChange(
                null, null, null, null, null, null, null,
                autoCloseableInitializer.key()
        ));

        verify(autoCloseableInitializer, never())
                .build(eq(metaInfoService), eq(ReloadType.INSTANCE_MODIFICATION), any(ReloadAttributes.class));

        metaInfoService.get(AutoCloseable.class);

        verify(autoCloseableInitializer, times(1))
                .build(eq(metaInfoService), eq(ReloadType.INSTANCE_MODIFICATION), any(ReloadAttributes.class));
    }

    @Test
    @Transactional
    public void testDoNotReloadPendingChangesBeforeCommit() {
        metaInfoService.handleMetaInfoStorageValueChange(new MetaInfoStorageValueChange(
                null, null, null, null, null, null, null,
                autoCloseableInitializer.key()
        ));

        verify(autoCloseableInitializer, never()).build(metaInfoService, ReloadType.INSTANCE_MODIFICATION,
                ReloadAttributes.EMPTY);
    }

    @Test
    public void testReloadPendingChangesAfterCommit() {
        txService.runInNewTx(() -> {
            metaInfoService.handleMetaInfoStorageValueChange(new MetaInfoStorageValueChange(
                    null, null, null, null, null, null, null,
                    autoCloseableInitializer.key()
            ));

            verify(autoCloseableInitializer, never())
                    .build(eq(metaInfoService), eq(ReloadType.INSTANCE_MODIFICATION), any(ReloadAttributes.class));
        });

        verify(autoCloseableInitializer, times(1))
                .build(eq(metaInfoService), eq(ReloadType.INSTANCE_MODIFICATION), any(ReloadAttributes.class));
    }

    private void testParallelTransactionEndWithReload(boolean shouldBeClosed) throws Exception {
        // Загружаем метаинформацию
        txService.runInNewTx(() -> metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION));

        // Лок для ожидания окончания перезагрузки метаинформации
        Lock waitReloadCompletionLock = new ReentrantLock();
        // Лок ожидания окончания транзакции, которая использует метаинформацию
        Lock waitTransactionEndLock = new ReentrantLock();
        // Лок ожидания прикрепления метаинформации к транзакции
        Lock waitDataAcquiredLock = new ReentrantLock();
        waitDataAcquiredLock.lock();
        final var waitDataAcquiredCondition = waitDataAcquiredLock.newCondition();

        // Забираем лок до окончания перезагрузки метаинформации
        waitReloadCompletionLock.lock();

        // Поток, в котором будет использоваться метаинформация
        Thread dataUsageThread = new Thread(() -> {
            // Забираем локи до наступления соответствующих событий
            waitDataAcquiredLock.lock();
            waitDataAcquiredCondition.signal();
            waitTransactionEndLock.lock();
            txService.runInNewTx(() -> {
                metaInfoService.get(Object.class);
                waitDataAcquiredLock.unlock();
                // Ожидаем окончания перезагрузки метаинформации
                waitReloadCompletionLock.lock();
                TransactionSynchronizationManager.registerSynchronization((AfterCompletionSynchronization) status -> {
                    if (status != TransactionSynchronization.STATUS_UNKNOWN) {
                        waitTransactionEndLock.unlock();
                    }
                });
            });
        });
        dataUsageThread.setPriority(Thread.MAX_PRIORITY);
        dataUsageThread.start();

        // Засыпаем для того, чтобы передать управление dataUsageThread
        Thread.sleep(1);

        // Лочимся до момента, когда метаинформация в потоке dataUsageThread гарантированно прикреплена к транзакции
        waitDataAcquiredCondition.await();

        txService.doInNewTx(() -> {
            metaInfoService.reload(ReloadType.INSTANCE_MODIFICATION);
            return null;
        });

        verifyNoMoreInteractions(autoCloseable);
        verifyNoMoreInteractions(simpleData);

        // Отпускаем лок для индикации того, что метаинфа перезагрузилась и начинаем ожидать окончания транзакции в
        // потоке dataUsageThread
        waitReloadCompletionLock.unlock();
        waitTransactionEndLock.lock();

        if (shouldBeClosed) {
            verify(autoCloseable, times(1)).close();
        } else {
            verifyNoMoreInteractions(autoCloseable);
        }
        verifyNoMoreInteractions(simpleData);
    }
}
