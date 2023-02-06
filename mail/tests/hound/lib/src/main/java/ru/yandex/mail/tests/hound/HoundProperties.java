package ru.yandex.mail.tests.hound;

import gumi.builders.UrlBuilder;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;


@Resource.File("hound.properties-devpack")
@Resource.Classpath("hound.properties-${system.testing.scope}")
public class HoundProperties {
    private static HoundProperties instance = null;

    public static HoundProperties properties() {
        if (instance == null) {
            instance = new HoundProperties();
        }
        return instance;
    }

    @Property("hound.host")
    private String houndHost = "http://meta-test.mail.yandex.net";

    @Property("hound.port")
    private int houndPort = 80;

    private HoundProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    public String houndUri() {
        UrlBuilder url = UrlBuilder.fromString(houndHost);
        if (80 != houndPort && url.scheme.equals("http")) {
            url = url.withPort(houndPort);
        }

        return url.toString();
    }
}

