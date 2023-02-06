package ru.yandex.autotests.direct.cmd.data.clients;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;


public class SettingsModel extends BasicDirectRequest {

    @SerializeKey("email")
    private String email;

    @SerializeKey("fio")
    private String fio;

    @SerializeKey("phone")
    private String phone;

    @SerializeKey("news")
    private String news;

    @SerializeKey("tags_allowed")
    @SerializedName("tags_allowed")
    private TagsAllowedEnum tagsAllowed;

    @SerializeKey("warn")
    private String warn;

    @SerializeKey("email_lang")
    @SerializedName("email_lang")
    private String emailLang;

    @SerializeKey("show_market_rating")
    @SerializedName("show_market_rating")
    private String showMarketRating;

    @SerializeKey("text_autocorrection")
    @SerializedName("text_autocorrection")
    private String textAutocorrection;

    @SerializeKey("auto_video")
    @SerializedName("auto_video")
    private String autoVideo;

    @SerializeKey("is_agreed_on_creatives_autogeneration")
    @SerializedName("is_agreed_on_creatives_autogeneration")
    private Integer isAgreedOnCreativesAutogeneration;

    @SerializeKey("client")
    @SerializedName("client")
    private Client client;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Integer getIsAgreedOnCreativesAutogeneration() {
        return isAgreedOnCreativesAutogeneration;
    }

    public SettingsModel withIsAgreedOnCreativesAutogeneration(Integer isAgreedOnCreativesAutogeneration) {
        this.isAgreedOnCreativesAutogeneration = isAgreedOnCreativesAutogeneration;
        return this;
    }

    public String getAutoVideo() {
        return autoVideo;
    }

    public void setAutoVideo(String autoVideo) {
        this.autoVideo = autoVideo;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getNews() {
        return news;
    }

    public void setNews(String news) {
        this.news = news;
    }

    public TagsAllowedEnum getTagsAllowed() {
        return tagsAllowed;
    }

    public SettingsModel withTagsAllowed(TagsAllowedEnum tagsAllowed) {
        this.tagsAllowed = tagsAllowed;
        return this;
    }

    public String getWarn() {
        return warn;
    }

    public void setWarn(String warn) {
        this.warn = warn;
    }

    public String getEmailLang() {
        return emailLang;
    }

    public void setEmailLang(String emailLang) {
        this.emailLang = emailLang;
    }

    public String getShowMarketRating() {
        return showMarketRating;
    }

    public void setShowMarketRating(String showMarketRating) {
        this.showMarketRating = showMarketRating;
    }

    public String getTextAutocorrection() {
        return textAutocorrection;
    }

    public void setTextAutocorrection(String textAutocorrection) {
        this.textAutocorrection = textAutocorrection;
    }

    public SettingsModel withEmail(String email) {
        this.email = email;
        return this;
    }

    public SettingsModel withFio(String fio) {
        this.fio = fio;
        return this;
    }

    public SettingsModel withPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public SettingsModel withNews(String news) {
        this.news = news;
        return this;
    }

    public SettingsModel withWarn(String warn) {
        this.warn = warn;
        return this;
    }

    public SettingsModel withEmailLang(String emailLang) {
        this.emailLang = emailLang;
        return this;
    }

    public SettingsModel withShowMarketRating(String showMarketRating) {
        this.showMarketRating = showMarketRating;
        return this;
    }

    public SettingsModel withTextAutocorrection(String textAutocorrection) {
        this.textAutocorrection = textAutocorrection;
        return this;
    }

    public SettingsModel withClient(Client client) {
        this.client = client;
        return this;
    }

    public SettingsModel withAutoVideo(String autoVideo) {
        this.autoVideo = autoVideo;
        return this;
    }
}
