package ru.yandex.autotests.direct.cmd.data.clients;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class ModifyUserModel extends BasicDirectRequest {

    @SerializeKey("uid")
    @SerializedName("uid")
    private String uid;

    @SerializeKey("ClientID")
    @SerializedName("ClientID")
    private String clientID;

    // = 'yes'
    @SerializeKey("save")
    @SerializedName("save")
    private String save;

    @SerializeKey("FIO")
    @SerializedName("user_fio")
    private String fio;

    @SerializeKey("phone")
    @SerializedName("phone")
    private String phone;

    @SerializeKey("email")
    @SerializedName("user_email")
    private String email;

    // Язык уведомлений = 'ru' || 'en' || 'tr' || 'ua'
    @SerializeKey("user_lang")
    @SerializedName("user_lang")
    private String userLang;

    // Показывать тизер для первой помощи = 'Yes' || null
    @SerializeKey("show_fa_teaser")
    @SerializedName("show_fa_teaser")
    private String showFaTeaser;

    // Настройка таргетинга на устройства = '1' || null
    @SerializeKey("allow_device_targeting")
    @SerializedName("allow_device_targeting")
    private String allowDeviceTargeting;

    // Заблокировать фавиконки на выдаче = '1' || null
    @SerializeKey("is_favicon_blocked")
    @SerializedName("is_favicon_blocked")
    private String isFaviconBlocked;

    // Примечания
    @SerializeKey("description")
    @SerializedName("description")
    private String description;

    // Тестовый пользователь = 1 || null
    @SerializeKey("hidden_status")
    @SerializedName("hidden")
    private String hiddenStatus;

    // Показывать капчу через каждые
    @SerializeKey("captcha_freq")
    @SerializedName("captcha_freq")
    private String captchaFreq;

    // Текущее пороговое число фраз для авто-показа капчи
    @SerializeKey("autoban_bonus")
    @SerializedName("autoban_bonus")
    private String autobanBonus;

    // Заблокирован в интерфейсе = 'Yes' || null
    @SerializeKey("blocked_status")
    @SerializedName("statusBlocked")
    private String blockedStatus;

    // Разрешенные ip
    @SerializeKey("allowed_ips")
    @SerializedName("allowed_ips")
    private String allowedIps;

    // Клиент размещает рекламу Яндекса только в РСЯ = 'Yes' || null
    @SerializeKey("statusYandexAdv")
    @SerializedName("statusYandexAdv")
    private String statusYandexAdv;

    // Клиент размещает рекламу Яндекса только в Яндексе = 'Yes' || null
    @SerializeKey("showOnYandexOnly")
    @SerializedName("showOnYandexOnly")
    private String showOnYandexOnly;

    // Заблокирован по карме Паспорта = 'Yes' || null
    @SerializeKey("is_bad_passport_karma")
    @SerializedName("is_bad_passport_karma")
    private String isBadPassportKarma;

    // Лимит числа объявлений в каждой кампании ClientID
    @SerializeKey("banner_count_limit")
    @SerializedName("banner_count_limit")
    private String bannerCountLimit;

    // Лимит числа ключевых фраз в каждой группе ClientID
    @SerializeKey("keyword_count_limit")
    @SerializedName("keyword_count_limit")
    private String keywordCountLimit;

    // Лимит количества фидов на ClientID
    @SerializeKey("feed_count_limit")
    @SerializedName("feed_count_limit")
    private String feedCountLimit;

    // Максимальное ограничение на размера файла фида (в байтах)
    @SerializeKey("feed_max_file_size")
    @SerializedName("feed_max_file_size")
    private String feedMaxFileSize;

    // Показывать тизер для перевода в валюту = 'on' || null
    @SerializeKey("force_multicurrency_teaser")
    @SerializedName("force_multicurrency_teaser")
    private String forceMulticurrencyTeaser;

    // Разрешить конвертацию без копирования (в рубли) = 'on' || null
    @SerializeKey("modify_convert_allowed")
    @SerializedName("modify_convert_allowed")
    private String modifyConvertAllowed;

    // Запретить пользователю задавать отображаемую ссылку
    @SerializeKey("no_display_hrefs")
    @SerializedName("no_display_hrefs")
    private String noDisplayHrefs;



    @SerializeKey("geo_id")
    private String geoId;

    @SerializedName("work_currency")
    private String currency;

    @SerializedName("default_feed_count_limit")
    private String defaultFeedCountLimit;

    @SerializedName("default_feed_max_file_size")
    private String defaultFeedMaxFileSize;

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

    public String getSave() {
        return save;
    }

    public void setSave(String save) {
        this.save = save;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserLang() {
        return userLang;
    }

    public void setUserLang(String userLang) {
        this.userLang = userLang;
    }

    public String getShowFaTeaser() {
        return showFaTeaser;
    }

    public void setShowFaTeaser(String showFaTeaser) {
        this.showFaTeaser = showFaTeaser;
    }

    public String getAllowDeviceTargeting() {
        return allowDeviceTargeting;
    }

    public void setAllowDeviceTargeting(String allowDeviceTargeting) {
        this.allowDeviceTargeting = allowDeviceTargeting;
    }

    public String getIsFaviconBlocked() {
        return isFaviconBlocked;
    }

    public void setIsFaviconBlocked(String isFaviconBlocked) {
        this.isFaviconBlocked = isFaviconBlocked;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHiddenStatus() {
        return hiddenStatus;
    }

    public void setHiddenStatus(String hiddenStatus) {
        this.hiddenStatus = hiddenStatus;
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

    public String getBlockedStatus() {
        return blockedStatus;
    }

    public void setBlockedStatus(String blockedStatus) {
        this.blockedStatus = blockedStatus;
    }

    public String getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }

    public String getStatusYandexAdv() {
        return statusYandexAdv;
    }

    public void setStatusYandexAdv(String statusYandexAdv) {
        this.statusYandexAdv = statusYandexAdv;
    }

    public String getShowOnYandexOnly() {
        return showOnYandexOnly;
    }

    public void setShowOnYandexOnly(String showOnYandexOnly) {
        this.showOnYandexOnly = showOnYandexOnly;
    }

    public String getIsBadPassportKarma() {
        return isBadPassportKarma;
    }

    public void setIsBadPassportKarma(String isBadPassportKarma) {
        this.isBadPassportKarma = isBadPassportKarma;
    }

    public String getBannerCountLimit() {
        return bannerCountLimit;
    }

    public void setBannerCountLimit(String bannerCountLimit) {
        this.bannerCountLimit = bannerCountLimit;
    }

    public String getKeywordCountLimit() {
        return keywordCountLimit;
    }

    public void setKeywordCountLimit(String keywordCountLimit) {
        this.keywordCountLimit = keywordCountLimit;
    }

    public String getForceMulticurrencyTeaser() {
        return forceMulticurrencyTeaser;
    }

    public void setForceMulticurrencyTeaser(String forceMulticurrencyTeaser) {
        this.forceMulticurrencyTeaser = forceMulticurrencyTeaser;
    }

    public String getModifyConvertAllowed() {
        return modifyConvertAllowed;
    }

    public void setModifyConvertAllowed(String modifyConvertAllowed) {
        this.modifyConvertAllowed = modifyConvertAllowed;
    }

    public String getFeedCountLimit() {
        return feedCountLimit;
    }

    public void setFeedCountLimit(String feedCountLimit) {
        this.feedCountLimit = feedCountLimit;
    }

    public String getFeedMaxFileSize() {
        return feedMaxFileSize;
    }

    public void setFeedMaxFileSize(String feedMaxFileSize) {
        this.feedMaxFileSize = feedMaxFileSize;
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

    public String getDefaultFeedCountLimit() {
        return defaultFeedCountLimit;
    }

    public void setDefaultFeedCountLimit(String defaultFeedCountLimit) {
        this.defaultFeedCountLimit = defaultFeedCountLimit;
    }

    public String getDefaultFeedMaxFileSize() {
        return defaultFeedMaxFileSize;
    }

    public void setDefaultFeedMaxFileSize(String defaultFeedMaxFileSize) {
        this.defaultFeedMaxFileSize = defaultFeedMaxFileSize;
    }

    public String getNoDisplayHrefs() {
        return noDisplayHrefs;
    }

    public void setNoDisplayHrefs(String noDisplayHrefs) {
        this.noDisplayHrefs = noDisplayHrefs;
    }

    public ModifyUserModel withUid(String uid) {
        this.uid = uid;
        return this;
    }

    public ModifyUserModel withClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public ModifyUserModel withSave(String save) {
        this.save = save;
        return this;
    }

    public ModifyUserModel withFio(String fio) {
        this.fio = fio;
        return this;
    }

    public ModifyUserModel withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public ModifyUserModel withEmail(String email) {
        this.email = email;
        return this;
    }

    public ModifyUserModel withUserLang(String userLang) {
        this.userLang = userLang;
        return this;
    }

    public ModifyUserModel withShowFaTeaser(String showFaTeaser) {
        this.showFaTeaser = showFaTeaser;
        return this;
    }

    public ModifyUserModel withAllowDeviceTargeting(String allowDeviceTargeting) {
        this.allowDeviceTargeting = allowDeviceTargeting;
        return this;
    }

    public ModifyUserModel withIsFaviconBlocked(String isFaviconBlocked) {
        this.isFaviconBlocked = isFaviconBlocked;
        return this;
    }

    public ModifyUserModel withDescription(String description) {
        this.description = description;
        return this;
    }

    public ModifyUserModel withHiddenStatus(String hiddenStatus) {
        this.hiddenStatus = hiddenStatus;
        return this;
    }

    public ModifyUserModel withCaptchaFreq(String captchaFreq) {
        this.captchaFreq = captchaFreq;
        return this;
    }

    public ModifyUserModel withAutobanBonus(String autobanBonus) {
        this.autobanBonus = autobanBonus;
        return this;
    }

    public ModifyUserModel withBlockedStatus(String blockedStatus) {
        this.blockedStatus = blockedStatus;
        return this;
    }

    public ModifyUserModel withAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
        return this;
    }

    public ModifyUserModel withStatusYandexAdv(String statusYandexAdv) {
        this.statusYandexAdv = statusYandexAdv;
        return this;
    }

    public ModifyUserModel withShowOnYandexOnly(String showOnYandexOnly) {
        this.showOnYandexOnly = showOnYandexOnly;
        return this;
    }

    public ModifyUserModel withIsBadPassportKarma(String isBadPassportKarma) {
        this.isBadPassportKarma = isBadPassportKarma;
        return this;
    }

    public ModifyUserModel withBannerCountLimit(String bannerCountLimit) {
        this.bannerCountLimit = bannerCountLimit;
        return this;
    }

    public ModifyUserModel withKeywordCountLimit(String keywordCountLimit) {
        this.keywordCountLimit = keywordCountLimit;
        return this;
    }

    public ModifyUserModel withForceMulticurrencyTeaser(String forceMulticurrencyTeaser) {
        this.forceMulticurrencyTeaser = forceMulticurrencyTeaser;
        return this;
    }

    public ModifyUserModel withModifyConvertAllowed(String modifyConvertAllowed) {
        this.modifyConvertAllowed = modifyConvertAllowed;
        return this;
    }

    public ModifyUserModel withFeedCountLimit(String feedCountLimit) {
        this.feedCountLimit = feedCountLimit;
        return this;
    }

    public ModifyUserModel withFeedMaxFileSize(String feedMaxFileSize) {
        this.feedMaxFileSize = feedMaxFileSize;
        return this;
    }

    public ModifyUserModel withCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public ModifyUserModel withGeoId(String geoId) {
        this.geoId = geoId;
        return this;
    }

    public ModifyUserModel withDefaultFeedCountLimit(String defaultFeedCountLimit) {
        this.defaultFeedCountLimit = defaultFeedCountLimit;
        return this;
    }

    public ModifyUserModel withDefaultFeedMaxFileSize(String defaultFeedMaxFileSize) {
        this.defaultFeedMaxFileSize = defaultFeedMaxFileSize;
        return this;
    }

    public ModifyUserModel withNoDisplayHrefs(String noDisplayHrefs) {
        this.noDisplayHrefs = noDisplayHrefs;
        return this;
    }

    public ModifyUserModel toResponse() {
        Gson gson = new Gson();
        ModifyUserModel response = gson.fromJson(gson.toJson(this), ModifyUserModel.class);
        response.
                withSave(null).
                withShowFaTeaser("Yes".equals(showFaTeaser) ? "Yes" : "No").
                withHiddenStatus("1".equals(hiddenStatus) ? "Yes" : "No").
                withCaptchaFreq(captchaFreq == null ? "0" : captchaFreq).
                withAutobanBonus(autobanBonus == null ? "0" : autobanBonus).
                withIsFaviconBlocked("1".equals(isFaviconBlocked)||"Yes".equals(isFaviconBlocked) ? "1" : "0").
                withAllowDeviceTargeting("1".equals(allowDeviceTargeting) ? "1" : "0").
                withBlockedStatus("Yes".equals(blockedStatus) ? "Yes" : "No").
                withAllowedIps(allowedIps == null ? "" : allowedIps).
                withStatusYandexAdv("Yes".equals(statusYandexAdv) ? "Yes" : "No").
                withShowOnYandexOnly("Yes".equals(showOnYandexOnly) ? "Yes" : "No").
                withIsBadPassportKarma("Yes".equals(isBadPassportKarma) ? "1" : "").
                withForceMulticurrencyTeaser("on".equals(forceMulticurrencyTeaser) ? "1" : "0").
                withModifyConvertAllowed("on".equals(modifyConvertAllowed) ? "1" : "0");
        return response;
    }
}
