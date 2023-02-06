package ru.yandex.autotests.direct.cmd.data.commons.banner;

import com.google.gson.annotations.SerializedName;

public class SiteLink {

    public static SiteLink getEmptySiteLink() {
        return new SiteLink().withHref("").withTitle("").withUrlProtocol("http://");
    }

    @SerializedName("title")
    private String title;

    @SerializedName("href")
    private String href;

    @SerializedName("url_protocol")
    private String urlProtocol;

    @SerializedName("turbolanding")
    private TurboLanding turboLanding;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

    public SiteLink withTitle(String title) {
        this.title = title;
        return this;
    }

    public SiteLink withHref(String href) {
        this.href = href;
        return this;
    }

    public SiteLink withUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
        return this;
    }

    public TurboLanding getTurboLanding() {
        return turboLanding;
    }

    public void setTurboLanding(TurboLanding turboLanding) {
        this.turboLanding = turboLanding;
    }

    public SiteLink withTurboLanding(TurboLanding turboLanding) {
        this.turboLanding = turboLanding;
        return this;
    }
}
