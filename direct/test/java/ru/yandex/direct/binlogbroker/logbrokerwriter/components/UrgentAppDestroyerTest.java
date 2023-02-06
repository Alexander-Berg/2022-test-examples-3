package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.misc.sunMiscSignal.SunMiscSignal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ParametersAreNonnullByDefault
public class UrgentAppDestroyerTest {
    @Rule
    public Timeout timeout = new org.junit.rules.Timeout(10, TimeUnit.SECONDS);

    @Test
    public void itWorks() {
        UrgentAppDestroyer urgentAppDestroyer = new UrgentAppDestroyer();
        Exception exception = new Exception("oops lol");
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                latch.await();
                urgentAppDestroyer.destroy(exception);
            } catch (InterruptedException exc) {
                urgentAppDestroyer.destroy(exc);
            }
        });

        latch.countDown();
        assertThatCode(urgentAppDestroyer::run).hasCause(exception);
    }

    @Test
    public void testTermSignal() {
        UrgentAppDestroyer urgentAppDestroyer = new UrgentAppDestroyer();
        SunMiscSignal.raise("TERM");

        assertThatCode(urgentAppDestroyer::run)
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("TERM");
    }

    @Test
    public void testHasError() {
        UrgentAppDestroyer urgentAppDestroyer = new UrgentAppDestroyer();
        assertThat(urgentAppDestroyer.hasError()).isFalse();
        var testException = new RuntimeException("test");
        urgentAppDestroyer.destroy(testException);
        assertThat(urgentAppDestroyer.hasError()).isTrue();
        assertThat(urgentAppDestroyer.hasError()).isTrue();
    }

    @Test
    public void testGetError() {
        UrgentAppDestroyer urgentAppDestroyer = new UrgentAppDestroyer();
        assertThat(urgentAppDestroyer.getError()).isNull();
        var testException = new RuntimeException("test");
        urgentAppDestroyer.destroy(testException);
        assertThat(urgentAppDestroyer.getError()).isEqualTo(testException);
        assertThat(urgentAppDestroyer.getError()).isEqualTo(testException);
    }

    @Test
    public void testThrowsErrorAfterGetError() {
        UrgentAppDestroyer urgentAppDestroyer = new UrgentAppDestroyer();
        var testException = new RuntimeException("test");
        urgentAppDestroyer.destroy(testException);
        assertThat(urgentAppDestroyer.getError()).isEqualTo(testException);
        assertThatCode(urgentAppDestroyer::run)
                .hasCause(testException);
    }
}
