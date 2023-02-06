package ru.yandex.travel.api.services.avia.variants;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.api.services.avia.references.AviaGeobaseCountryService;
import ru.yandex.travel.api.services.avia.variants.model.AviaCachedVariantCheck;
import ru.yandex.travel.api.services.avia.variants.model.AviaVariantAvailabilityCheck;
import ru.yandex.travel.api.services.avia.variants.repositories.AviaCachedVariantRepository;
import ru.yandex.travel.api.services.avia.variants.repositories.AviaVariantRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = {"avia-booking.enabled=true"})
@ActiveProfiles("test")
@Slf4j
public class AviaVariantsCacheServiceTest {
    @Autowired
    AviaVariantCacheService cacheService;
    @SpyBean
    AviaCachedVariantRepository cachedVariantRepositoryTxProxy;
    AviaCachedVariantRepository cachedVariantRepository;
    @Autowired
    AviaVariantRepository variantAvailabilityCheckRepository;
    @Autowired
    TransactionTemplate txTemplate;
    @MockBean
    AviaGeobaseCountryService geobaseCountryService;

    @Before
    public void init() {
        // there is a bug that doesn't let us simply SpyBean AOP-proxied beans (e.g. @Transactional):
        // https://github.com/spring-projects/spring-boot/issues/7033
        // we have to unwrap the spied bean ourselves as well as reset it between tests
        cachedVariantRepository = AopTestUtils.getUltimateTargetObject(cachedVariantRepositoryTxProxy);
        Mockito.reset(cachedVariantRepository);
        Mockito.clearInvocations(cachedVariantRepository);
    }

    @Test
    public void testFindAndLockSerial() {
        log.info("t1 starts a new availability check");
        AviaCachedVariantCheck row1 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v1"));
        log.info("t1 finished a new availability check");
        assertThat(row1).isNotNull();

        log.info("t2 starts the same sequential availability check");
        AviaCachedVariantCheck row2 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v1"));
        log.info("t2 finished the same sequential availability check");
        Assertions.assertThat(row2).isEqualTo(row1);

        // 2 selectForUpdate-s and only 1 insert
        verify(cachedVariantRepository, times(2)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(1)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());
    }

    @Test
    public void testFindAndLockSimultaneousLock() throws Exception {
        log.info("t0 creates a new cache record");
        AviaCachedVariantCheck row0 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v2"));
        log.info("t0 finished a new cache record creation");
        verify(cachedVariantRepository, times(1)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(1)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());

        AtomicInteger lockCallNum = new AtomicInteger(0);
        CompletableFuture<Void> t1Started = new CompletableFuture<>();
        CompletableFuture<Void> t2Completed = new CompletableFuture<>();
        doAnswer(invocation -> {
            Object res;
            switch (lockCallNum.incrementAndGet()) {
                case 1:
                    // t1 locks the records until t2 fails on the same lock acquisition
                    log.info("Executing selectForUpdate #1");
                    res = invocation.callRealMethod();
                    t1Started.complete(null);
                    // imitating t1 in-progress state until t2 has finished
                    t2Completed.get(15, TimeUnit.SECONDS);
                    log.info("Finished selectForUpdate #1");
                    return res;
                case 2:
                    try {
                        log.info("Executing selectForUpdate #2");
                        return invocation.callRealMethod();
                    } finally {
                        log.info("Finished selectForUpdate #2");
                        // t2 has completed, telling t1 to continue
                        t2Completed.complete(null);
                    }
                default:
                    throw new IllegalStateException("Exactly 2 selectForUpdate calls are expected");
            }
        }).when(cachedVariantRepository).selectForUpdateNoWait(any(), any());

        CompletionService<Void> cs = new ExecutorCompletionService<>(Executors.newCachedThreadPool());
        cs.submit(() -> {
            log.info("t1 starts locking the existing cache record");
            AviaCachedVariantCheck row1 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v2"));
            log.info("t1 successfully locked the existing cache record");
            Assertions.assertThat(row1).isEqualTo(row0);
            return null;
        });
        cs.submit(() -> {
            // waiting until t1 reaches the in-progress state
            t1Started.get(5, TimeUnit.SECONDS);
            log.info("t2 starts locking the existing cache record");
            assertThatThrownBy(() -> callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v2")))
                    .isExactlyInstanceOf(AviaVariantCacheRecordLockException.class)
                    .hasMessageContaining("failed to lock the existing record");
            log.info("t2 failed to lock the existing cache record");
            return null;
        });

        completeAll(cs, 2, Duration.ofSeconds(15));

        verify(cachedVariantRepository, times(3)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(1)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());
    }

    @Test
    public void testFindAndLockSimultaneousLockOfDifferentRecords() throws Exception {
        log.info("t0 creates new cache records");
        AviaCachedVariantCheck row01 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v31"));
        AviaCachedVariantCheck row02 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v32"));
        log.info("t0 finished new cache records creation");
        verify(cachedVariantRepository, times(2)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(2)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());

        AtomicInteger lockCallNum = new AtomicInteger(0);
        CompletableFuture<Void> t1Started = new CompletableFuture<>();
        CompletableFuture<Void> t2Completed = new CompletableFuture<>();
        doAnswer(invocation -> {
            Object res;
            switch (lockCallNum.incrementAndGet()) {
                case 1:
                    // t1 locks the records until t2 does the same
                    log.info("Executing selectForUpdate #1");
                    res = invocation.callRealMethod();
                    t1Started.complete(null);
                    // imitating t1 in-progress state until t2 has finished
                    t2Completed.get(5, TimeUnit.SECONDS);
                    log.info("Finished selectForUpdate #1");
                    return res;
                case 2:
                    // t2 locks it own record in parallel with t1
                    log.info("Executing selectForUpdate #2");
                    res = invocation.callRealMethod();
                    // t2 has completed, telling t1 to continue
                    t2Completed.complete(null);
                    log.info("Finished selectForUpdate #2");
                    return res;
                default:
                    throw new IllegalStateException("Exactly 2 selectForUpdate calls are expected");
            }
        }).when(cachedVariantRepository).selectForUpdateNoWait(any(), any());

        CompletionService<Void> cs = new ExecutorCompletionService<>(Executors.newCachedThreadPool());
        cs.submit(() -> {
            log.info("t1 starts locking the existing cache record");
            AviaCachedVariantCheck row1 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v31"));
            log.info("t1 successfully locked the existing cache record");
            Assertions.assertThat(row1).isEqualTo(row01);
            return null;
        });
        cs.submit(() -> {
            // waiting until t1 reaches the in-progress state
            t1Started.get(5, TimeUnit.SECONDS);
            log.info("t2 starts locking the existing cache record");
            AviaCachedVariantCheck row1 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v32"));
            log.info("t2 successfully locked the existing cache record");
            Assertions.assertThat(row1).isEqualTo(row02);
            return null;
        });

        completeAll(cs, 2, Duration.ofSeconds(5));

        verify(cachedVariantRepository, times(4)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(2)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());
    }

    @Test
    public void testFindAndLockSimultaneousMissingLockAndCreate() throws Exception {
        AtomicInteger createCallNum = new AtomicInteger(0);
        CompletableFuture<Void> t1Started = new CompletableFuture<>();
        CompletableFuture<Void> t2Completed = new CompletableFuture<>();
        // both threads skip locking the not existing cache record and try to create a new one
        doAnswer(invocation -> {
            Object res;
            switch (createCallNum.incrementAndGet()) {
                case 1:
                    // t1 selects nothing
                    log.info("Executing create #1");
                    res = invocation.callRealMethod();
                    t1Started.complete(null);
                    // imitating t1 in-progress state until t2 has finished
                    t2Completed.get(5, TimeUnit.SECONDS);
                    log.info("Finished create #1");
                    return res;
                case 2:
                    try {
                        log.info("Executing create #2");
                        return invocation.callRealMethod();
                    } finally {
                        log.info("Finished create #2");
                        // t2 has completed, telling t1 to continue
                        t2Completed.complete(null);
                    }
                default:
                    throw new IllegalStateException("Exactly 2 create calls are expected");
            }
        }).when(cachedVariantRepository).insert(any());

        CompletionService<Void> cs = new ExecutorCompletionService<>(Executors.newCachedThreadPool());
        cs.submit(() -> {
            log.info("t1 starts locking the existing cache record");
            callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v4"));
            log.info("t1 successfully locked the existing cache record");
            return null;
        });
        cs.submit(() -> {
            // waiting until t1 reaches the in-progress state
            t1Started.get(5, TimeUnit.SECONDS);
            log.info("t2 starts locking the existing cache record");
            assertThatThrownBy(() -> callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v4")))
                    .isExactlyInstanceOf(AviaVariantCacheRecordLockException.class)
                    .hasMessageContaining("failed to create a unique cache record");
            log.info("t2 failed to lock the existing cache record");
            return null;
        });

        completeAll(cs, 2, Duration.ofSeconds(5));

        verify(cachedVariantRepository, times(2)).selectForUpdateNoWait(any(), any());
        verify(cachedVariantRepository, times(2)).insert(any());
        verify(cachedVariantRepository, times(0)).update(any());
    }

    @Test
    public void testExpiration() {
        UUID orderItemId = UUID.randomUUID();
        runInTx(() -> {
            variantAvailabilityCheckRepository.insert(AviaVariantAvailabilityCheck.builder()
                    .id(orderItemId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build());
        });

        AviaCachedVariantCheck row1 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v5"));
        Assertions.assertThat(row1).isNotNull();
        Assertions.assertThat(row1.getExpiresAt()).isNotNull();

        runInTx(() -> {
            row1.setCheckId(orderItemId);
            cachedVariantRepository.update(row1);
        });

        AviaCachedVariantCheck row2 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v5"));
        Assertions.assertThat(row2.getCheckId()).isNotNull();
        Assertions.assertThat(row2.getExpiresAt()).isNotNull();

        runInTx(() -> {
            row2.setExpiresAt(Instant.now().minus(Duration.ofMinutes(5)));
            cachedVariantRepository.update(row2);
        });

        AviaCachedVariantCheck row3 = callInTx(() -> cacheService.findOrCreateCachedCheck("p1", "v5"));
        Assertions.assertThat(row3.getCheckId()).isNull();
        Assertions.assertThat(row3.getExpiresAt()).isNull();
    }

    private void runInTx(Runnable action) {
        callInTx(() -> {
            action.run();
            return null;
        });
    }

    private <T> T callInTx(Callable<T> action) {
        try {
            return txTemplate.execute(txStatus -> {
                try {
                    return action.call();
                } catch (Exception e) {
                    throw ExceptionUtils.throwException(e);
                }
            });
        } catch (TransactionSystemException e) {
            throw ExceptionUtils.throwException(e.getApplicationException());
        }
    }

    private void completeAll(CompletionService cs, int n, Duration timeout) throws InterruptedException, TimeoutException {
        long stopTs = System.currentTimeMillis() + timeout.toMillis();
        while (n-- > 0) {
            long leftMs = stopTs - System.currentTimeMillis();
            Future f;
            if (leftMs <= 0 || (f = cs.poll(leftMs, TimeUnit.MILLISECONDS)) == null) {
                throw new TimeoutException("Failed to complete all tasks in time");
            }
            // checking exceptions
            try {
                f.get(1, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                throw ExceptionUtils.throwException(e.getCause());
            }
        }
    }
}
