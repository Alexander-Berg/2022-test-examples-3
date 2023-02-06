package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by f1nal on 01.07.15
 * TESTIRT-6117.
 */
public class DynamicBannerCmdBean {

    @JsonPath(responsePath = "bid", requestPath = "bid")
    private String bannerID;

    @JsonPath(responsePath = "title", requestPath = "title")
    private String title;

    @JsonPath(responsePath = "body", requestPath = "body")
    private String text;

    @JsonPath(responsePath = "callouts", requestPath = "callouts")
    private List<Object> callouts;

    @JsonPath(responsePath = "image_source_url", requestPath = "image_source_url")
    private String imageSourceUrl;

    @JsonPath(responsePath = "vcard", requestPath = "vcard")
    private ContactInfoCmdBean contactInfo;

    @JsonPath(responsePath = "has_vcard", requestPath = "has_vcard")
    private Integer hasVcard;

    @JsonPath(responsePath = "sitelinks", requestPath = "sitelinks")
    private List<SiteLinksCmdBean> sitelinks;

    @JsonPath(responsePath = "statusShow", requestPath = "statusShow")
    private String statusShow;

    @JsonPath(responsePath = "statusModerate", requestPath = "statusModerate")
    private String statusBannerModerate;

    @JsonPath(responsePath = "image", requestPath = "image")
    private String image;

    @JsonPath(responsePath = "url_protocol", requestPath = "url_protocol")
    private String urlProtocol;

    @JsonPath(responsePath = "href", requestPath = "href")
    private String href;

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ContactInfoCmdBean getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(ContactInfoCmdBean contactInfo) {
        this.contactInfo = contactInfo;
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

    public Integer getHasVcard() {
        return hasVcard;
    }

    public void setHasVcard(Integer hasVcard) {
        this.hasVcard = hasVcard;
    }

    public String getImageSourceUrl() {
        return imageSourceUrl;
    }

    public void setImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUrlProtocol() {
        return urlProtocol;
    }

    public void setUrlProtocol(String urlProtocol) {
        this.urlProtocol = urlProtocol;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public List<Object> getCallouts() {
        return callouts;
    }

    public void setCallouts(List<Object> callouts) {
        this.callouts = callouts;
    }
}
