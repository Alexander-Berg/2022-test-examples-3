package ru.yandex.market.tsum.clients.appmetrica.filters;

import org.junit.Test;

import ru.yandex.market.tsum.clients.appmetrica.Platform;

import static org.junit.Assert.assertEquals;

public class AppVersionFilterTest {

    private static final String ANDROID_EXP = "appVersionDetails=='1.29 (Android)'";
    private static final String IOS_EXP = "appVersionDetails=='1.29 (iOS)'";

    @Test
    public void createAndroid() {
        AppVersionFilter inst = AppVersionFilter.create(Platform.ANDROID, "1.29");
        assertEquals(ANDROID_EXP, inst.toQueryParam());
    }

    @Test
    public void createAndroidWithPlatform() {
        AppVersionFilter inst = AppVersionFilter.create(Platform.ANDROID, "1.29 (Android)");
        assertEquals(ANDROID_EXP, inst.toQueryParam());
    }


    @Test
    public void createIos() {
        AppVersionFilter inst = AppVersionFilter.create(Platform.IOS, "1.29");
        assertEquals(IOS_EXP, inst.toQueryParam());
    }

    @Test
    public void createIosWithPlatform() {
        AppVersionFilter inst = AppVersionFilter.create(Platform.IOS, "1.29 (iOS)");
        assertEquals(IOS_EXP, inst.toQueryParam());
    }
}
