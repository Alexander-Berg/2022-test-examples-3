package ru.yandex.direct.hourglass.implementations;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThreadsHierarchyTest {

    @Test
    void threadPropertiesTest() {
        var threadsHierarchy = new ThreadsHierarchy();

        var threadFactory = threadsHierarchy.getThreadFactory("groupName", "name_prefix", Thread.MIN_PRIORITY);
        var thread = threadFactory.newThread(() -> {
        });
        assertThat(thread.getName()).isEqualTo("name_prefix");
        assertThat(thread.getPriority()).isEqualTo(Thread.MIN_PRIORITY);
        assertThat(thread.getThreadGroup().getName()).isEqualTo("groupName");
        assertThat(thread.isDaemon()).isEqualTo(false);
    }

    @Test
    void threadPropertiesAndThreadNameFactoryTest() {
        var threadsHierarchy = new ThreadsHierarchy();

        var threadFactory = threadsHierarchy.getThreadFactory("groupName", threadsHierarchy.numberNameFactory(
                "name_prefix_%d"), Thread.MAX_PRIORITY);
        for (int i = 1; i < 3; i++) {
            var thread = threadFactory.newThread(() -> {
            });
            assertThat(thread.getName()).isEqualTo("name_prefix_" + i);
            assertThat(thread.getPriority()).isEqualTo(Thread.MAX_PRIORITY);
            assertThat(thread.getThreadGroup().getName()).isEqualTo("groupName");
            assertThat(thread.isDaemon()).isEqualTo(false);
        }
    }

    /**
     * Тест проверяет, что если указать несколько обраотчиков непойманных исклчений, они все вызовутся
     */
    @Test
    void uncaughtExceptionHandlerTest() throws InterruptedException {
        var threadsHierarchy = new ThreadsHierarchy();
        var isException1 = new AtomicBoolean();
        var isException2 = new AtomicBoolean();
        threadsHierarchy.addUncaughtExceptionHandler((t, e) -> {
            isException1.set(true);
        });
        threadsHierarchy.addUncaughtExceptionHandler((t, e) -> isException2.set(true));

        var threadFactory = threadsHierarchy.getThreadFactory("groupName", "name_prefix", Thread.MIN_PRIORITY);
        var thread = threadFactory.newThread(() -> {
            throw new RuntimeException("exception");

        });
        thread.start();
        thread.join();
        assertThat(isException1).isTrue();
        assertThat(isException2).isTrue();
    }

    @Test
    void systemThreadFactoryTest() {
        var threadsHierarchy = new ThreadsHierarchy();

        var threadFactory = threadsHierarchy.getSystemThreadFactory();
        var thread = threadFactory.newThread(() -> {
        });
        assertThat(thread.getName()).isEqualTo("hourglass-system-thread-");
        assertThat(thread.getPriority()).isEqualTo(Thread.MAX_PRIORITY);
        assertThat(thread.getThreadGroup().getName()).isEqualTo("hourglass-system-threads");
        assertThat(thread.isDaemon()).isEqualTo(false);
    }

    @Test
    void workerThreadFactoryTest() {
        var threadsHierarchy = new ThreadsHierarchy();

        var threadFactory = threadsHierarchy.getWorkersThreadFactory();
        for (int i = 1; i < 3; i++) {
            var thread = threadFactory.newThread(() -> {
            });
            assertThat(thread.getName()).isEqualTo("hourglass-worker-" + i);
            assertThat(thread.getPriority()).isEqualTo(Thread.NORM_PRIORITY);
            assertThat(thread.getThreadGroup().getName()).isEqualTo("hourglass-workers");
            assertThat(thread.isDaemon()).isEqualTo(false);
        }
    }
}
