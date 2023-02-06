package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by aleran on 15.09.2015.
 */
public class MobileAppBannerCmdBean {

    @JsonPath(responsePath = "bid", requestPath = "bid")
    private String bannerID;

    @JsonPath(responsePath = "title", requestPath = "title")
    private String title;

    @JsonPath(responsePath = "body", requestPath = "body")
    private String body;

    @JsonPath(responsePath = "href", requestPath = "href")
    private String href;

    @JsonPath(responsePath = "reflected_attrs", requestPath = "reflected_attrs")
    private List<String> reflectedAttrs;

    @JsonPath(responsePath = "url_protocol", requestPath = "url_protocol")
    private String urlProtocol;

    @JsonPath(responsePath = "sitelinks", requestPath = "sitelinks")
    private List<SiteLinksCmdBean> sitelinks;

    @JsonPath(responsePath = "statusShow", requestPath = "statusShow")
    private String statusShow;

    @JsonPath(responsePath = "statusModerate", requestPath = "statusModerate")
    private String statusBannerModerate;

    @JsonPath(responsePath = "banner_type", requestPath = "banner_type")
    private String bannerType;

    @JsonPath(responsePath = "hash_flags", requestPath = "hash_flags")
    private HashFlags hashFlags;

    public String getBannerID() {
        return bannerID;
    }

    public void setBannerID(String bannerID) {
        this.bannerID = bannerID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
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

    public List<SiteLinksCmdBean> getSitelinks() {
        return sitelinks;
    }

    public void setSitelinks(List<SiteLinksCmdBean> sitelinks) {
        this.sitelinks = sitelinks;
    }

    public String getStatusShow() {
        return statusShow;
    }

    public String getStatusBannerModerate() {
        return statusBannerModerate;
    }

    public void setStatusBannerModerate(String statusBannerModerate) {
        this.statusBannerModerate = statusBannerModerate;
    }

    public void setStatusShow(String statusShow) {
        this.statusShow = statusShow;
    }

    public String getBannerType() {
        return bannerType;
    }

    public void setBannerType(String bannerType) {
        this.bannerType = bannerType;
    }
}
