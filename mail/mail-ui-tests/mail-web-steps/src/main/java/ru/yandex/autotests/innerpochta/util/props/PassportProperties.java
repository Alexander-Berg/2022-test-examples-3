package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;

public class PassportProperties {

    private static PassportProperties instance;

    public static PassportProperties passProps() {
        if (null == instance) {
            instance = new PassportProperties();
        }
        return instance;
    }

    private PassportProperties() {
        PropertyLoader.populate(this);
    }

    @Property("use.pasport")
    private Boolean usePassport = false;

    @Property("pasport.host")
    private String pasportHost = "passport-rc";

    public Boolean usePassportHost() {
        return usePassport;
    }

    public String pasportHost() {
        return pasportHost;
    }
}
