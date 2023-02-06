package ru.yandex.ir.util;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XXX: write some tests that test with many threads
 *
 * @author yozh
 */
public class TwoPrioritySetQueueTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TwoPrioritySetQueueTest.class);

    public void testSimple() throws Exception {
        TwoPrioritySetQueue<Integer> q = new TwoPrioritySetQueue<>();
        q.add(1);
        q.add(2);
        q.addToFaster(3);
        q.addToFaster(2);
        q.add(4);
        assertEquals(3, (int) q.take());
        assertEquals(2, (int) q.take());
        assertEquals(1, (int) q.take());
        assertEquals(4, (int) q.poll());
        assertNull(q.poll());
    }
} //~
