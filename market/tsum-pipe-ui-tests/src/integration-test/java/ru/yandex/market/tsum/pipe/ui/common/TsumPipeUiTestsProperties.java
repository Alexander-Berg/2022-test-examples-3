package ru.yandex.market.tsum.pipe.ui.common;

/**
 * Значения по умолчанию подходят для локального запуска из Идеи над локальным ЦУМом и локальным Selenoid'ом.
 *
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 19.02.2018
 */
public class TsumPipeUiTestsProperties {
    private static String OS = System.getProperty("os.name").toLowerCase();

    public static final String SELENIUM_URL = System.getProperty(
        "tsum.selenium.url",
        "http://localhost:4444/wd/hub"
    );

    /**
     * Включает VNC в Selenoid'е.
     * http://aerokube.com/selenoid/latest/#_live_browser_screen_enablevnc
     */
    public static final boolean ENABLE_VNC = Boolean.parseBoolean(System.getProperty(
        "tsum.selenium.enableVNC",
        "true"
    ));

    /**
     * При запуске на локальном Selenoid'е браузер запускается в Docker-контейнере, и не может ходить в localhost:3000.
     * Приходится указывать IP хоста в докерной сети.
     */
    public static final String TSUM_URL = System.getProperty(
        "tsum.main.page.url",
        OS.contains("mac") ? "http://docker.for.mac.localhost:3000" : "http://host.docker.internal:3000"
    );

    /**
     * При запуске на локальном ЦУМе логиниться не нужно.
     */
    public static final String TSUM_TEST_USER_LOGIN = System.getProperty("tsum.test.user.login");
    public static final String TSUM_TEST_USER_SECRET_ID = System.getProperty("tsum.test.user.secretId");

    public static final String BROWSER_NAME = System.getProperty("tsum.test.browser.name", "firefox");
    public static final String BROWSER_VERSION = System.getProperty("tsum.test.browser.version", "57.0");
}
