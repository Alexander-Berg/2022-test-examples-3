package ru.yandex.common.util;

import junit.framework.TestCase;

import java.util.List;

/**
 * Created 29.03.12 19:17
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class GuardedBatcherTest extends TestCase {

    public void testCollect() throws Exception {
        Batcher<Void> f = new GuardedBatcher<Void>(new Batcher<Void>(50) {
            @Override
            protected void doFlushAction(final List<Void> batch) {
                //doNothing
            }
        });
        f.submit(null);
        f.submit(null);

        f = null;

        System.gc();
        Thread.sleep(1000);
    }
}
