package ru.yandex.calendar.test;

import ru.yandex.calendar.util.conf.CalendarPropertiesLoader;
import ru.yandex.calendar.util.conf.Configuration;
import ru.yandex.misc.property.PropertiesHolder;

/**
 * @author Stepan Koltsov
 */
public class Developer {

    public static void beforeTest() {
        Configuration.initializeEnvironment(true);

        CalendarPropertiesLoader.loadForTests();
    }

    public static void afterTest() {
        // XXX: restore properties
        PropertiesHolder.reset();
    }

} //~
