package ru.yandex.mail.tests.sendbernar.models;

import org.apache.commons.lang.StringUtils;
import ru.yandex.qatools.properties.PropertyLoader;
import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.properties.annotations.Resource;
import ru.yandex.qatools.properties.annotations.With;
import ru.yandex.qatools.properties.providers.MapOrSyspropPathReplacerProvider;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 07.09.15
 * Time: 17:08
 */
@Resource.Classpath("remind_msg/letter.${map.lang.value}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class RemindMsgProperties {
    private static RemindMsgProperties instance;

    public static final String SUBJ_PATTERN = "{subj}";

    public static RemindMsgProperties langProps(String lang) {
        if (instance == null || !lang.equals(instance.getLang())) {
            instance = new RemindMsgProperties(lang);
        }
        return instance;
    }

    public RemindMsgProperties(String lang) {
        Properties map = new Properties();
        map.put("lang.value", lang);
        PropertyLoader.populate(this, map);
    }

    @Property("lang.value")
    private String lang = "undefined";

    @Property("subj")
    private String subj = "undefined";

    public String getLang() {
        return lang;
    }

    public String getSubj(String subject) {
        return StringUtils.replace(subj, SUBJ_PATTERN, subject);
    }
}

