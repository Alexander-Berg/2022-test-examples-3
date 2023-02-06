package ru.yandex.common.util.collections;

import junit.framework.TestCase;

/**
 * Date: 12/9/11
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class InternerTest extends TestCase {

    public void testSimpleIntern() {
        final Interner<String> i = new Interner<String>();

        final String s = new String("aaa");
        assertSame(s, i.interned(s));
        assertEquals("aaa", i.interned(s));
        assertNotSame("aaa", i.interned("aaa"));
        assertEquals(new String("aaa"), i.interned(s));
        final String s1 = new String("aaa");
        assertNotSame(s1, i.interned(s1));

    }
    
    public void testRefFactor() throws Exception {
        final Interner<String> i = new Interner<String>();
        
        assertEquals(0.0d, (double)i.averageRefCount());
        
        i.intern("foo");
        
        assertEquals(1, i.size());
        assertEquals(1.0d, (double)i.averageRefCount());

        i.interned("boo");

        assertEquals(2, i.size());
        assertEquals(1.0d, (double)i.averageRefCount());

        i.interned(new String("boo"));

        assertEquals(2, i.size());
        assertEquals(1.5d, (double)i.averageRefCount());


        i.interned(new String("boo"));
        assertEquals(2, i.size());
        assertEquals(2d, (double)i.averageRefCount());
        
        i.interned("baz");
        assertEquals(3, i.size());
    }


}
