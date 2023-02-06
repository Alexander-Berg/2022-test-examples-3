package ru.yandex.market.marketpromo.core.scheduling.lock;

import java.time.Duration;
import java.time.LocalDateTime;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class YdbLockProviderTest extends ServiceTestBase {

    @Autowired
    private LockProvider lockProvider;
    private LockConfiguration lockConfiguration;

    @BeforeEach
    void configure() {
        lockConfiguration = new LockConfiguration(clock.instant(), "dummy task",
                Duration.ofSeconds(6),
                Duration.ofSeconds(3));
    }

    @Test
    void shouldLockByConfig() {
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(true));
    }

    @Test
    void shouldNotBeLockedTwice() {
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(true));
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(false));
    }

    @Test
    void shouldLockAtLeast3Seconds() {
        LocalDateTime now = clock.dateTime();
        var lockOptional = lockProvider.lock(lockConfiguration);
        assertThat(lockOptional.isPresent(), is(true));
        lockOptional.orElseThrow().unlock();
        clock.setFixed(now.plusSeconds(2));
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(false));
    }

    @Test
    void shouldUnlockAfter3Seconds() throws InterruptedException {
        var lockOptional = lockProvider.lock(lockConfiguration);
        assertThat(lockOptional.isPresent(), is(true));
        lockOptional.orElseThrow().unlock();
        Thread.sleep(3500);
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(true));
    }

    @Test
    void shouldLockAtMost6Seconds() {
        LocalDateTime now = clock.dateTime();
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(true));
        clock.setFixed(now.plusSeconds(7));
        assertThat(lockProvider.lock(lockConfiguration).isPresent(), is(true));
    }
}
