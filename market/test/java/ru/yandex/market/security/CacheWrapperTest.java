package ru.yandex.market.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.functional.Function;
import ru.yandex.market.security.util.cache.Cache;
import ru.yandex.market.security.util.cache.CacheWrapper;
import ru.yandex.market.security.util.cache.TimeLimitedCache;
import ru.yandex.market.security.util.cache.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmitry Tsyganov dtsyganov@yandex-team.ru
 */
class CacheWrapperTest {

    private List<String> serviceInvocations;
    private Function<String, String> service = new Function<String, String>() {
        @Override
        public String apply(final String arg) {
            serviceInvocations.add(arg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
            return arg;
        }
    };

    @Test
    void testOldVersion() throws Exception {
        serviceInvocations = Collections.synchronizedList(new ArrayList<>());
        final Cache<String> cache = new TimeLimitedCache<>();
        final Runnable command = () -> {
            String result = cache.get("a");
            if (result == null) {
                result = service.apply("a");
                cache.put("a", result);
            }
        };
        concurrentExecuteRunnables(command, command);
        assertEquals(2, serviceInvocations.size());
    }

    @Test
    void testNewVersion() throws Exception {
        serviceInvocations = Collections.synchronizedList(new ArrayList<>());
        final ConcurrentMap<String, Value<String>> currentRequests = new ConcurrentHashMap<>();
        final Cache<String> cache = new TimeLimitedCache<>();
        final Runnable command = () -> {
            final Value<String> newValue = new Value<String>() {
                @Override
                public synchronized String getValue() {
                    String result = cache.get("a");
                    if (result == null) {
                        result = service.apply("a");
                        cache.put("a", result);
                    }
                    return result;
                }
            };
            final Value<String> prevValue = currentRequests.putIfAbsent("a", newValue);
            (prevValue != null ? prevValue : newValue).getValue();
        };
        concurrentExecuteRunnables(command, command);
        assertEquals(1, serviceInvocations.size());
    }

    @Test
    void testCacheWrapper() throws Exception {
        serviceInvocations = Collections.synchronizedList(new ArrayList<>());
        final CacheWrapper<String> cacheWrapper = new CacheWrapper<>(new TimeLimitedCache<>());
        final Runnable commandA = () -> cacheWrapper.get("a", () -> service.apply("a")).hashCode();
        final Runnable commandB = () -> cacheWrapper.get("b", () -> service.apply("b")).hashCode();
        concurrentExecuteRunnables(commandA, commandA, commandB, commandB, commandB);
        assertEquals(2, serviceInvocations.size());
    }

    private void concurrentExecuteRunnables(final Runnable... commands) throws InterruptedException {
        final ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(commands.length);
        final ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(commands.length, commands.length, 1, TimeUnit.MINUTES, blockingQueue);
        for (final Runnable command : commands) {
            threadPoolExecutor.execute(command);
        }
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(1, TimeUnit.HOURS);
    }
}
