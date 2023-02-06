package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;

public class XivaProperties {

    private static XivaProperties instance;

    public static XivaProperties xivaProps() {
        if (null == instance) {
            instance = new XivaProperties();
        }
        return instance;
    }

    private XivaProperties() {
        PropertyLoader.populate(this);
    }

    @Property("xiva.host")
    private String xivaHost = "xiva-qa.mail.yandex.net";

    public String xivaHost() {
        return xivaHost;
    }
}
