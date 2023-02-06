package ru.yandex.autotests.innerpochta.util;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Created by kurau on 20.02.14.
 */
public class Signature {

    private String text;
    private List<String> emails = new ArrayList<String>();
    private String lang;
    private boolean isDefault;


    public Signature() {
        this.text = Utils.getRandomString();
        this.lang = "ru";
        this.isDefault = false;
    }

    private Signature(String text) {
        this.text = text;
        this.lang = "ru";
        this.isDefault = false;
    }

    public static Signature sign(String text) {
        return new Signature(text);
    }

    public String text() {
        return text;
    }

    public List<String> emails() {
        return emails;
    }

    public String lang() {
        return lang;
    }


    public boolean isDefault() { return isDefault; }

    public Signature mail(String mail) {
        this.emails.add(mail);
        return this;
    }

    public Signature text(String text) {
        this.text = text;
        return this;
    }

    public Signature lang(String lang) {
        this.lang = lang;
        return this;
    }

    public Signature isDefault(boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    @Override
    public String toString() {
        return format("[>Text: %s >Mail: %s >Lang: %s >isDefault: %s]", text, emails, lang, isDefault);
    }
}
