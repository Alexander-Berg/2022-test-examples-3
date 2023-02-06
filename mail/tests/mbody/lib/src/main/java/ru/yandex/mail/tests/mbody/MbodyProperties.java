package ru.yandex.mail.tests.mbody;

import gumi.builders.UrlBuilder;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;


@Resource.File("mbody.properties-devpack")
@Resource.Classpath("mbody.properties-${system.testing.scope}")
public class MbodyProperties {
    private static MbodyProperties instance = null;

    public static MbodyProperties properties() {
        if (instance == null) {
            instance = new MbodyProperties();
        }
        return instance;
    }

    @Property("mbody.host")
    private String mbodyHost = "http://mbody-test.mail.yandex.net";

    @Property("mbody.port")
    private int mbodyPort = 80;

    private MbodyProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    public String mbodyUri() {
        UrlBuilder url = UrlBuilder.fromString(mbodyHost);
        if (80 != mbodyPort && url.scheme.equals("http")) {
            url = url.withPort(mbodyPort);
        }

        return url.toString();
    }
}

