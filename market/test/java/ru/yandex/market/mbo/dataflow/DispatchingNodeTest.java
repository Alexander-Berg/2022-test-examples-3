package ru.yandex.market.mbo.dataflow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Test for dispatching node.
 *
 * @author maxkar
 */
public class DispatchingNodeTest {

    /**
     * Tests, that dispatching work.
     */
    @Test
    public void testDispatching() {
        class Handler implements DataProcessingNode<String> {
            boolean started;
            boolean finished;
            Throwable error;
            boolean fatal;

            final Set<String> data = new HashSet<String>();

            public void beforeProcessing() {
                if (started) {
                    fatal = true;
                }
                started = true;
            }

            public void process(String data) {
                if (!started) {
                    fatal = true;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fatal = true;
                }
                this.data.add(data);
            }

            public void processingFailed(Throwable t) {
                if (this.error != null) {
                    fatal = true;
                }
                error = t;
            }

            public void processingFinished() {
                if (finished != false) {
                    fatal = true;
                }
                finished = true;
            }

            void reset() {
                started = false;
                finished = false;
                error = null;
                fatal = false;
                data.clear();
            }
        }

        final List<Handler> handlers = Arrays.asList(new Handler(),
                new Handler(), new Handler());
        final DataProcessingNode<String> node = new DispatchNode<String>(
                handlers);
        final Set<String> input = new LinkedHashSet<String>(Arrays.asList("a",
                "b", "c", "d", "e", "f", "g", "h", "i", "fail", "l", "m", "n"));
        node.beforeProcessing();
        for (String s : input) {
            node.process(s);
        }
        node.processingFinished();

        final Set<String> agg = new HashSet<String>();
        for (Handler h : handlers) {
            Assert.assertTrue(h.started);
            Assert.assertTrue(h.finished);
            Assert.assertFalse(h.fatal);
            Assert.assertNull(h.error);
            final int oldS = agg.size();
            agg.addAll(h.data);
            Assert.assertEquals(agg.size(), h.data.size() + oldS);
            h.reset();
        }
        Assert.assertEquals(input, agg);
        agg.clear();

        node.beforeProcessing();
        for (String s : input) {
            node.process(s);
        }
        node.processingFailed(new Throwable());
        for (Handler h : handlers) {
            Assert.assertTrue(h.started);
            Assert.assertFalse(h.finished);
            Assert.assertNotNull(h.error);
            Assert.assertFalse(h.fatal);
            final int oldS = agg.size();
            agg.addAll(h.data);
            Assert.assertEquals(agg.size(), h.data.size() + oldS);
            h.reset();
        }
        Assert.assertEquals(input, agg);
    }

    /**
     * Tests, that handler stops on internal error.
     */
    @Test
    public void testStopsOnInternal() {
        final int limit = 1000;
        final int failAt = 500;
        class Handler implements DataProcessingNode<Integer> {
            boolean finished;
            boolean failed;
            int lastId;

            public void beforeProcessing() {
                // ignore
            }

            public void process(Integer data) {
                lastId = data.intValue();
                if (lastId == failAt) {
                    throw new RuntimeException();
                }
            }

            public void processingFailed(Throwable t) {
                failed = true;
            }

            public void processingFinished() {
                finished = true;
            }
        }
        final List<Handler> handlers = Arrays.asList(new Handler(),
                new Handler(), new Handler());
        final DataProcessingNode<Integer> node = new DispatchNode<Integer>(
                handlers);
        node.beforeProcessing();
        for (int i = 1; i <= failAt; i++) {
            node.process(Integer.valueOf(i));
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }
        for (int i = failAt + 1; i < limit; i++) {
            node.process(Integer.valueOf(i));
        }
        node.processingFinished();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }
        for (Handler h : handlers) {
            Assert.assertTrue(h.failed);
            Assert.assertFalse(h.finished);
            Assert.assertTrue(h.lastId <= failAt);
        }
    }
}
