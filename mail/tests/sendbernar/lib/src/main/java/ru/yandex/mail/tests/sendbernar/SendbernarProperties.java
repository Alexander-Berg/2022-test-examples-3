package ru.yandex.mail.tests.sendbernar;

import gumi.builders.UrlBuilder;
import org.apache.commons.io.FilenameUtils;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;


@lombok.ToString
@Resource.File("sendbernar.properties-${system.testing.scope}")
@Resource.Classpath("sendbernar.properties-${system.testing.scope}")
class SendbernarProperties {
    private static SendbernarProperties instance = null;

    public static SendbernarProperties properties() {
        if (instance == null) {
            instance = new SendbernarProperties();
        }
        return instance;
    }

    @Property("sendbernar.host")
    private String sendbernarHost = "http://iva8-43638c081a03.qloud-c.yandex.net";

    @Property("sendbernar.port")
    private int sendbernarPort = 80;

    @Property("sendbernar.root_path")
    private String sendbernarRootPath = "/home/massaraksh/devpack/sendbernar_testing_9120";

    @Property("webattach.stable")
    private String webattachStable = "http://webattach-test.mail.yandex.net";

    private SendbernarProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);
    }

    public String sendbernarUri() {
        UrlBuilder url = UrlBuilder.fromString(sendbernarHost);
        if (80 != sendbernarPort && url.scheme.equals("http")) {
            url = url.withPort(sendbernarPort);
        }

        return url.toString();
    }

    public String messagePartReal(String sid) {
        return UrlBuilder.fromString(webattachStable)
                .withPath("/message_part_real")
                .addParameter("sid", sid)
                .toString();
    }

    public String messagePartReal() {
        return UrlBuilder.fromString(webattachStable)
                .withPath("/message_part_real")
                .toString();
    }

    public String relativePath(String subpath) {
        return FilenameUtils.concat(sendbernarRootPath, subpath);
    }
}
