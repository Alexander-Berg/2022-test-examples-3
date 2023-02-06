package ru.yandex.common.util.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Created on 11:52:58 19.06.2008
 *
 * @author jkff
 */
public class MapWithDefaultTest {
    private static class StringHolder implements Cloneable {
        String val;

        public StringHolder() {
            val = "dummy";
        }

        public StringHolder(String val) {
            this.val = val;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StringHolder)) return false;

            StringHolder that = (StringHolder) o;

            if (val != null ? !val.equals(that.val) : that.val != null) return false;

            return true;
        }

        public int hashCode() {
            return (val != null ? val.hashCode() : 0);
        }

        @Override protected Object clone() throws CloneNotSupportedException {
            StringHolder res = (StringHolder)super.clone();
            res.val = this.val;
            return res;
        }
    }

    @Test
    public void testSimple() {
        StringHolder h = new StringHolder("a");
        MapWithDefault<String, StringHolder> map = MapWithDefault.simple(h);
        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        assertSame(map.get("x"), h);
        assertSame(map.get("x"), h);
        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("x"));
        assertEquals(0, map.explicitKeyCount());

        StringHolder h2 = new StringHolder("bb");
        map.put("b", h2);
        assertSame(h2, map.get("b"));
        assertSame(h, map.get("x"));
        assertEquals(1, map.explicitKeyCount());
    }

    @Test
    public void testLazyCreate() {
        StringHolder h = new StringHolder("a");
        MapWithDefault<String, StringHolder> map = MapWithDefault.withLazyCreate(h);

        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        assertEquals(0, map.explicitKeyCount());
        assertSame(map.get("x"), h);
        assertEquals(1, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));

        assertTrue(map.containsKey("y"));

        StringHolder h2 = new StringHolder("bb");
        map.put("b", h2);
        assertSame(h2, map.get("b"));
        assertSame(h, map.get("x"));
    }

    @Test
    public void testLazyCloneNotNull() {
        StringHolder h = new StringHolder("a");
        MapWithDefault<String, StringHolder> map = MapWithDefault.withLazyClone(h);

        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        assertEquals(0, map.explicitKeyCount());

        StringHolder x = map.get("x");
        assertNotSame(x, h);
        assertEquals(x, h);
        
        assertEquals(1, map.explicitKeyCount());

        assertTrue(map.containsKey("x"));
        assertSame(map.get("x"), x);

        assertTrue(map.containsKey("y"));
        assertNotSame(map.get("y"), h);
        assertNotSame(map.get("y"), x);
    }

    @Test
    public void testLazyCloneNull() {
        MapWithDefault<String, StringHolder> map = MapWithDefault.withLazyClone(null);
        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        assertNull(map.get("x"));
        assertTrue(map.containsKey("x"));
        assertEquals(1, map.explicitKeyCount());
        assertNull(map.get("x"));
    }

    @Test
    public void testLazyCreateWithConstructor() {
        MapWithDefault<String,StringHolder> map = MapWithDefault.withLazyCreate(StringHolder.class);
        assertEquals(0, map.explicitKeyCount());
        assertTrue(map.containsKey("x"));
        StringHolder h = map.get("x");
        assertNotNull(h);
        assertEquals("dummy", h.val);
        assertTrue(map.containsKey("x"));
        assertEquals(1, map.explicitKeyCount());

        StringHolder h2 = map.get("x");
        assertSame(h, h2);

        StringHolder h3 = map.get("y");
        assertNotSame(h, h3);
        assertEquals("dummy", h3.val);
    }
}
