package ru.yandex.mail.tests.akita;

import gumi.builders.UrlBuilder;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;

@Resource.File("akita.properties-devpack")
@Resource.Classpath("akita.properties-${system.testing.scope}")
public class AkitaProperties {
    private static AkitaProperties instance = null;

    public static AkitaProperties properties() {
        if (instance == null) {
            instance = new AkitaProperties();
        }
        return instance;
    }

    @Property("akita.host")
    private String akitaHost = "http://akita-test.mail.yandex.net";

    @Property("akita.port")
    private int akitaPort = 80;

    private AkitaProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    public String akitaUri() {
        UrlBuilder url = UrlBuilder.fromString(akitaHost);
        if (80 != akitaPort && url.scheme.equals("http")) {
            url = url.withPort(akitaPort);
        }

        return url.toString();
    }
}
