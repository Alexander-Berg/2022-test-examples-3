package ru.yandex.antifraud;

import java.util.concurrent.CancellationException;

import org.apache.http.concurrent.FutureCallback;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.antifraud.util.Waterfall;
import ru.yandex.test.util.TestBase;

public class WaterfallTest extends TestBase {
    public WaterfallTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws Exception {
        final boolean[] calls = new boolean[]{false, false};

        Waterfall.<Integer, String>waterfall(
                (cb) -> {
                    logger.info("wf: 0 ");
                    cb.completed(5);
                    calls[0] = true;
                },
                (cb, count) -> {
                    logger.info("wf: 1 " + count);
                    Assert.assertEquals(5, count.intValue());
                    calls[1] = true;
                    cb.completed("finished");
                },
                new FutureCallback<>() {
                    @Override
                    public void completed(String o) {
                        logger.info("wf: fin " + o);
                    }

                    @Override
                    public void failed(Exception e) {
                        throw new RuntimeException(e);
                    }

                    @Override
                    public void cancelled() {
                        throw new CancellationException("cancel");
                    }
                });

        Assert.assertArrayEquals(new boolean[]{true, true}, calls);
    }
}
