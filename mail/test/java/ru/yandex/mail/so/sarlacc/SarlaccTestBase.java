package ru.yandex.mail.so.sarlacc;

import java.util.Locale;

import org.junit.Assert;

import ru.yandex.http.util.YandexReasonPhraseCatalog;
import ru.yandex.test.util.TestBase;

public class SarlaccTestBase extends TestBase {
    @SuppressWarnings("unused")
    protected static final int BUFFER_SIZE = 1024;
    @SuppressWarnings("unused")
    protected static final long TIMEOUT = 2000L;
    protected static final String STAT = "/stat";
    @SuppressWarnings("unused")
    protected static final String MASS_IN_GET = "/mass-in/get?";
    @SuppressWarnings("unused")
    protected static final String MASS_IN_PUT = "/mass-in/put?";

    @SuppressWarnings("unused")
    public static void assertStatusCode(final int expected, final int status) {
        if (status != expected) {
            String msg = "Expected " + expected + ' '
                + YandexReasonPhraseCatalog.INSTANCE.getReason(expected, Locale.ENGLISH) + " but received " + status
                + ' ' + YandexReasonPhraseCatalog.INSTANCE.getReason(status, Locale.ENGLISH);
            Assert.fail(msg);
        }
    }
}
