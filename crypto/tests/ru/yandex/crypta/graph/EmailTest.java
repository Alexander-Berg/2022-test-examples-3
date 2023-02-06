package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class EmailTest {
    @Test
    public void testIsValid() {
        Email test1 = new Email("");
        Email test2 = new Email("a@a.a");
        Email test3 = new Email("wdqwdqwdq");
        Email test4 = new Email(".@a.ru");
        Email test5 = new Email("test@yandex-team.ru");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "a@a.a");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "wdqwdqwdq");
        assertEquals(test3.isValid(), false);
        assertEquals(test4.getValue(), ".@a.ru");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "test@yandex-team.ru");
        assertEquals(test5.isValid(), true);
    }

    @Test
    public void testNormalize() {
        Email test1 = new Email("andrey@yandex.ru");    //not correct, empty
        Email test2 = new Email("andrey@yandex.com");   //correct
        Email test3 = new Email("andrey@01yandex.kz");  //not correct
        Email test4 = new Email(".@a.ru");              //not correct
        Email test5 = new Email("test@yandex-team.ru"); //correct
        Email test6 = new Email("yebok88@yandex.ri");   //correct
        Email test7 = new Email("test@googlemail.com"); //correct

        assertEquals(test1.getNormalizedValue(), "andrey@yandex.ru");
        assertEquals(test2.getNormalizedValue(), "andrey@yandex.ru");
        assertEquals(test3.getNormalizedValue(), "andrey@01yandex.kz");
        assertEquals(test4.getNormalizedValue(), ".@a.ru");
        assertEquals(test5.getNormalizedValue(), "test@yandex-team.ru");
        assertEquals(test6.getNormalizedValue(), "yebok88@yandex.ri");
        assertEquals(test7.getNormalizedValue(), "test@gmail.com");
    }

    @Test
    public void testHash() {
        Email test1 = new Email("hasanurhan65@yandex.ru");
        Email test2 = new Email("hasanurhan@yandex.ru");
        Email test3 = new Email("hasanuri@gmail.com");

        assertEquals(test1.getMd5(), "2194c06f085b5f489d9956b4517f7f39");
        assertEquals(test2.getMd5(), "340b88a9f589e9301af2b3b256513086");
        assertEquals(test3.getMd5(), "2722ba0a82ba5bf6c6b1af66096bd16d");

        assertEquals(
            test1.getSha256(),
            "142ad76a39223eec4d8a3ad6cc8641560801ceb22ef20cd7d9eda03d83316a70");
        assertEquals(
            test2.getSha256(),
            "5479e8f6b12d8e00c50893e2d2e0af1bc112555569adeabb2fe946d473967f28");
        assertEquals(
            test3.getSha256(),
            "104f2127f4297f34fb2d7a4b7a13cf4096d7c8b81c6c89f6935768c9faf0e8d1");
    }

    @Test
    public void testgetType() {
        Email test = new Email("");
        assertEquals(test.getType(), EIdType.EMAIL);
    }
}
