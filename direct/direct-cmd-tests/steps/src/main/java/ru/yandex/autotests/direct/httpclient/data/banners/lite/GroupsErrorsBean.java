package ru.yandex.autotests.direct.httpclient.data.banners.lite;

import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.List;

/**
 * Created by shmykov on 23.04.15.
 */
public class GroupsErrorsBean {

    @JsonPath(responsePath = "/groups/errors/common")
    private List<String> commonErrors;

    @JsonPath(responsePath = "/groups/errors/group_name")
    private List<String> groupNameErrors;

    @JsonPath(responsePath = "/groups/banners/errors/title")
    private List<String> bannerTitleErrors;

    @JsonPath(responsePath = "/groups/banners/errors/body")
    private List<String> bannerBodyErrors;

    @JsonPath(responsePath = "/groups/banners/errors/href")
    private List<String> bannerHrefErrors;

    @JsonPath(responsePath = "/groups/errors/phrases")
    private List<String> bannerPhraseErrors;

    @JsonPath(responsePath = "/groups/banners/errors/contactinfo")
    private List<String> bannerContactInfoErrors;

    @JsonPath(responsePath = "/groups/errors/store_content_href")
    private List<String> groupStoreContentHref;

    @JsonPath(responsePath = "/groups/errors/network_targeting")
    private List<String> groupNetworkTargeting;

    @JsonPath(responsePath = "/groups/errors/device_type_targeting")
    private List<String> groupDeviceTypeTargeting;

    public List<String> getCommonErrors() {
        return commonErrors;
    }

    public void setCommonErrors(List<String> commonErrors) {
        this.commonErrors = commonErrors;
    }

    public List<String> getBannerTitleErrors() {
        return bannerTitleErrors;
    }

    public void setBannerTitleErrors(List<String> bannerTitleErrors) {
        this.bannerTitleErrors = bannerTitleErrors;
    }

    public List<String> getBannerBodyErrors() {
        return bannerBodyErrors;
    }

    public void setBannerBodyErrors(List<String> bannerBodyErrors) {
        this.bannerBodyErrors = bannerBodyErrors;
    }

    public List<String> getBannerHrefErrors() {
        return bannerHrefErrors;
    }

    public void setBannerHrefErrors(List<String> bannerHrefErrors) {
        this.bannerHrefErrors = bannerHrefErrors;
    }

    public List<String> getBannerPhraseErrors() {
        return bannerPhraseErrors;
    }

    public void setBannerPhraseErrors(List<String> bannerPhraseErrors) {
        this.bannerPhraseErrors = bannerPhraseErrors;
    }

    public List<String> getBannerContactInfoErrors() {
        return bannerContactInfoErrors;
    }

    public void setBannerContactInfoErrors(List<String> bannerContactInfoErrors) {
        this.bannerContactInfoErrors = bannerContactInfoErrors;
    }

    public List<String> getGroupNameErrors() {
        return groupNameErrors;
    }

    public void setGroupNameErrors(List<String> groupNameErrors) {
        this.groupNameErrors = groupNameErrors;
    }

    public List<String> getGroupStoreContentHref() {
        return groupStoreContentHref;
    }

    public void setGroupStoreContentHref(List<String> groupStoreContentHref) {
        this.groupStoreContentHref = groupStoreContentHref;
    }

    public List<String> getGroupNetworkTargeting() {
        return groupNetworkTargeting;
    }

    public void setGroupNetworkTargeting(List<String> groupNetworkTargeting) {
        this.groupNetworkTargeting = groupNetworkTargeting;
    }

    public List<String> getGroupDeviceTypeTargeting() {
        return groupDeviceTypeTargeting;
    }

    public void setGroupDeviceTypeTargeting(List<String> groupDeviceTypeTargeting) {
        this.groupDeviceTypeTargeting = groupDeviceTypeTargeting;
    }
}
