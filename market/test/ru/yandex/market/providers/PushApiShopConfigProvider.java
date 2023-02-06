package ru.yandex.market.providers;

import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.shopadminstub.model.PushApiShopConfig;

import java.util.Properties;

public abstract class PushApiShopConfigProvider {

    public static final String DEFAULT_TOKEN = "EB00000195533612";

    public static PushApiShopConfig buildGenerateXML() {
        Properties p = new Properties();
        p.setProperty("generate-data", "auto");
        p.setProperty("random-order-id", "true");
        p.setProperty("token", "0123456789ABCDEF");
        p.setProperty("data-type", DataType.XML.name());
        p.setProperty("auth-type", AuthType.URL.name());
        p.setProperty("price", "250");
        p.setProperty("shop-id", "123456");
        p.setProperty("fastdelivery", "");
        return new PushApiShopConfig(p);
    }

    public static PushApiShopConfig buildNoGenerationXML() {
        Properties properties = new Properties();
        properties.setProperty("token", DEFAULT_TOKEN);
        properties.setProperty("data-type", DataType.XML.name());
        properties.setProperty("auth-type", AuthType.URL.name());
        properties.setProperty("shop-id", "2");
        properties.setProperty("random-order-id", "on");
        properties.setProperty("generate-data", "off");
        return new PushApiShopConfig(properties);
    }

    public static PushApiShopConfig buildInventoryXML() {
        Properties properties = new Properties();
        properties.setProperty("token", DEFAULT_TOKEN);
        properties.setProperty("data-type", DataType.XML.name());
        properties.setProperty("auth-type", AuthType.URL.name());
        properties.setProperty("generate-data", "inventory");
        properties.setProperty("shop-id", "");
        return new PushApiShopConfig(properties);
    }
}
