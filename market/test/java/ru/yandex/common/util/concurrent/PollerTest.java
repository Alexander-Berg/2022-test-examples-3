package ru.yandex.common.util.concurrent;

import javax.annotation.Nonnull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * User: jkff
 * Date: Feb 5, 2010
 * Time: 12:25:58 PM
 */
public class PollerTest {
    @Test
    public void testPoller() throws Exception {
        final List<Integer> intervals = Collections.synchronizedList(Arrays.asList(30,200,50,200,400,600,600));
        final AtomicInteger index = new AtomicInteger(0);
        final AtomicLong prevTime = new AtomicLong(System.currentTimeMillis());

        final List<Integer> res = Collections.synchronizedList(new ArrayList<Integer>());

        Poller<Integer> p = new Poller<Integer>(100, false, "test") {
            @Override
            public Integer poll() {
                long now = System.currentTimeMillis();
                if(now - prevTime.get() > intervals.get(index.get())) {
                    prevTime.set(now);
                    return index.getAndIncrement();
                }
                return null;
            }

            @Override
            public void process(@Nonnull Integer i) {
                res.add(i);
            }
        };

        Stoppable s = p.start();

        Thread.sleep(1500);

        assertEquals(Arrays.asList(0,1,2,3,4), res);

        s.takePoison();

        Thread.sleep(1000);
        
        assertEquals(Arrays.asList(0,1,2,3,4), res);
    }
}
