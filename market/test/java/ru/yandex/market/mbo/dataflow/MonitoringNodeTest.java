package ru.yandex.market.mbo.dataflow;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for monitoring node.
 *
 * @author maxkar
 */
public class MonitoringNodeTest {
    /**
     * Test basic monitoring node operations.
     */
    @Test
    public void testBasicOperatoins() {
        final MonitoringNode<?> mn = new MonitoringNode<Object>();
        Assert.assertTrue(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        mn.beforeProcessing();
        Assert.assertFalse(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        mn.process(null);
        Assert.assertFalse(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        mn.processingFinished();
        Assert.assertTrue(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        mn.beforeProcessing();
        mn.processingFailed(new Throwable());
        Assert.assertTrue(mn.isStopped());
        Assert.assertTrue(mn.hasError());
        Assert.assertNotNull(mn.getError());
        mn.beforeProcessing();
        Assert.assertFalse(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        mn.processingFinished();
        Assert.assertTrue(mn.isStopped());
        Assert.assertFalse(mn.hasError());
        Assert.assertNull(mn.getError());
        try {
            mn.awaitTermination();
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    /**
     * Tests event dispatching.
     */
    @Test
    public void testDispatch() {
        final AtomicInteger id = new AtomicInteger();
        final MonitoringNode<Object> mn = new MonitoringNode<Object>(
                new DataProcessingNode<Object>() {
                    public void processingFinished() {
                        id.set(4);
                    }

                    public void processingFailed(Throwable t) {
                        id.set(3);
                    }

                    public void process(Object data) {
                        id.set(2);
                    }

                    public void beforeProcessing() {
                        id.set(1);
                    }
                });
        mn.beforeProcessing();
        Assert.assertEquals(1, id.get());
        mn.process(mn);
        Assert.assertEquals(2, id.get());
        mn.processingFinished();
        Assert.assertEquals(4, id.get());
        mn.beforeProcessing();
        Assert.assertEquals(1, id.get());
        mn.process(mn);
        Assert.assertEquals(2, id.get());
        mn.processingFailed(new Throwable());
        Assert.assertEquals(3, id.get());
    }
}
