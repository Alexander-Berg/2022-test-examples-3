package ru.yandex.autotests.smtpgate.tests.utils;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;


@Resource.Classpath("smtpgate.properties")
public class SmtpgateProperties {

    private SmtpgateProperties() {
        PropertyLoader.populate(this);
    }

    public static SmtpgateProperties smtpgateProps() {
        return new SmtpgateProperties();
    }

    @Property("smtpgate.host")
    private String smtpgateHost = "mxback-qa.mail.yandex.net";

    @Property("smtpgate.port")
    private String smtpgatePort = "2000";

    public String getHost() {
        return smtpgateHost;
    }

    public String getUrl() {
        return String.format("http://%s:%s/", smtpgateHost, smtpgatePort);
    }
}
