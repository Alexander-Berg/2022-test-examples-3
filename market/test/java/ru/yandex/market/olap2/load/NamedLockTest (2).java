package ru.yandex.market.olap2.load;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.olap2.util.NamedLock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NamedLockTest {
    private int counter = 0;
    @Test
    @Ignore
    public void executeInNamedLockTest() throws Exception {
        final int M = 500_000;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        for(int i = 0; i < M; i++) {
            pool.submit(() -> NamedLock.doSynchronized(this.getClass(),"l1", () -> {
                counter++;
            }));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(counter, is(M));
    }
}
