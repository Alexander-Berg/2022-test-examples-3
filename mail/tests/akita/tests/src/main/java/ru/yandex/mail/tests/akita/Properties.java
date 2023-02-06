package ru.yandex.mail.tests.akita;

import gumi.builders.UrlBuilder;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;

@Resource.File("akita.properties-devpack")
@Resource.Classpath("akita.properties-${system.testing.scope}")
class Properties {

    @Property("akita.host")
    private String akitaHost = "http://akita-test.mail.yandex.net";

    @Property("akita.port")
    private int akitaPort = 80;

    Properties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    String akitaUri() {
        UrlBuilder url = UrlBuilder.fromString(akitaHost);
        if (80 != akitaPort && url.scheme.equals("http")) {
            url = url.withPort(akitaPort);
        }

        return url.toString();
    }
}
