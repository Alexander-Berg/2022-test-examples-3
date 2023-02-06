package ru.yandex.market.mbo.dataflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Test of the buffered processor.
 *
 * @author maxkar
 */
public class BufferedProcessorTest {
    /**
     * Tests events propagation.
     */
    @Test
    public void testEvents() {
        final List<String> input = Arrays.asList("a", "b", "c", "e", "f", "g");
        final List<String> result = new ArrayList<String>();
        final DataProcessingNode<String> dpn = new BufferedNode<String>(2,
                new DataProcessingNode<String>() {
                    public void processingFinished() {
                        // ignore
                    }

                    public void processingFailed(Throwable t) {
                        // ignore
                    }

                    public void process(String data) {
                        result.add(data);
                    }

                    public void beforeProcessing() {
                        // ignore.
                    }

                });
        dpn.beforeProcessing();
        for (String s : input) {
            dpn.process(s);
        }
        dpn.processingFinished();
        Assert.assertEquals(input, result);
    }

    /**
     * Tests processing marks.
     */
    @Test
    public void testStartup() {
        final AtomicBoolean ssucc = new AtomicBoolean();
        final AtomicBoolean esucc = new AtomicBoolean();
        final AtomicBoolean pfail = new AtomicBoolean();
        final DataProcessingNode<String> dpn = new BufferedNode<String>(2,
                new DataProcessingNode<String>() {
                    public void processingFinished() {
                        esucc.set(true);
                    }

                    public void processingFailed(Throwable t) {
                        pfail.set(true);
                    }

                    public void process(String data) {
                        // ignore
                    }

                    public void beforeProcessing() {
                        ssucc.set(true);
                    }

                });
        dpn.beforeProcessing();
        dpn.processingFinished();
        Assert.assertTrue(ssucc.get());
        Assert.assertTrue(esucc.get());
        Assert.assertFalse(pfail.get());
        dpn.beforeProcessing();
        ssucc.set(false);
        esucc.set(false);
        pfail.set(false);
        dpn.processingFailed(new Throwable());
        Assert.assertTrue(ssucc.get());
        Assert.assertFalse(esucc.get());
        Assert.assertTrue(pfail.get());
    }

    /**
     * Tests aborting on internal exception.
     */
    @Test
    public void testAbortOnError() {
        testAbortOnError(10, 2, 4);
        testAbortOnError(10, 2, 50);
        testAbortOnError(10, 32, 34);
        testAbortOnError(10, 32, 78);
    }

    /**
     * Tests abort on error.
     *
     * @param capacity queue capacity.
     * @param mark     mark of the error.
     * @param over     number of the elements after mark.
     */
    private void testAbortOnError(int capacity, final int mark, int over) {
        final AtomicBoolean reached = new AtomicBoolean();
        final AtomicBoolean overed = new AtomicBoolean();
        final AtomicBoolean fdetected = new AtomicBoolean();
        final AtomicBoolean finished = new AtomicBoolean();
        final DataProcessingNode<Integer> dpn = new BufferedNode<Integer>(2,
                new DataProcessingNode<Integer>() {
                    private int cval = 0;

                    public void processingFinished() {
                        finished.set(true);
                    }

                    public void processingFailed(Throwable t) {
                        fdetected.set(true);
                    }

                    public void process(Integer data) {
                        if (++cval != data.intValue()) {
                            overed.set(true);
                        }
                        if (cval == mark) {
                            reached.set(true);
                            throw new RuntimeException("Test");
                        }
                    }

                    public void beforeProcessing() {
                        // ignore.
                    }
                });

        dpn.beforeProcessing();
        for (int i = 1; i < mark + over; i++) {
            dpn.process(Integer.valueOf(i));
        }
        dpn.processingFinished();
        Assert.assertTrue(reached.get());
        Assert.assertTrue(fdetected.get());
        Assert.assertFalse(overed.get());
        Assert.assertFalse(finished.get());
    }
}
