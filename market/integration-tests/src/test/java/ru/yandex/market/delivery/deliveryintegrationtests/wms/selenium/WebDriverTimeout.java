package ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium;

import java.util.Properties;

/**
* Класс для единого выставления таймаутов ожидания событий веб-драйвера
* Заданные по умолчанию значения можно переопределить через параметр jvm-args при запуске из командной строки
* Пример синтаксиса параметра:
* --jvm-args "-Dwebdriver.timeout.long=50 -Dwebdriver.timeout.medium=5 -Dwebdriver.timeout.milliseconds.small=300"
*/

public class WebDriverTimeout {
    private static Properties systemProperties = System.getProperties();

    private static boolean isSetInSysProps(String propertyName) {
        return systemProperties.getProperty(propertyName) != null;
    }

    private static int getSystemTimeout(String propertyName) {
        return Integer.valueOf(systemProperties.getProperty(propertyName));
    }

    private static String longTimeout = "webdriver.timeout.long";
    private static String mediumTimeout = "webdriver.timeout.medium";
    private static String smallTimeoutMilliseconds = "webdriver.timeout.milliseconds.small";

    public static final int LONG_WAIT_TIMEOUT = isSetInSysProps(longTimeout) ? getSystemTimeout(longTimeout) : 90;
    public static final int MEDIUM_WAIT_TIMEOUT = isSetInSysProps(mediumTimeout) ? getSystemTimeout(mediumTimeout) : 5;
    public static final int SMALL_WAIT_TIMEOUT_MILLISECONDS = isSetInSysProps(smallTimeoutMilliseconds)
            ? getSystemTimeout(smallTimeoutMilliseconds) : 200;
}
