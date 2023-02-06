package ru.yandex.autotests.direct.httpclient.data.CmdBeans;

import java.util.List;

import ru.yandex.autotests.direct.httpclient.data.adjustment.multiplier_stats.GroupMultiplierStats;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 10.04.15.
 */
public class BannerCmdBean {

    @JsonPath(responsePath = "bid", requestPath = "bid")
    private String bannerID;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(responsePath = "title", requestPath = "title")
    private String title;

    @JsonPath(responsePath = "title_extension", requestPath = "title_extension")
    private String titleExtension;

    @JsonPath(responsePath = "body", requestPath = "body")
    private String text;

    @JsonPath(responsePath = "href", requestPath = "href")
    private String href;

    @JsonPath(responsePath = "domain", requestPath = "domain")
    private String domain;

    @JsonPath(responsePath = "image_source_url", requestPath = "image_source_url")
    private String imageSourceUrl;

    @JsonPath(responsePath = "has_href", requestPath = "has_href")
    private Integer hasHref;

    @JsonPath(responsePath = "url_protocol", requestPath = "url_protocol")
    private String urlProtocol;

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

    @JsonPath(responsePath = "banner_type", requestPath = "banner_type")
    private String bannerType;

    @JsonPath(responsePath = "group_multiplier_stats")
    private GroupMultiplierStats groupMultiplierStats;

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

    public String getTitleExtension() {
        return titleExtension;
    }

    public void setTitleExtension(String titleExtension) {
        this.titleExtension = titleExtension;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public Integer getHasHref() {
        return hasHref;
    }

    public void setHasHref(Integer hasHref) {
        this.hasHref = hasHref;
    }

    public Integer getHasVcard() {
        return hasVcard;
    }

    public void setHasVcard(Integer hasVcard) {
        this.hasVcard = hasVcard;
    }

    public String getBannerType() {
        return bannerType;
    }

    public void setBannerType(String bannerType) {
        this.bannerType = bannerType;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getImageSourceUrl() {
        return imageSourceUrl;
    }

    public void setImageSourceUrl(String imageSourceUrl) {
        this.imageSourceUrl = imageSourceUrl;
    }

    public GroupMultiplierStats getGroupMultiplierStats() {
        return groupMultiplierStats;
    }

    public void setGroupMultiplierStats(GroupMultiplierStats groupMultiplierStats) {
        this.groupMultiplierStats = groupMultiplierStats;
    }
}
