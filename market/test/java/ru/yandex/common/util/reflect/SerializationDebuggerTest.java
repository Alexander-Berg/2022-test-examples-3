package ru.yandex.common.util.reflect;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Created on 22.08.2007 16:26:18
 *
 * @author Eugene Kirpichov jkff@yandex-team.ru
 */
public class SerializationDebuggerTest {
    @Test
    public void testOneObject() {
        Object a = new Object();
        assertEquals("", new SerializationDebugger().getPathToProhibitedClass(a, new AllowAllClasses()));
        assertEquals("null", new SerializationDebugger().getPathToProhibitedClass(a, new DisallowAllClasses()));
    }

    @Test
    public void testObjectWithField() {
        class X {
            private String value = "Hello";
        }

        X x = new X();

        assertEquals(
            "java.lang.String " +
                "ru.yandex.common.util.reflect.SerializationDebuggerTest::testObjectWithField()::X::value",
            new SerializationDebugger().getPathToProhibitedClass(
                x, new DisallowSpecificClass(String.class)));
    }

    @Test
    public void testChainOfTwo() {
        class X {
            private String value = "Hello";
        }
        class Y {
            private X x = new X();
        }

        Y y = new Y();

        assertEquals("ru.yandex.common.util.reflect.SerializationDebuggerTest::testChainOfTwo()::X " +
            "ru.yandex.common.util.reflect.SerializationDebuggerTest::testChainOfTwo()::Y::x->" +
            "java.lang.String ru.yandex.common.util.reflect.SerializationDebuggerTest::testChainOfTwo()::X::value",
            new SerializationDebugger().getPathToProhibitedClass(
                y, new DisallowSpecificClass(String.class)));
    }

    interface IF {
    }

    @Test
    public void testEnclosingInstance() {
        class Y {
            private IF i = new IF() {
            };

            public IF getIF() {
                return i;
            }
        }

        assertEquals(
            "ru.yandex.common.util.reflect.SerializationDebuggerTest::testEnclosingInstance()::Y " +
                "ru.yandex.common.util.reflect.SerializationDebuggerTest::testEnclosingInstance()::Y::<anonymous>::[enclosing instance]",
            new SerializationDebugger().getPathToProhibitedClass(
                new Y().getIF(), new DisallowSpecificClass(Y.class)));
    }

    @Test
    public void testSelfReferenceDoesNotHang() {
        class X {
            X x;
        }
        X x = new X();
        x.x = x;

        assertEquals("null", new SerializationDebugger().getPathToProhibitedClass(x, new DisallowAllClasses()));
        assertEquals("", new SerializationDebugger().getPathToProhibitedClass(x, new AllowAllClasses()));
    }

    @Test
    public void testArray() {
        class X {
        }

        class Y {
            Object val;
        }

        Y y = new Y();
        Object[] val = new Object[5];
        val[3] = new X();
        y.val = val;

        assertEquals("java.lang.Object ru.yandex.common.util.reflect.SerializationDebuggerTest::testArray()::Y::val[3]",
            new SerializationDebugger().getPathToProhibitedClass(y, new DisallowSpecificClass(X.class)));
    }

    @Test
    public void testMutuallyReferencingArrays() {
        class X {
            Object[] a = new Object[3];
            Object[] b = new Object[3];
        }
        class Y {
            X x;
            String z = "";

            public Y(X x) {
                this.x = x;
            }
        }

        X x = new X();
        x.a[1] = x.b;
        x.a[0] = x.a;
        x.b[1] = new Y(x);
        x.b[2] = x;
        // Does not hang
        assertEquals(
            "java.lang.Object[] ru.yandex.common.util.reflect.SerializationDebuggerTest::testMutuallyReferencingArrays()::X::b[1]->java.lang.String ru.yandex.common.util.reflect.SerializationDebuggerTest::testMutuallyReferencingArrays()::Y::z",
            new SerializationDebugger().getPathToProhibitedClass(x, new DisallowSpecificClass(String.class)));
    }

    @Test
    public void testDisallowByWildcard() {
        assertTrue(new DisallowByWildcard("ru.yandex.*").fits(SerializationDebugger.class));
        assertTrue(new DisallowByWildcard("ru.yandex.*bugger").fits(SerializationDebugger.class));
        assertFalse(new DisallowByWildcard("com.google.*").fits(SerializationDebugger.class));
        assertFalse(new DisallowByWildcard("SerializationDebugger").fits(SerializationDebugger.class));
        assertTrue(new DisallowByWildcard("*SerializationDebugger*").fits(SerializationDebugger.class));
        assertTrue(new DisallowByWildcard("*Seria*bugger*").fits(SerializationDebugger.class));
        assertFalse(new DisallowByWildcard("*Serializati.*nDebugger*").fits(SerializationDebugger.class));
        assertFalse(new DisallowByWildcard("*Serializ.tionDebugger*").fits(SerializationDebugger.class));
    }
}
