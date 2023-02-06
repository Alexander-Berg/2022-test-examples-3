package ru.yandex.travel.orders.services;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.misc.ExceptionUtils;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
@ActiveProfiles("test")
public class DeduplicationServiceTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DeduplicationService deduplicationService;

    @Test
    public void testKey() {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        UUID callId = UUID.randomUUID();

        CountDownLatch firstFinished = new CountDownLatch(1);
        CountDownLatch proceedFirst = new CountDownLatch(1);
        CountDownLatch startBoth = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(2);

        AtomicBoolean secondTxIllegalArgument = new AtomicBoolean(false);

        CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(() -> transactionTemplate.execute(ignored -> {
            boolean result = false;
            try {
                startBoth.await();
                proceedFirst.await();
                deduplicationService.registerAtMostOnceCall(callId);
                firstFinished.countDown();
                result = true;
            } catch (InterruptedException e) {
                throw ExceptionUtils.throwException(e);
            }
            return result;
        }), executor);

        CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(() -> transactionTemplate.execute(ignored -> {
            boolean result = false;
            try {
                startBoth.await();
                try {
                    proceedFirst.countDown();
                    firstFinished.await();
                    deduplicationService.registerAtMostOnceCall(callId);
                    result = true;
                } catch (IllegalStateException e) {
                    secondTxIllegalArgument.set(true);
                }
            } catch (InterruptedException e) {
                throw ExceptionUtils.throwException(e);
            }
            return result;
        }), executor);


        startBoth.countDown();

        CompletableFuture.allOf(first, second).join();

        Assertions.assertThat(first.join()).isTrue();
        Assertions.assertThat(second.join()).isFalse();
        Assertions.assertThat(secondTxIllegalArgument.get()).isTrue();

        MoreExecutors.shutdownAndAwaitTermination(executor, 3, TimeUnit.SECONDS);
    }
}
