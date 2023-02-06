package ru.yandex.direct.tracing.util;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MockedThreadUsedResourcesProvider extends ThreadUsedResourcesProvider {
    private final Map<Thread, Long> times = new HashMap<>();
    private Duration advance;

    public MockedThreadUsedResourcesProvider(Duration advance) {
        this.advance = advance;
    }

    public ThreadUsedResources getCurrentThreadCpuTime() {
        return getThreadCpuTime(Thread.currentThread());
    }

    public ThreadUsedResources getThreadCpuTime(Thread thread) {
        Long cur = times.getOrDefault(thread, 0L);
        times.put(thread, cur + advance.toNanos());
        return new ThreadUsedResources(cur, 3 * cur, 2 * cur);
    }
}
