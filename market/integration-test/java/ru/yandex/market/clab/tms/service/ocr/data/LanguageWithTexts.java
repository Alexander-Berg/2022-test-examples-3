package ru.yandex.market.clab.tms.service.ocr.data;

import java.util.List;

public class LanguageWithTexts {
    private String lang;
    private List<Text> texts;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public List<Text> getTexts() {
        return texts;
    }

    public void setTexts(List<Text> texts) {
        this.texts = texts;
    }
}
