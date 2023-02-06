package ru.yandex.direct.core.testing.data;

import java.net.MalformedURLException;
import java.net.URL;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.utils.io.RuntimeIoException;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class TestMobileApps {

    public static final String DEFAULT_STORE_URL = "https://play.google.com/store/apps/details?id=com.ya.test";

    public static MobileApp defaultMobileApp() {
        return defaultMobileApp(null);
    }

    public static MobileApp defaultMobileApp(String storeContentId) {
        return new MobileApp()
                .withName(storeContentId)
                .withStoreType(MobileAppStoreType.GOOGLEPLAYSTORE)
                .withStoreHref(DEFAULT_STORE_URL)
                .withDisplayedAttributes(emptySet())
                .withTrackers(emptyList())
                .withDomain(getUrlHost(DEFAULT_STORE_URL));
    }

    public static String getUrlHost(String storeUrl) {
        final String domain;
        try {
            domain = new URL(storeUrl).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeIoException(e);
        }
        return domain;
    }
}
