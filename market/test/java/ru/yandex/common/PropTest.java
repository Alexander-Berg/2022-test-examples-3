package ru.yandex.common;

import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * @author lvovich
 */
public class PropTest extends TestCase {

    public void test() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("ABC", "1.234");
        properties.setProperty("DEF", "12|34");
        properties.store(System.out, null);
        System.out.println("===");
        properties.store(System.out, "this is a comment");
    }

    public void testFormat() {
        System.out.println(String.format(Locale.US, "%.2f", 0.21));
        System.out.println(String.format(Locale.US, "%.2f", 0.021));
        System.out.println(String.format(Locale.US, "%.2f", 0.0021));
        System.out.println(String.format(Locale.US, "%.2f", 12.21));
        System.out.println(String.format(Locale.US, "%.2f", 12345678901234.21));
    }
}
