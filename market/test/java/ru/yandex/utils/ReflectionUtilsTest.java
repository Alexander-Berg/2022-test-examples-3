package ru.yandex.utils;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static junit.framework.Assert.*;
import static ru.yandex.utils.IteratorUtils.arr;
import static ru.yandex.utils.ReflectionUtils.*;


public class ReflectionUtilsTest {
    @SuppressWarnings("unchecked")
    @Test
    public void testConstructorFit() {
        assertEquals(int.class, Integer.TYPE);
        assertTrue(isAssignable(Integer.TYPE, Integer.class));
        assertTrue(isAssignable(Integer.class, Integer.TYPE));

        assertEquals(1, constructorFit(arr(Integer.TYPE), getCtor(A.class)));
        assertEquals(0, constructorFit(arr(Integer.class), getCtor(A.class)));
        assertEquals(-1, constructorFit(arr(String.class), getCtor(A.class)));

        assertEquals(2, constructorFit(arr(int.class, int.class), getCtor(A1.class)));
        assertEquals(1, constructorFit(arr(Integer.class, int.class), getCtor(A1.class)));
        assertEquals(0, constructorFit(arr(Integer.class, Integer.class), getCtor(A1.class)));
        assertEquals(-1, constructorFit(arr(Integer.class), getCtor(A1.class)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFindConstructor() throws Exception {
        assertNull(findConstructor(B.class, arr(Double.class, int.class)));
        assertNull(findConstructor(B.class, arr(int.class)));

        assertEquals("Integer,int", create(B.class, arr(int.class, int.class), arr(1, 1)));
        assertEquals("Integer,int", create(B.class, arr(Integer.class, int.class), arr(1, 1)));
        assertEquals("Integer,Integer", create(B.class, arr(int.class, Integer.class), arr(1, 1)));
        assertEquals("Integer,Integer", create(B.class, arr(Integer.class, Integer.class), arr(1, 1)));
        assertEquals("Integer,int", create(B.class, arr(int.class, int.class), arr(1, 1)));
        assertEquals("int,int,int", create(B.class, arr(int.class, int.class, Integer.class), arr(1, 1, 1)));
    }

    public static <T> String create(Class<T> clazz, Class<?>[] params, Object[] args) throws Exception {
        Constructor<T> ctor = findConstructor(clazz, params);
        if (null == ctor) {
            return null;
        }
        return ctor.newInstance(args).toString();
    }

    public static Constructor<?> getCtor(Class<?> clazz) {
        return clazz.getConstructors()[0];
    }

    public static class A {
        public A(int x) {
        }
    }

    public static class A1 {
        public A1(int x, int y) {
        }
    }

    public static class B {
        private String m;

        public B(int x, int y, int z) {
            m = "int,int,int";
        }

        public B(Integer x, int y) {
            m = "Integer,int";
        }

        public B(Integer x, Integer y) {
            m = "Integer,Integer";
        }

        @Override
        public String toString() {
            return m;
        }
    }
}
