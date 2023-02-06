package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.io.InputStream;
import java.util.Properties;

import lombok.SneakyThrows;

public class Urls {

    private static String host;
    private static boolean loaded = false;

    public static synchronized String getMenuPage() {
        if (!loaded) {
            load();
            loaded = true;
        }
        return host + "/ui/";
    }

    public static synchronized String getHostUrl() {
        if (!loaded) {
            load();
            loaded = true;
        }
        return host;
    }

    @SneakyThrows
    public static void load() {
        InputStream propertiesStream = Urls.class.getClassLoader().getResourceAsStream("wms/infor.properties");
        Properties properties = new Properties();
        properties.load(propertiesStream);
        host = (String) properties.get("infor.host");
    }
}
