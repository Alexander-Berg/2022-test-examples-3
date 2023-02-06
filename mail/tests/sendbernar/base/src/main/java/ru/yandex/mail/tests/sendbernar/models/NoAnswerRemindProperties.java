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
 * Date: 14.02.14
 * Time: 19:42
 */
@Resource.Classpath("no_answer/letter.${map.lang.value}.properties")
@With(MapOrSyspropPathReplacerProvider.class)
public class NoAnswerRemindProperties {
    private static NoAnswerRemindProperties instance;

    public static NoAnswerRemindProperties langProps(String lang) {
        if (instance == null || !lang.equals(instance.getLang())) {
            instance = new NoAnswerRemindProperties(lang);
        }
        return instance;
    }

    public NoAnswerRemindProperties(String lang) {
        Properties map = new Properties();
        map.put("lang.value", lang);
        PropertyLoader.populate(this, map);
    }

    @Property("lang.value")
    private String lang = "undefined";

    @Property("text")
    private String text = "undefined";

    public String getLang() {
        return lang;
    }

    public String getText() {
        return text;
    }

    public String getContent(String subj, String date, String email, String mid, String url) {
        String result = StringUtils.replace(text, "{subj}", subj);
        result = StringUtils.replace(result, "{date}", date);
        result = StringUtils.replace(result, "{email}", email);
        result = StringUtils.replace(result, "{mid}", mid);
        result = StringUtils.replace(result, "{verstka_url}", url);
        return result;
    }
}
