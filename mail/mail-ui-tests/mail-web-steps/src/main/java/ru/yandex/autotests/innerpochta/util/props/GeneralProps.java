package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;

/**
 * @author mabelpines
 */
public class GeneralProps {

    public static GeneralProps instance;

    public static GeneralProps generalProps() {
        if (instance == null) {
            instance = new GeneralProps();
        }
        return instance;
    }

    public GeneralProps() {
        PropertyLoader.populate(this);
    }

    @Property("is.local.debug")
    private boolean isLocalDebug = false;

    @Property("domain")
    private String domain = "ru";

    public boolean isLocalDebug() {
        return isLocalDebug;
    }

    public String getDomain() {
        return domain;
    }
}
