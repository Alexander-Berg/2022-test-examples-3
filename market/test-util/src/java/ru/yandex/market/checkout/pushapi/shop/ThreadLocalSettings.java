package ru.yandex.market.checkout.pushapi.shop;

import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;

/**
 * @author msavelyev
 */
public class ThreadLocalSettings {

    private static final ThreadLocal<Settings> threadLocal = new ThreadLocal<>();

    public static Settings getSettings() {
        return threadLocal.get();
    }

    public static void setSettings(Settings newShopId) {
        threadLocal.set(newShopId);
    }
}
