package ru.yandex.autotests.innerpochta.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import ru.yandex.qatools.allure.annotations.Step;


public class AllureLogger {

    private static final Logger logger = LogManager.getLogger(AllureLogger.class);

    @Step("{0}")
    public static void logToAllure(String log) {
        logger.debug("Logged to allure: " + log);
    }
}