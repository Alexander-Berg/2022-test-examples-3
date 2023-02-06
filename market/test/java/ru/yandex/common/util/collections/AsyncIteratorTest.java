package ru.yandex.common.util.collections;

import java.util.Iterator;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.common.util.functional.Function;

import static org.junit.Assert.assertEquals;

/**
 * @author lvovich
 */
public class AsyncIteratorTest {

    private static final int TIMEOUT_FAST = 500;
    private static final int TIMEOUT_SLOW = 1000;

    @Test(timeout = TIMEOUT_FAST)
    public void testFast() {
        runTest(0, 0);
    }

    @Test(timeout = TIMEOUT_SLOW)
    public void testSlowSource() {
        runTest(10, 0);
    }

    @Test(timeout = TIMEOUT_SLOW)
    public void testSlowFunction() {
        runTest(0, 100);
    }

    @Test(timeout = TIMEOUT_SLOW)
    public void testSlowConsumer() {
        runTest(0, 0);
    }

    @Test(timeout = TIMEOUT_FAST)
    public void testException() {
        runTest(0, 0, true);
    }


    private void runTest(long sourceDelay, long functionDelay) {
        runTest(sourceDelay, functionDelay, false);
    }

    private void runTest(long sourceDelay, long functionDelay, boolean withException) {
        Iterable<Integer> iterable = new SourceIterable(sourceDelay, withException);
        Function<Integer, Integer> function = new Square(functionDelay);
        final Iterator<Pair<Integer, Integer>> iterator = CollectionUtils.mapAsync(iterable, function, 5).iterator();
        boolean[] found = new boolean[10];

        while (iterator.hasNext()) {
            try {
                final Pair<Integer, Integer> result = iterator.next();
                final Integer arg = result.getFirst();
                assertEquals(arg * arg, (int) result.getSecond());
                found[arg] = true;
            } catch (RuntimeException e) {
                if (!withException) {
                    throw e;
                }
            }
        }
        int foundCount = 0;
        for (boolean f : found) {
            if (f)
                foundCount += 1;
        }
        assertEquals(foundCount, withException ? 9 : 10);
    }


    private static class SourceIterator implements Iterator<Integer> {
        private static final int LIMIT = 10;
        private final long delayMs;
        private int count = 0;

        private SourceIterator(long delayMs) {
            this.delayMs = delayMs;
        }

        @Override
        public boolean hasNext() {
            return count < LIMIT;
        }

        @Override
        public Integer next() {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return count++;
        }

        @Override
        public void remove() {
            throw new RuntimeException("Not implemented");
        }
    }

    private static class SourceIterable implements Iterable<Integer> {
        private final long delayMs;
        private final boolean withException;

        private SourceIterable(long delayMs, boolean withException) {
            this.delayMs = delayMs;
            this.withException = withException;
        }

        @Nonnull
        @Override
        public Iterator<Integer> iterator() {
            return withException ? new SourceIteratorWithException(delayMs) : new SourceIterator(delayMs);
        }
    }

    public static class SourceIteratorWithException extends SourceIterator {
        private SourceIteratorWithException(long delayMs) {
            super(delayMs);
        }

        @Override
        public Integer next() {
            final Integer integer = super.next();
            if (integer == 5) {
                throw new RuntimeException();
            }
            return integer;
        }
    }

    private static class Square extends Function<Integer, Integer> {
        private final long delayMs;

        private Square(long delayMs) {
            this.delayMs = delayMs;
        }

        @Override
        public Integer apply(Integer arg) {
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return arg * arg;
        }
    }

}
