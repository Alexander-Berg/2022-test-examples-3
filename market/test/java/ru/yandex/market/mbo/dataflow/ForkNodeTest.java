package ru.yandex.market.mbo.dataflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Test for the fork processing node.
 *
 * @author maxkar
 */
public class ForkNodeTest {

    /**
     * Tests, that events are dispatched to all handlers.
     */
    @Test
    public void testDispatch() {
        final List<String> source = Arrays.asList("a", "b", "c", "d", "e", "f",
                "k");
        class Handler implements DataProcessingNode<String> {
            boolean ps;
            final List<String> data = new ArrayList<String>();
            boolean finished;
            Throwable ex;

            public void beforeProcessing() {
                Assert.assertFalse(ps);
                ps = true;
            }

            public void process(String data) {
                this.data.add(data);
            }

            public void processingFailed(Throwable t) {
                Assert.assertFalse(finished);
                Assert.assertNull(ex);
                ex = t;
            }

            public void processingFinished() {
                Assert.assertFalse(finished);
                Assert.assertNull(ex);
                finished = true;
            }

            void reset() {
                ps = false;
                data.clear();
                finished = false;
                ex = null;
            }
        }

        final Collection<Handler> handlers = Arrays.asList(new Handler(),
                new Handler(), new Handler());
        final DataProcessingNode<String> fork = new ForkNode<String>(handlers);
        fork.beforeProcessing();
        for (String s : source) {
            fork.process(s);
        }
        fork.processingFinished();

        for (Handler h : handlers) {
            Assert.assertTrue(h.ps);
            Assert.assertTrue(h.finished);
            Assert.assertNull(h.ex);
            Assert.assertEquals(source, h.data);
            h.reset();
        }
        fork.beforeProcessing();
        for (String s : source) {
            fork.process(s);
        }
        fork.processingFailed(new Throwable("Test"));
        for (Handler h : handlers) {
            Assert.assertTrue(h.ps);
            Assert.assertFalse(h.finished);
            Assert.assertNotNull(h.ex);
            Assert.assertEquals(source, h.data);
            h.reset();
        }
    }

    /**
     * Tests failure on the startup.
     */
    public void testFailOnStart() {
        class Handler implements DataProcessingNode<String> {
            final boolean fail;
            boolean ps;
            boolean failed;
            boolean finished;
            boolean data;

            Handler(boolean fail) {
                this.fail = fail;
            }

            public void beforeProcessing() {
                if (fail) {
                    throw new Error();
                }
                ps = true;
            }

            public void process(String data) {
                this.data = true;
            }

            public void processingFailed(Throwable t) {
                failed = true;
            }

            public void processingFinished() {
                finished = true;
            }
        }

        final int failedIdx = 6;
        final int size = 10;

        final List<Handler> handlers = new ArrayList<Handler>(size);
        for (int i = 0; i < size; i++) {
            handlers.add(new Handler(i == failedIdx));
        }

        final DataProcessingNode<String> dpn = new ForkNode<String>(handlers);
        dpn.beforeProcessing();
        dpn.process("a");
        dpn.processingFinished();

        for (int i = 0; i < failedIdx; i++) {
            final Handler h = handlers.get(i);
            Assert.assertTrue(h.ps);
            Assert.assertTrue(h.failed);
            Assert.assertFalse(h.finished);
            Assert.assertFalse(h.data);
        }
        {
            final Handler h = handlers.get(failedIdx);
            Assert.assertFalse(h.ps);
            Assert.assertFalse(h.failed);
            Assert.assertFalse(h.finished);
            Assert.assertFalse(h.data);
        }
        for (int i = failedIdx + 1; i < size; i++) {
            final Handler h = handlers.get(i);
            Assert.assertFalse(h.ps);
            Assert.assertFalse(h.failed);
            Assert.assertFalse(h.finished);
            Assert.assertFalse(h.data);
        }
    }

    /**
     * Tests failure in the process.
     */
    public void testFailOnProcess() {
        class Handler implements DataProcessingNode<String> {
            final int fail;
            boolean ps;
            boolean failed;
            boolean finished;
            int data;

            Handler(int fail) {
                this.fail = fail;
            }

            public void beforeProcessing() {
                ps = true;
            }

            public void process(String data) {
                if (++this.data == fail) {
                    throw new Error("Yes!");
                }
            }

            public void processingFailed(Throwable t) {
                failed = true;
            }

            public void processingFinished() {
                finished = true;
            }
        }

        final int failedIdx = 6;
        final int failedIter = 5;
        final int size = 10;

        final List<Handler> handlers = new ArrayList<Handler>(size);
        for (int i = 0; i < size; i++) {
            handlers.add(new Handler(i == failedIdx ? failedIter : 0));
        }

        final DataProcessingNode<String> dpn = new ForkNode<String>(handlers);
        dpn.beforeProcessing();
        for (int i = 0; i < failedIter * 2; i++) {
            dpn.process("a");
        }
        dpn.processingFinished();

        for (int i = 0; i < size; i++) {
            final Handler h = handlers.get(i);
            Assert.assertTrue(h.ps);
            Assert.assertTrue(h.failed);
            Assert.assertFalse(h.finished);
            Assert.assertEquals(i < failedIdx ? failedIter + 1 : failedIter,
                    h.data);
        }
    }
}
