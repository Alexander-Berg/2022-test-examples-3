package ru.yandex.autotests.direct.httpclient.data.clients;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.05.15
 */
public class ModifyUserBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "FIO", responsePath = "user_fio")
    private String fio;

    @JsonPath(requestPath = "email", responsePath = "user_email")
    private String email;

    @JsonPath(requestPath = "save")
    private String save;

    @JsonPath(requestPath = "user_lang", responsePath = "user_lang")
    private String userLang;

    @JsonPath(requestPath = "phone", responsePath = "phone")
    private String phone;

    @JsonPath(requestPath = "captcha_freq")
    private String captchaFreq;

    @JsonPath(requestPath = "autoban_bonus")
    private String autobanBonus;

    @JsonPath(responsePath = "uid")
    private String uid;

    @JsonPath(responsePath = "ClientID")
    private String clientID;

    @JsonPath(responsePath = "country/region_id")
    private String regionId;

    @JsonPath(responsePath = "work_currency")
    private String currency;

    @JsonPath(requestPath = "geo_id", responsePath = "geo_id")
    private String geoId;

    @JsonPath(requestPath = "show_fa_teaser", responsePath = "show_fa_teaser")
    private String showFaTeaser;

    @JsonPath(responsePath = "default_feed_count_limit")
    private String defaultFeedCountLimit;

    @JsonPath(requestPath = "feed_count_limit", responsePath = "feed_count_limit")
    private String feedCountLimit;

    @JsonPath(responsePath = "default_feed_max_file_size")
    private String defaultFeedMaxFileSize;

    @JsonPath(requestPath = "feed_max_file_size", responsePath = "feed_max_file_size")
    private String feedMaxFileSize;


    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSave() {
        return save;
    }

    public void setSave(String save) {
        this.save = save;
    }

    public String getUserLang() {
        return userLang;
    }

    public void setUserLang(String userLang) {
        this.userLang = userLang;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCaptchaFreq() {
        return captchaFreq;
    }

    public void setCaptchaFreq(String captchaFreq) {
        this.captchaFreq = captchaFreq;
    }

    public String getAutobanBonus() {
        return autobanBonus;
    }

    public void setAutobanBonus(String autobanBonus) {
        this.autobanBonus = autobanBonus;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getGeoId() {
        return geoId;
    }

    public void setGeoId(String geoId) {
        this.geoId = geoId;
    }

    public String getShowFaTeaser() {
        return showFaTeaser;
    }

    public void setShowFaTeaser(String showFaTeaser) {
        this.showFaTeaser = showFaTeaser;
    }

    public String getDefaultFeedCountLimit() {
        return defaultFeedCountLimit;
    }

    public void setDefaultFeedCountLimit(String defaultFeedCountLimit) {
        this.defaultFeedCountLimit = defaultFeedCountLimit;
    }

    public String getFeedCountLimit() {
        return feedCountLimit;
    }

    public void setFeedCountLimit(String feedCountLimit) {
        this.feedCountLimit = feedCountLimit;
    }

    public String getDefaultFeedMaxFileSize() {
        return defaultFeedMaxFileSize;
    }

    public void setDefaultFeedMaxFileSize(String defaultFeedMaxFileSize) {
        this.defaultFeedMaxFileSize = defaultFeedMaxFileSize;
    }

    public String getFeedMaxFileSize() {
        return feedMaxFileSize;
    }

    public void setFeedMaxFileSize(String feedMaxFileSize) {
        this.feedMaxFileSize = feedMaxFileSize;
    }
}
