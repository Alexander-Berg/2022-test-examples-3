package ru.yandex.market.sdk.userinfo;

/**
 * @authror dimkarp93
 */
public class TestConstants {
    private static final String SBERLOG = "https://sberlog.tst.vs.market.yandex.net";
    private static final String PASSPORT = "https://blackbox-test.aida.yandex.ru";

    private static final int SBERLOG_APP_ID = 2011264;
    private static final int PASSPORT_APP_ID = 224;

    public static final Environment ENVIRONMENT = new Environment(
            SBERLOG,
            SBERLOG_APP_ID,
            PASSPORT,
            PASSPORT_APP_ID
    );
}
