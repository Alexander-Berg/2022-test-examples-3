package ru.yandex.autotests.innerpochta.util;

import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;
import ru.yandex.qatools.properties.PropertyLoader;

/**
 * @author cosmopanda
 */
@Resource.Classpath({"webdriver.properties"})
public class ConsumerProps {
    private static ConsumerProps instance;

    public static ConsumerProps consumerProps() {
        if (null == instance) {
            instance = new ConsumerProps();
        }
        return instance;
    }

    private ConsumerProps() {
        PropertyLoader.populate(this);
    }

    @Property("userName")
    private String userName = "mail-qa";

    @Property("password")
    private String password = "selenium";

    public String userName() {
        return this.userName;
    }

    public String password() {
        return this.password;
    }
}