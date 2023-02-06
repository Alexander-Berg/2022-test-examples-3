package ru.yandex.market.api.util.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.test.TestHelp;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by apershukov on 25.01.17.
 */
public class AsyncConcurrentBarrierTest extends UnitTestBase {

    private static AsyncExecutor executor;
    private AsyncConcurrentBarrier<String, String> lock;

    @BeforeClass
    public static void setUpClass() {
        executor = new AsyncExecutor(ApiFuturesHelper.getInstance());
    }


    public void setUp() throws Exception {
        super.setUp();
        lock = new AsyncConcurrentBarrier<>();
    }

    @Test
    public void testExecuteSingleAction() {
        ContextHolderTestHelper.initContext(new Context("test-req-id"));

        Future<String> future = lock.execute("key", () -> executor.call(() -> "value"))
            .thenSync(v -> v + "-" + ContextHolder.get().getRequestId());
        String reqId = Futures.waitAndGet(future);

        assertEquals("value-test-req-id", reqId);
    }

    @Test
    public void testCallSameActionTwice() {
        AtomicInteger counter = new AtomicInteger(0);

        Supplier<Future<String>> action = () -> executor.call(() -> {
            counter.incrementAndGet();
            // Для того чтобы добиться одновременного выполнения действия, тормозим его
            TestHelp.sleep(1000);
            return "value";
        });

        Future<String> future1 = executor.call(() -> {
            ContextHolderTestHelper.initContext(new Context("req1"));
            return Futures.waitAndGet(lock.execute("key", action)
                .thenSync(v -> v + "-" + ContextHolder.get().getRequestId()));
        });

        Future<String> future2 = executor.call(() -> {
            ContextHolderTestHelper.initContext(new Context("req2"));
            return Futures.waitAndGet(lock.execute("key", action)
                .thenSync(v -> v + "-" + ContextHolder.get().getRequestId()));
        });


        String id1 = Futures.waitAndGet(future1);
        String id2 = Futures.waitAndGet(future2);

        // Проверка того что нельзя выполнять одно и тоже действие дважды
        assertEquals(1, counter.get());

        assertEquals("value-req1", id1);
        assertEquals("value-req2", id2);
    }

    @Test
    public void testExecuteSameActionSequence() {
        Future<String> future = lock.execute("key", () -> executor.call(() -> "value1"));
        assertEquals("value1", Futures.waitAndGet(future));

        future = lock.execute("key", () -> executor.call(() -> "value2"));
        assertEquals("value2", Futures.waitAndGet(future));
    }

    /**
     * Проверка корректной передачи контекста в поток обработки повалившейся future
     */
    @Test
    public void testContextOnActionFail() {
        ContextHolderTestHelper.initContext(new Context("test-req-id"));

        Future<String> future = lock.execute("key", () -> executor.call(() -> {
            throw new RuntimeException("error");
        }));

        Promise<String> result = Futures.newPromise();
        Futures.onResult(result, future, f -> {
            assertFalse(f.isSuccess());
            result.trySuccess(ContextHolder.get().getRequestId());
        });

        String reqId = Futures.waitAndGet(result);
        assertEquals("test-req-id", reqId);
    }

    /**
     * Проверка того что неудачное действие не влияет на дальнейшее выполнение действий
     * с тем же ключем
     */
    @Test
    public void testDoSameActionAfterFailedTry() {
        Future<String> future = lock.execute("key", () -> executor.call(() -> {
            throw new RuntimeException("error");
        }));

        Futures.wait(future);

        future = lock.execute("key", () -> executor.call(() -> "value"));
        String value = Futures.waitAndGet(future);

        assertEquals("value", value);
    }

    /**
     * Тестирование того что на время выполнения действия контекст сбрасывается
     */
    @Test
    @WithContext
    public void testContextInActionExecution() {
        Future<String> future = lock.execute("key", () -> {
            assertNull(ContextHolder.get());
            return executor.call(() -> "value");
        });

        assertNotNull(ContextHolder.get());
        Futures.wait(future);
        assertTrue(future.isSuccess());
    }
}
