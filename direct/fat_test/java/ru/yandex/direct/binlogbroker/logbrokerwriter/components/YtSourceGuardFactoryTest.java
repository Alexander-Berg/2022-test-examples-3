package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.direct.binlogbroker.logbroker_utils.models.SourceType;
import ru.yandex.direct.utils.Completer;

import static ru.yandex.direct.env.EnvironmentType.DEVTEST;

@ParametersAreNonnullByDefault
public class YtSourceGuardFactoryTest {
    @Rule
    public JunitLocalYt localYt = new JunitLocalYt();
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Rule
    public Timeout timeout = new Timeout(2, TimeUnit.MINUTES);

    @Test
    public void lockingLikeDifferentProcesses() throws InterruptedException {
        int threads = 5;
        int iterations = 200;
        SourceType source = SourceType.fromType(DEVTEST, "ppc:1");
        AtomicInteger acquires = new AtomicInteger();
        AtomicInteger attemptsToAcquire = new AtomicInteger();
        CyclicBarrier barrier = new CyclicBarrier(threads);
        Completer.Builder completerBuilder = new Completer.Builder(Duration.ofSeconds(5));

        for (int threadId = 0; threadId < threads; ++threadId) {
            completerBuilder.submitVoid("worker-" + threadId, () -> {
                for (int i = 0; i < iterations; ++i) {
                    YtSourceGuardFactory ytSourceGuardFactory = new YtSourceGuardFactory(
                            localYt.getYt(), localYt.getTestPath(), new UrgentAppDestroyer());
                    try {
                        ytSourceGuardFactory.init();
                        barrier.await();
                        attemptsToAcquire.incrementAndGet();
                        Optional<SourceGuard> guard = ytSourceGuardFactory.guard(source);
                        if (guard.isPresent()) {
                            acquires.incrementAndGet();
                        }
                        barrier.await();
                    } finally {
                        ytSourceGuardFactory.destroy();
                    }
                }
            });
        }
        try (Completer completer = completerBuilder.build()) {
            completer.waitAll();
        }

        softly.assertThat(acquires.get())
                .describedAs("Acquire count")
                .isEqualTo(iterations);
        softly.assertThat(attemptsToAcquire.get())
                .describedAs("Attempts to acquire")
                .isEqualTo(iterations * threads);
    }

    @Test
    public void lockingSeveralSourcesInOneProcess() throws InterruptedException {
        int iterations = 200;
        int threadsForSource = 2;
        List<SourceType> sources = ImmutableList.of(
                SourceType.fromType(DEVTEST, "ppc:1"),
                SourceType.fromType(DEVTEST, "ppc:2"),
                SourceType.fromType(DEVTEST, "ppc:3"));
        AtomicIntegerArray attemptsToAcquireBySource = new AtomicIntegerArray(sources.size());
        AtomicIntegerArray acquiresBySource = new AtomicIntegerArray(sources.size());
        CyclicBarrier barrier = new CyclicBarrier(threadsForSource * sources.size());
        Completer.Builder completerBuilder = new Completer.Builder(Duration.ofSeconds(5));

        YtSourceGuardFactory ytSourceGuardFactory = new YtSourceGuardFactory(
                localYt.getYt(), localYt.getTestPath(), new UrgentAppDestroyer());
        for (int sourceIdMutable = 0; sourceIdMutable < sources.size(); ++sourceIdMutable) {
            final int sourceId = sourceIdMutable;
            SourceType source = sources.get(sourceId);
            for (int threadForSourceId = 0; threadForSourceId < threadsForSource; ++threadForSourceId) {
                completerBuilder.submitVoid(
                        String.format("worker-%s-%d", source.getSourceName(), threadForSourceId),
                        () -> {
                            for (int i = 0; i < iterations; ++i) {
                                barrier.await();
                                attemptsToAcquireBySource.incrementAndGet(sourceId);
                                Optional<SourceGuard> guard = ytSourceGuardFactory.guard(source);
                                if (guard.isPresent()) {
                                    acquiresBySource.incrementAndGet(sourceId);
                                }
                                barrier.await();
                                if (guard.isPresent()) {
                                    guard.get().close();
                                }
                            }
                        });
            }
        }
        try {
            ytSourceGuardFactory.init();
            try (Completer completer = completerBuilder.build()) {
                completer.waitAll();
            }
        } finally {
            ytSourceGuardFactory.destroy();
        }

        softly.assertThat(attemptsToAcquireBySource)
                .describedAs("Attempts to acquire")
                .containsExactly(
                        IntStream.range(0, sources.size()).map(ignored -> iterations * threadsForSource).toArray());
        softly.assertThat(acquiresBySource)
                .describedAs("Acquire count")
                .containsExactly(
                        IntStream.range(0, sources.size()).map(ignored -> iterations).toArray());
    }
}
