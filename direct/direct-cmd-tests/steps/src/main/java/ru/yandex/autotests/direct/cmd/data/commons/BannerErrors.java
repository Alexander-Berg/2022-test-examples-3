package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BannerErrors {

    @SerializedName("title")
    private List<String> title;

    @SerializedName("body")
    private List<String> body;

    @SerializedName("href")
    private List<String> href;

    @SerializedName("phrases")
    private List<String> phrases;

    @SerializedName("contactinfo")
    private List<String> contactInfo;

    public List<String> getTitle() {
        return title;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public List<String> getBody() {
        return body;
    }

    public void setBody(List<String> body) {
        this.body = body;
    }

    public List<String> getHref() {
        return href;
    }

    public void setHref(List<String> href) {
        this.href = href;
    }

    public List<String> getPhrases() {
        return phrases;
    }

    public void setPhrases(List<String> phrases) {
        this.phrases = phrases;
    }

    public List<String> getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(List<String> contactInfo) {
        this.contactInfo = contactInfo;
    }
}
