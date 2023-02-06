package ru.yandex.market.mbo.dataflow;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for the join node.
 *
 * @author maxkar
 */
public class JoinNodeTest {
    static class Handler implements DataProcessingNode<String> {

        boolean fatal;
        boolean started;
        boolean finished;
        boolean failed;
        int data;

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
            this.data++;
        }

        public void processingFailed(Throwable t) {
            if (!started) {
                fatal = true;
            }
            failed = true;
        }

        public void processingFinished() {
            if (!started) {
                fatal = true;
            }
            finished = true;
        }

        void reset() {
            fatal = false;
            started = false;
            finished = false;
            failed = false;
            data = 0;
        }
    }

    /**
     * Tests, that the event propagation works.
     */
    @Test
    public void testEventPropagation() {
        final Handler h = new Handler();
        final JoinNode<String> jn = new JoinNode<String>(h);
        final List<DataProcessingNode<String>> pnodes = new ArrayList<DataProcessingNode<String>>(
                3);
        pnodes.add(jn.createInputPort());
        pnodes.add(jn.createInputPort());
        pnodes.add(jn.createInputPort());

        final int dataSize = 100;
        for (DataProcessingNode<String> pn : pnodes) {
            pn.beforeProcessing();
        }
        for (int i = 0; i < dataSize; i++) {
            pnodes.get(i % pnodes.size()).process("Test");
        }
        for (DataProcessingNode<String> pn : pnodes) {
            pn.processingFinished();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertTrue(h.started);
        Assert.assertTrue(h.finished);
        Assert.assertFalse(h.fatal);
        Assert.assertFalse(h.failed);
        Assert.assertEquals(dataSize, h.data);
        h.reset();

        for (DataProcessingNode<String> pn : pnodes) {
            pn.beforeProcessing();
        }
        for (int i = 0; i < dataSize; i++) {
            pnodes.get(i % pnodes.size()).process("Test");
        }
        for (DataProcessingNode<String> pn : pnodes) {
            pn.processingFailed(new Throwable());
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertTrue(h.started);
        Assert.assertFalse(h.finished);
        Assert.assertFalse(h.fatal);
        Assert.assertTrue(h.failed);
        Assert.assertEquals(dataSize, h.data);
        h.reset();

        for (DataProcessingNode<String> pn : pnodes) {
            pn.beforeProcessing();
        }
        for (int i = 0; i < dataSize; i++) {
            pnodes.get(i % pnodes.size()).process("Test");
        }

        pnodes.get(0).processingFinished();
        pnodes.get(1).processingFailed(new Throwable());
        for (DataProcessingNode<String> pn : pnodes.subList(2, pnodes.size())) {
            pn.processingFailed(new Throwable());
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertTrue(h.started);
        Assert.assertFalse(h.finished);
        Assert.assertFalse(h.fatal);
        Assert.assertTrue(h.failed);
        Assert.assertEquals(dataSize, h.data);
        h.reset();
    }

    /**
     * Tests abrupt completion on internal processing error.
     */
    @Test
    public void testAbrubtOnSelfError() {
        final Handler h = new Handler() {
            @Override
            public void process(String data) {
                super.process(data);
                if ("fail".equals(data)) {
                    throw new Error();
                }
            }
        };
        final JoinNode<String> jn = new JoinNode<String>(h);
        final List<DataProcessingNode<String>> pnodes = new ArrayList<DataProcessingNode<String>>(
                3);
        pnodes.add(jn.createInputPort());
        pnodes.add(jn.createInputPort());
        pnodes.add(jn.createInputPort());

        final int dataSize = 100;
        final int failIdx = 67;
        for (DataProcessingNode<String> pn : pnodes) {
            pn.beforeProcessing();
        }
        for (int i = 0; i < dataSize; i++) {
            pnodes.get(i % pnodes.size()).process(
                    failIdx == i ? "fail" : "Test");
        }
        for (DataProcessingNode<String> pn : pnodes) {
            pn.processingFinished();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Assert.fail();
        }

        Assert.assertTrue(h.started);
        Assert.assertFalse(h.finished);
        Assert.assertFalse(h.fatal);
        Assert.assertTrue(h.failed);
        Assert.assertEquals(failIdx + 1, h.data);
    }
}
