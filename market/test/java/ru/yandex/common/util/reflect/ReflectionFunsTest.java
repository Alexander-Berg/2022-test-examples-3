package ru.yandex.common.util.reflect;

import junit.framework.TestCase;

/**
 * User: corvette
 * Date: 12.08.2011
 */
public class ReflectionFunsTest extends TestCase {

    public void testStatInvoker() throws Exception {
        assertEquals("1b", ReflectionFuns.<Long, String>statInvoker(A.class, "name", "b").apply(1L));
    }

    public void testInvoker() throws Exception {
        assertEquals(new Integer("more-more".length()), ReflectionFuns.<A, Integer>invoker("head", "more-more").apply(new A()));
    }

}

class A {
    static String name(Long a, String b) {
        return a + b;
    }

    Integer head(final String field) {
        return field.length();
    }
}
