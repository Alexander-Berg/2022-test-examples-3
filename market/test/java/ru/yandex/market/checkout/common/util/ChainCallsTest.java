package ru.yandex.market.checkout.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.common.util.ChainCalls.safe;

/**
 * @author sergeykoles
 * Created on: 17.04.18
 */
public class ChainCallsTest {

    @Test
    public void testSafeNull1() {
        TestClass1 obj = null;
        assertNull(ChainCalls.safeNull(obj, TestClass1::getTestClass2));
        obj = new TestClass1(new TestClass2(null, 10), "lalala");
        assertEquals("lalala", ChainCalls.safeNull(obj, TestClass1::getValue));
    }

    @Test
    public void testSafeNull2() {
        TestClass1 obj = null;
        assertNull(ChainCalls.safeNull(obj, TestClass1::getTestClass2, TestClass2::getTestClass3));
        obj = new TestClass1(new TestClass2(null, 10), null);
        assertEquals(10L, (long) ChainCalls.safeNull(obj, TestClass1::getTestClass2, TestClass2::getNum));

    }

    @Test
    public void testSafe4() {
        TestClass1 obj = null;
        String result = safe(obj, TestClass1::getTestClass2,
                TestClass2::getTestClass3, TestClass3::getTestClass1,
                TestClass1::getValue, "I'm default");
        assertEquals("I'm default", result);
    }

    private static class TestClass1 {

        private final TestClass2 testClass2;
        private final String value;

        TestClass1(TestClass2 testClass2, String value) {
            this.testClass2 = testClass2;
            this.value = value;
        }

        public TestClass2 getTestClass2() {
            return testClass2;
        }

        public String getValue() {
            return value;
        }
    }

    private static class TestClass2 {

        private final TestClass3 testClass3;
        private final Integer num;

        TestClass2(TestClass3 testClass3, Integer num) {
            this.testClass3 = testClass3;
            this.num = num;
        }

        public TestClass3 getTestClass3() {
            return testClass3;
        }

        public Integer getNum() {
            return num;
        }
    }

    private static class TestClass3 {

        private final Character character;
        private TestClass1 testClass1;

        TestClass3(Character character) {
            this.character = character;
        }

        public Character getCharacter() {
            return character;
        }

        public TestClass1 getTestClass1() {
            return testClass1;
        }
    }

}
