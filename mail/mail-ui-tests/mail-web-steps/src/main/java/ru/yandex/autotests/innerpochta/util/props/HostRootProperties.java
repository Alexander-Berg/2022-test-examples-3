package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;

public class HostRootProperties {

    private static HostRootProperties instance;

    public static HostRootProperties hostrootProps() {
        if (null == instance) {
            instance = new HostRootProperties();
        }
        return instance;
    }

    private HostRootProperties() {
        PropertyLoader.populate(this);
    }

    @Property("beta.host")
    private String testhost = "mail.yandex.ru";

    @Property("java.io.tmpdir")
    private String tempdir;

    @Property("hostroot.useproxy")
    private Boolean useproxy = false;

    public Boolean useProxy() {
        return useproxy;
    }

    public String testhost() {
        return testhost.replace("https://", "");
    }

    public String getTempdir() {
        return tempdir;
    }

    public void setTestHost(String testhost) {
        this.testhost = testhost;
    }
}
