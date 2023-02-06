package ru.yandex.autotests.direct.httpclient.util;

import ru.yandex.autotests.direct.httpclient.core.exceptions.HttpClientIOException;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class CommonUtils {
    public static void sleep(long mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            throw new HttpClientIOException("Error while sleep", e);
        }
    }
}
