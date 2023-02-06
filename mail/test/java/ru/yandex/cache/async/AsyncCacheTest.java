package ru.yandex.cache.async;

import java.util.concurrent.CountDownLatch;

import org.apache.http.concurrent.FutureCallback;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.BasicFuture;
import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.test.util.TestBase;

public class AsyncCacheTest extends TestBase {
    public AsyncCacheTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        try (ConcurrentLinkedHashMapAsyncStorage<String, String> storage =
                new ConcurrentLinkedHashMapAsyncStorage<>(
                    512L,
                    2,
                    key -> key.length() * 2 + 48,
                    value -> value.length() * 2 + 48);
            AsyncCache<String, String, String, RuntimeException> cache =
                new AsyncCache<>(
                    storage,
                    new ConstAsyncCacheTtlCalculator(3600000),
                    (key, context, callback) ->
                        callback.completed(key + ':' + context)))
        {
            BasicFuture<AsyncCacheResult<String>> future =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key", "context", future);
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future.get().type());
            String firstResult = future.get().value();
            Assert.assertEquals("key:context", firstResult);

            // try again
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            // In real cases, result doesn't depend on context
            cache.get("key", "ctx", future);
            Assert.assertEquals(AsyncCacheResultType.HIT, future.get().type());
            Assert.assertSame(firstResult, future.get().value());
            // entry weight: 6 + 22 + 48 * 4 + 8 = 228
            Assert.assertEquals(228, storage.weightedSize());

            // try another key
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key2", "ctx", future);
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future.get().type());
            Assert.assertEquals("key2:ctx", future.get().value());

            // Old result still not evicted
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            // result doesn't depend on context
            cache.get("key", "ctx", future);
            Assert.assertEquals(AsyncCacheResultType.HIT, future.get().type());
            Assert.assertSame(firstResult, future.get().value());

            // third record will cause eviction
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key3", "ctx", future);
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future.get().type());
            Assert.assertEquals("key3:ctx", future.get().value());

            // Least recently used node not evicted
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            // result doesn't depend on context
            cache.get("key", "ctx", future);
            Assert.assertEquals(AsyncCacheResultType.HIT, future.get().type());
            Assert.assertSame(firstResult, future.get().value());

            // Not so recently used node was evicted
            future = new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key2", "ctx", future);
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future.get().type());
            Assert.assertEquals("key2:ctx", future.get().value());
        }
    }

    @Test
    public void testLock() throws Exception {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        AsyncLoader<String, String, String> loader = new AsyncLoader<>() {
            @Override
            public void load(
                final String key,
                final String context,
                final FutureCallback<? super String> callback)
            {
                Thread thread =
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                latch2.countDown();
                                latch1.await();
                                callback.completed(key + ':' + context);
                            } catch (InterruptedException e) {
                                callback.cancelled();
                            }
                        }
                    };
                thread.setDaemon(true);
                thread.start();
            }
        };
        try (ConcurrentLinkedHashMapAsyncStorage<String, String> storage =
                new ConcurrentLinkedHashMapAsyncStorage<>(
                    512L,
                    2,
                    key -> key.length() * 2 + 48,
                    value -> value.length() * 2 + 48);
            AsyncCache<String, String, String, RuntimeException> cache =
                new AsyncCache<>(
                    storage,
                    new ConstAsyncCacheTtlCalculator(3600000),
                    loader))
        {
            BasicFuture<AsyncCacheResult<String>> future1 =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            BasicFuture<AsyncCacheResult<String>> future2 =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key", "ctx1", future1);
            cache.get("key", "ctx2", future2);
            latch2.await();
            latch1.countDown();
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future1.get().type());
            Assert.assertEquals(
                AsyncCacheResultType.LOCK,
                future2.get().type());
            Assert.assertSame(
                future1.get().value(),
                future2.get().value());
            Assert.assertEquals("key:ctx1", future1.get().value());

            // Check that result was saved
            BasicFuture<AsyncCacheResult<String>> future =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key", "ctx3", future);
            Assert.assertEquals(AsyncCacheResultType.HIT, future.get().type());
            Assert.assertSame(
                future1.get().value(),
                future.get().value());
        }
    }

    @Test
    public void testEmptyStorage() throws Exception {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        AsyncLoader<String, String, String> loader = new AsyncLoader<>() {
            @Override
            public void load(
                final String key,
                final String context,
                final FutureCallback<? super String> callback)
            {
                Thread thread =
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                latch2.countDown();
                                latch1.await();
                                callback.completed(key + ':' + context);
                            } catch (InterruptedException e) {
                                callback.cancelled();
                            }
                        }
                    };
                thread.setDaemon(true);
                thread.start();
            }
        };
        try (AsyncCache<String, String, String, RuntimeException> cache =
                new AsyncCache<>(
                    EmptyAsyncStorage.instance(),
                    new ConstAsyncCacheTtlCalculator(3600000),
                    loader))
        {
            BasicFuture<AsyncCacheResult<String>> future1 =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            BasicFuture<AsyncCacheResult<String>> future2 =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key", "ctx1", future1);
            cache.get("key", "ctx2", future2);
            latch2.await();
            latch1.countDown();
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future1.get().type());
            Assert.assertEquals(
                AsyncCacheResultType.LOCK,
                future2.get().type());
            Assert.assertSame(
                future1.get().value(),
                future2.get().value());
            Assert.assertEquals("key:ctx1", future1.get().value());

            BasicFuture<AsyncCacheResult<String>> future =
                new BasicFuture<>(EmptyFutureCallback.INSTANCE);
            cache.get("key", "ctx3", future);
            Assert.assertEquals(
                AsyncCacheResultType.MISS,
                future.get().type());
            Assert.assertEquals("key:ctx3", future.get().value());
        }
    }
}

