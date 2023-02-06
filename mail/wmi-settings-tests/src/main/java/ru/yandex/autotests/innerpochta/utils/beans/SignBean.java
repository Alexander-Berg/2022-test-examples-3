package ru.yandex.autotests.innerpochta.utils.beans;

import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

/**
 * User: lanwen
 * Date: 06.12.13
 * Time: 18:03
 */
public class SignBean {
    @Expose()
    private String text;
    @Expose()
    private Boolean isDefault;
    @Expose()
    private List<String> associatedEmails = new ArrayList<>();
    @Expose(serialize = false)
    private Map<String, String> textTraits = new HashMap<>();
    @Expose()
    private Boolean isSanitize = false;

    public String text() {
        return text;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public List<String> associatedEmailsInBean() {
        return associatedEmails;
    }

    public Map<String, String> textTraits() {
        return textTraits;
    }


    public SignBean text(String text) {
        this.text = text;
        return this;
    }

    public SignBean isDefault(boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public SignBean associatedEmails(String... associatedEmails) {
        this.associatedEmails = asList(associatedEmails);
        return this;
    }

    public boolean isSanitize() {
        return isSanitize;
    }

    public SignBean isSanitize(boolean isSanitize) {
        this.isSanitize = isSanitize;
        return this;
    }

    @Override
    public String toString() {
        return getGson().toJson(this);
    }

    public static SignBean sign(String text) {
        return new SignBean().text(text);
    }

    public static List<SignBean> fromJson(Object json) {
        return getGson().fromJson(json.toString(), new TypeToken<List<SignBean>>() {
        }.getType());
    }

    public static String serialize(SignBean... beans) throws UnsupportedEncodingException {
        return encode(getGson().toJson(Arrays.asList(beans)), UTF_8.toString());
    }

    private static Gson getGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

}
