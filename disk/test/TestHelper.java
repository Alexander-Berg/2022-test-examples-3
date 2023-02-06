package ru.yandex.chemodan.test;

import ru.yandex.chemodan.log.Log4jHelper;

/**
 * @author dbrylev
 */
public class TestHelper {

    public static void initialize() {
        Log4jHelper.configureTestLogger();
    }
}
