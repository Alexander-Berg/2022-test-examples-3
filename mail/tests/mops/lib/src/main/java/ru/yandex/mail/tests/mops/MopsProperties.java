package ru.yandex.mail.tests.mops;

import gumi.builders.UrlBuilder;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;


@Resource.File("mops.properties-devpack")
@Resource.Classpath("mops.properties-${system.testing.scope}")
class MopsProperties {
    private static MopsProperties instance = null;

    public static MopsProperties mopsProperties() {
        if (instance == null) {
            instance = new MopsProperties();
        }
        return instance;
    }

    @Property("mops.host")
    private String mopsHost = "http://mops-test.mail.yandex.net";

    @Property("mops.port")
    private int mopsPort = 80;

    MopsProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    String mopsUri() {
        UrlBuilder url = UrlBuilder.fromString(mopsHost);
        if (80 != mopsPort && url.scheme.equals("http")) {
            url = url.withPort(mopsPort);
        }

        return url.toString();
    }
}
