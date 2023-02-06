package ru.yandex.market.olap2.load.tasks;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.olap2.load.tasks.AbstractTask.anyCauseIs;

public class AbstractTaskTest {

    @Test
    public void testAnyCauseIs() {
        Exception e = new Exception("1", new Exception("2", new MyErr()));
        assertTrue(anyCauseIs(e, MyErr.class));
        assertTrue(anyCauseIs(e, Exception.class));
        assertTrue(anyCauseIs(new MyErr(), MyErr.class));
        assertFalse(anyCauseIs(e, RuntimeException.class));
    }

    private static class MyErr extends Exception {}
}