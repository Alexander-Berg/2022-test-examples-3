package ru.yandex.direct.core.service.urlchecker;

public class UrlToCheckUtils {

    public static UrlToCheck createUrlToCheck(String url) {
        return new UrlToCheck()
                .withUrl(url)
                .withTimeout(1000000L)
                .withRedirectsLimit(2);
    }

}
