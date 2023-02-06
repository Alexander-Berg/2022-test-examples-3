package ru.yandex.calendar.test;

import org.junit.After;
import org.junit.Before;

/**
 * @author Stepan Koltsov
 */
public class PropertiesAwareCalendarTestBase extends CalendarTestBase {

    @Before
    public void beforePropertiesAwareTest() {
        Developer.beforeTest();
    }

    @After
    public void afterPropertiesAwareTest() {
        Developer.afterTest();
    }

} //~
