package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class LoginTest {
    @Test
    public void testIsValid() {
        Login test1 = new Login("");
        Login test2 = new Login("  loGin  ");
        Login test3 = new Login(" log.in@ya.ru  ");
        Login test4 = new Login("login@ya@gmail.com");
        Login test5 = new Login("4login");
        Login test6 = new Login(".login");
        Login test7 = new Login("login.");
        Login test8 = new Login("-login");
        Login test9 = new Login("login-");
        Login test10 = new Login("log.in");
        Login test11 = new Login("log.-in");
        Login test12 = new Login("log-.in");
        Login test13 = new Login("log..in");
        Login test14 = new Login("log--in");
        Login test15 = new Login("login@gmail.com");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "  loGin  ");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), " log.in@ya.ru  ");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "login@ya@gmail.com");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "4login");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), ".login");
        assertEquals(test6.isValid(), false);
        assertEquals(test7.getValue(), "login.");
        assertEquals(test7.isValid(), false);
        assertEquals(test8.getValue(), "-login");
        assertEquals(test8.isValid(), false);
        assertEquals(test9.getValue(), "login-");
        assertEquals(test9.isValid(), false);
        assertEquals(test10.getValue(), "log.in");
        assertEquals(test10.isValid(), true);
        assertEquals(test11.getValue(), "log.-in");
        assertEquals(test11.isValid(), false);
        assertEquals(test12.getValue(), "log-.in");
        assertEquals(test12.isValid(), false);
        assertEquals(test13.getValue(), "log..in");
        assertEquals(test13.isValid(), false);
        assertEquals(test14.getValue(), "log--in");
        assertEquals(test14.isValid(), false);
        assertEquals(test15.getValue(), "login@gmail.com");
        assertEquals(test15.isValid(), true);
    }

    @Test
    public void testNormalize() {
        Login test1 = new Login("");
        Login test2 = new Login("  loGin  ");
        Login test3 = new Login(" log.in@ya.ru  ");
        Login test4 = new Login("login@ya@gmail.com");
        Login test5 = new Login("4login");
        Login test6 = new Login(".login");
        Login test7 = new Login("login.");
        Login test8 = new Login("-login");
        Login test9 = new Login("login-");
        Login test10 = new Login("log.in");
        Login test11 = new Login("log.-in");
        Login test12 = new Login("log-.in");
        Login test13 = new Login("log..in");
        Login test14 = new Login("log--in");
        Login test15 = new Login("login@gmail.com");

        assertEquals(test1.getNormalizedValue(), "");
        assertEquals(test2.getNormalizedValue(), "login");
        assertEquals(test3.getNormalizedValue(), "log-in");
        assertEquals(test4.getNormalizedValue(), "login@ya@gmail.com");
        assertEquals(test5.getNormalizedValue(), "4login");
        assertEquals(test6.getNormalizedValue(), ".login");
        assertEquals(test7.getNormalizedValue(), "login.");
        assertEquals(test8.getNormalizedValue(), "-login");
        assertEquals(test9.getNormalizedValue(), "login-");
        assertEquals(test10.getNormalizedValue(), "log-in");
        assertEquals(test11.getNormalizedValue(), "log.-in");
        assertEquals(test12.getNormalizedValue(), "log-.in");
        assertEquals(test13.getNormalizedValue(), "log..in");
        assertEquals(test14.getNormalizedValue(), "log--in");
        assertEquals(test15.getNormalizedValue(), "login@gmail.com");
    }

    @Test
    public void testgetType() {
        Login test = new Login("");
        assertEquals(test.getType(), EIdType.LOGIN);
    }
}
