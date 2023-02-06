package ru.yandex.market.deliverycalculator.storage.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.persistence.PessimisticLockException;

import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.invocation.InterceptedInvocation;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.envers.GenerationLockEntity;
import ru.yandex.market.deliverycalculator.storage.repository.GenerationLockRepository;
import ru.yandex.market.deliverycalculator.storage.service.impl.LockerDbService;
import ru.yandex.misc.thread.ThreadUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LockerDbServiceTest extends FunctionalTest {
    @Autowired
    private LockerDbService lockerDbService;

    @Autowired
    private GenerationLockRepository generationLockRepository;


    @BeforeEach
    public void init() {
        lockerDbService.deleteEntity();
    }

    @Test
    void baseTest() {
        lockerDbService.waitAndLock("test1", () -> null);
        lockerDbService.waitAndLock("test2", () -> null);
        lockerDbService.waitAndLock("test3", () -> null);
    }

    private volatile int value = 0;

    @Test
    void threadTestCount() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(3);
        CyclicBarrier barrier = new CyclicBarrier(3);

        Callable<Integer> r = () -> {
            try {
                barrier.await();
                return lockerDbService.waitAndLock("test", () -> {
                    int res = value + 5;
                    value += 2;
                    ThreadUtils.doSleep(100, TimeUnit.MILLISECONDS);
                    value += 3;
                    ThreadUtils.doSleep(100, TimeUnit.MILLISECONDS);
                    assertEquals(res, value);
                    return res;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        List<Future<Integer>> futures = List.of(es.submit(r), es.submit(r), es.submit(r));
        es.shutdown();
        es.awaitTermination(1, TimeUnit.MINUTES);
        int res = 0;
        for (Future<Integer> f : futures) {
            res += f.get();
        }
        assertEquals(value, 15);
        assertEquals(res, 30);
    }


    @Test
    void threadTestMultiThread() throws ExecutionException, InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(6);
        Callable<Boolean> r = () -> {
            try {
                return lockerDbService.waitAndLock("test", () -> {
                    ThreadUtils.doSleep(100, TimeUnit.MILLISECONDS);
                    return true;
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i <= 36; i++) {
            futures.add(es.submit(r));
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.MINUTES);
        for (Future<Boolean> f : futures) {
            assertTrue(f.get());
        }
    }

    @Test
    void testViewData() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(3);
        CyclicBarrier barrier = new CyclicBarrier(2);
        es.submit(() -> {
            lockerDbService.waitAndLock("jobName", () -> {
                try {
                    barrier.await();
                    ThreadUtils.doSleep(10000, TimeUnit.MILLISECONDS);
                } catch (BrokenBarrierException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return null;
            });
        });
        barrier.await();
        Future<Boolean> res = es.submit(() -> {
            Optional<GenerationLockEntity> optionEntity = generationLockRepository.findById(1L);
            assertTrue(optionEntity.isPresent());
            GenerationLockEntity entity = optionEntity.get();
            assertEquals(entity.getJobName(), "jobName");
            return true;
        });
        es.shutdown();
        assertTrue(res.get(1, TimeUnit.SECONDS));
        es.shutdownNow();
    }

    @Test
    void testMockDataSource() throws IllegalAccessException {
        Supplier<?> supplier = Mockito.mock(Supplier.class);
        LockerDbService lockerDbService = new LockerDbService(1000, 1000, 100);
        GenerationLockRepository generationLockRepository = Mockito.mock(GenerationLockRepository.class);
        FieldUtils.writeField(lockerDbService, "generationLockRepository", generationLockRepository, true);
        FieldUtils.writeField(lockerDbService, "lockerDbService", lockerDbService, true);
        lockerDbService.waitAndLock("test1", supplier);
        lockerDbService.waitAndLock("test2", supplier);
        lockerDbService.waitAndLock("test3", supplier);
        verify(supplier, times(3)).get();
    }

    @Test
    void testMockDataSourcePessimisticLockException() throws IllegalAccessException {
        Supplier<?> supplier = Mockito.mock(Supplier.class);
        LockerDbService lockerDbService = new LockerDbService(100000, 1000, 100);
        GenerationLockRepository generationLockRepository = Mockito.mock(GenerationLockRepository.class);
        doAnswer(invocation -> {
            InterceptedInvocation interceptedInvocation = (InterceptedInvocation) invocation;
            if (interceptedInvocation.getSequenceNumber() < 10) {
                throw new PessimisticLockException();
            }
            return null;
        }).when(generationLockRepository).lock();
        FieldUtils.writeField(lockerDbService, "generationLockRepository", generationLockRepository, true);
        FieldUtils.writeField(lockerDbService, "lockerDbService", lockerDbService, true);
        lockerDbService.waitAndLock("test1", supplier);
        verify(supplier, times(1)).get();
    }

    private volatile GenerationLockEntity lockEntity;

    @Test
    void testMockDataSourceOldLock() throws IllegalAccessException {
        Supplier<?> supplier = Mockito.mock(Supplier.class);
        LockerDbService lockerDbService = new LockerDbService(100000, 250 * 1000, 1000);
        GenerationLockRepository generationLockRepository = Mockito.mock(GenerationLockRepository.class);
        lockEntity = new GenerationLockEntity(1L, "test", "test",
                Instant.now().minus(4, ChronoUnit.MINUTES));
        doAnswer(invocation -> Optional.ofNullable(lockEntity)).when(generationLockRepository).findById(anyLong());
        doAnswer(invocation -> lockEntity = invocation.getArgument(0))
                .when(generationLockRepository).save(any(GenerationLockEntity.class));
        doAnswer(invocation -> lockEntity = null).when(generationLockRepository).deleteAllById(anyLong());
        FieldUtils.writeField(lockerDbService, "generationLockRepository", generationLockRepository, true);
        FieldUtils.writeField(lockerDbService, "lockerDbService", lockerDbService, true);
        lockerDbService.waitAndLock("test1", supplier);
        assertNull(lockEntity);
        verify(supplier, times(1)).get();
    }
}
