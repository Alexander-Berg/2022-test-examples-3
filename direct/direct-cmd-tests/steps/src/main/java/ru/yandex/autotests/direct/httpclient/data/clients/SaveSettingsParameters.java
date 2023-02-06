package ru.yandex.autotests.direct.httpclient.data.clients;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 30.11.14
 */
public class SaveSettingsParameters extends BasicDirectFormParameters {

    @FormParameter("email")
    private String email;

    @FormParameter("email_lang")
    private String emailLang;

    @FormParameter("fio")
    private String fio;

    @FormParameter("news")
    private String news;

    @FormParameter("show_market_rating")
    private Integer showMarketRating;

    @FormParameter("tags_allowed")
    private String tagsAllowed;

    @FormParameter("warn")
    private String warn;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailLang() {
        return emailLang;
    }

    public void setEmailLang(String emailLang) {
        this.emailLang = emailLang;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getNews() {
        return news;
    }

    public void setNews(String news) {
        this.news = news;
    }

    public Integer getShowMarketRating() {
        return showMarketRating;
    }

    public void setShowMarketRating(Integer showMarketRating) {
        this.showMarketRating = showMarketRating;
    }

    public String getTagsAllowed() {
        return tagsAllowed;
    }

    public void setTagsAllowed(String tagsAllowed) {
        this.tagsAllowed = tagsAllowed;
    }

    public String getWarn() {
        return warn;
    }

    public void setWarn(String warn) {
        this.warn = warn;
    }
}
