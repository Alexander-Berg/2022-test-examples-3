package ru.yandex.autotests.innerpochta.cal.util.handlers;

import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;

/**
 * @author cosmopanda
 */
@Resource.Classpath("url.properties")
public class HandlerProps {

    private static HandlerProps instance;

    public static HandlerProps handlerProps() {
        if (null == instance) {
            instance = new HandlerProps();
        }
        return instance;
    }

    private HandlerProps() {
        PropertyLoader.populate(this);
    }

    @Property("api.host")
    private String host = "calendar.qa.yandex.ru";

    @Property("api.connection")
    private String connection = "keep-alive";

    public String getHost() {return host; }

    public String getConnection(){ return connection; }
}
