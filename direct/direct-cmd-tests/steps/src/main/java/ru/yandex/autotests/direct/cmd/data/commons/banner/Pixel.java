package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class Pixel {
    @SerializedName("url")
    private String url;

    @SerializedName("provider")
    private String provider;

    @SerializedName("id")
    private String id;

    @SerializedName("kind")
    private String kind;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Pixel withUrl(String url) {
        this.url = url;
        return this;
    }

    public Pixel withProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public Pixel withId(String id) {
        this.id = id;
        return this;
    }

    public Pixel withKind(String kind) {
        this.kind = kind;
        return this;
    }
}
