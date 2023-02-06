package ru.yandex.calendar.test;

import org.junit.BeforeClass;

import ru.yandex.calendar.util.conf.Configuration;
import ru.yandex.misc.test.TestBase;

/**
 * The most common test case
 */
public class CalendarTestBase extends TestBase {
    @BeforeClass
    public static void beforeAnyTest() {
        Configuration.initializeEnvironment(true);
    }
}
