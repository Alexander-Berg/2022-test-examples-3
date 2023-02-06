package ru.yandex.autotests.innerpochta.util.experiments;

import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

public class ExperimentsConstants {

    public static final Map<String, String> EMPTY_EXP = of(
        "0,0,0",
        "W3siSEFORExFUiI6Ik1BSUwiLCJDT05URVhUIjp7Ik1BSUwiOnsiZmxhZ3MiOlsie30iXX19fV0="
    );

    public static final String EMPTY_EXP_NUM = "12345";

    public static final String CLASSDIALOG_EXP = "\"touch_email_classification\":{\"types\":" +
        "[8,103,100,102,101,4,13,64,65,15,18],\"frequency\":1,\"options\":" +
        "[{\"type\":[4,103],\"title\":\"Classifier_title_person\",\"text\":\"Classifier_subtitle_person\"},{\"type\":" +
        "[13,100],\"title\":\"Classifier_title_subs\",\"text\":\"Classifier_subtitle_subs\"},{\"type\":" +
        "[64,65,102],\"title\":\"Classifier_title_notification\",\"text\":\"Classifier_subtitle_notification\"}," +
        "{\"type\":[15,18,101],\"title\":\"Classifier_title_social\",\"text\":\"Classifier_subtitle_social\"}]}";

    public static final String LK_PROMO = "401926";
    public static final String SETTINGS_POPUP = "358260";
    public static final String OPT_IN = "500413";
    public static final String OPT_IN_PROMO = "428729";
    public static final String REPLY_LATER_EXP = "551459";
}
