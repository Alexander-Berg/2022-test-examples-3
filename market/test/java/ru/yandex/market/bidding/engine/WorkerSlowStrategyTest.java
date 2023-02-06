package ru.yandex.market.bidding.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import com.codahale.metrics.MetricRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.bidding.engine.storage.BasicOracleStorage;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by skiftcha on 18.03.2016.
 */
@Ignore
public class WorkerSlowStrategyTest {
    private static final Logger logger = LoggerFactory.getLogger(WorkerSlowStrategyTest.class);

    private Dispatcher dispatcher;
    private int shopWorkerCount = 1;
    private int shopWorkerAbandonTimeMinutes = 1;
    private int bulkWorkerCount = 1;
    private int bulkWorkerAbandonTimeMinutes = 1;
    private Tasks tasks = new Tasks(100);
    private TestAction[] actions;
    private Map<Long, Semaphore> locks = new HashMap<>();
    private String error;

    // включает jog4j для отладки
    private boolean debug = false;

    @Before
    public void setUp() {
        error = null;
        DispatcherOptions dopt = new DispatcherOptions(shopWorkerCount, shopWorkerAbandonTimeMinutes, bulkWorkerCount, bulkWorkerAbandonTimeMinutes);
        dispatcher = new Dispatcher(tasks, new BasicOracleStorage(), dopt, new MetricRegistry());
        new Thread(dispatcher).start();
    }

    // Слип для удобства, чтобы логи из одного теста в другой не перетекли
    @After
    public void tearDown() throws Exception {
        if (debug) {
            Thread.sleep(500L);
        }
    }

    @Test
    public void simpleSingleShopTest() {
        actions = new TestAction[10];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new TestAction(i, 100, false);
        }
        actions[3].setSlow(true);
        submit(actions.length - 1);
        performWaitAndAssert(actions[actions.length - 1]);
    }

    @Test
    public void simpleDoubleShopOneSlowTest() {
        actions = new TestAction[10];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new TestAction(i, 100 + i % 2, false);
        }
        actions[3].setSlow(true);
        submit(actions.length - 1);
        performWaitAndAssert(actions[actions.length - 1]);
    }

    @Test
    public void simpleDoubleShopTwoSlowTest() {
        actions = new TestAction[10];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = new TestAction(i, 100 + i % 2, false);
        }
        actions[3].setSlow(true);
        actions[6].setSlow(true);
        submit(actions.length - 2);
        performWaitAndAssert(actions[actions.length - 2]);
    }

    private void submit(Integer... skip) {
        for (int i = 0; i < actions.length; i++) {
            if (Arrays.asList(skip).contains(i)) {
                continue;
            }
            logger.debug("submitting " + (i + 1));
            tasks.submit(actions[i]);
        }
    }

    private void performWaitAndAssert(TestAction task) {
        ActionResult result = tasks.perform(task);
        assertNull(error, error);
        assertTrue("await task returned not OK", result == ActionResult.OK);
        for (int i = 0; i < actions.length; i++) {
            assertTrue("action " + (i + 1) + " not disposed or succeeded, current state is " + actions[i].getState(), actions[i].getState() == State.DISPOSED || actions[i].state == State.SUCCEEDED);
        }
    }

    enum State {
        NEW,
        PREPARED,
        PERSISTED,
        SUCCEEDED,
        FAILED,
        CANCELLED,
        DISPOSED
    }

    private class TestAction implements Action {
        private long time = System.nanoTime();
        private int num;
        private long shopId;
        private boolean slow;
        private State state;
        private Semaphore lock;

        TestAction(int num, long shopId, boolean slow) {
            this.num = num;
            this.shopId = shopId;
            this.slow = slow;
            this.state = State.NEW;
            this.lock = locks.get(shopId);
            if (lock == null) {
                lock = new Semaphore(1);
                locks.put(shopId, lock);
            }
        }

        void assertTrue(String message, boolean ok) {
            if (!ok && error == null) {
                error = "[action " + (num + 1) + " shop " + shopId + "] " + message;
                throw new RuntimeException(error);
            }
        }

        void fail(String message) {
            assertTrue(message, false);
        }

        State getState() {
            return state;
        }

        void setSlow(boolean slow) {
            this.slow = slow;
        }

        private void println(String s) {
            if (debug) {
                logger.debug(s + " num: " + (num + 1) + " shop: " + shopId);
            }
        }

        private void sleep(long time) {
            try {
                Thread.sleep(time);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Optional<Long> shop() {
            return Optional.of(shopId);
        }

        @Override
        public void onSuccess() {
            assertTrue("onSuccess called not after persist", state == State.PERSISTED);
            sleep(50L);
            lock.release();
            state = State.SUCCEEDED;
            println("success");
        }

        @Override
        public void onFailure() {
            println("failure");
            sleep(50L);
            state = State.FAILED;
            fail("onFailure should not be called");
        }

        @Override
        public void onCancel() {
            println("cancel");
            sleep(50L);
            state = State.CANCELLED;
            fail("onCancel should not be called");
        }

        @Override
        public void onDispose() {
            println("dispose");
            assertTrue("onDispose called not after success", state == State.SUCCEEDED);
            sleep(50L);
            state = State.DISPOSED;
        }

        @Override
        public void persist(BasicOracleStorage storage) {
            println("persist");
            assertTrue("persist called not after prepare", state == State.PREPARED);
            state = State.PERSISTED;
            if (slow) {
                sleep(1000L);
            } else {
                sleep(100L);
            }
            println("persisted");
        }

        @Override
        public Outcome prepare() {
            println("prepare");
            assertTrue("prepared action before previous succeeded", lock.tryAcquire());
            assertTrue("prepare called not in the begining", state == State.NEW);
            sleep(100L);
            state = State.PREPARED;
            if (slow) {
                return Outcome.SLOW;
            } else {
                return Outcome.FAST;
            }
        }

        @Override
        public long expirationTime() {
            return time + 10000000000L;
        }

        @Override
        public long startTime() {
            return time;
        }

        @Override
        public String info() {
            return "info num: " + (num + 1) + " slow: " + slow;
        }
    }
}
