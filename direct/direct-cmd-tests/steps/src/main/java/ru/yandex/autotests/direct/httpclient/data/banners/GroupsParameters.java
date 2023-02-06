package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

public class GroupsParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "bids")
    private String bids;
    @JsonPath(requestPath = "cid")
    private String cid;
    @JsonPath(requestPath = "adgroup_ids")
    private String adgroupIds;
    @JsonPath(requestPath = "json_groups")
    private String jsonGroups;
    @JsonPath(requestPath = "camp_banners_domain")
    private String campBannersDomain;
    @JsonPath(requestPath = "from_newCamp")
    private String fromNewCamp;
    @JsonPath(requestPath = "new_group")
    private String newGroup;
    @JsonPath(requestPath = "is_groups_copy_action")
    private String isGroupsCopyAction;
    @JsonPath(requestPath = "banner_status")
    private String bannerStatus;
    @JsonPath(requestPath = "save_draft")
    private String saveDraft;
    @JsonPath(requestPath = "is_light")
    private String isLite;

    public void setSaveDraft(String saveDraft) {
        this.saveDraft = saveDraft;
    }

    public String getSaveDraft() {
        return saveDraft;
    }


    public String getBids() {
        return bids;
    }

    public void setBids(String bids) {
        this.bids = bids;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
    }

    public String getJsonGroups() {
        return jsonGroups;
    }

    public void setJsonGroups(String jsonGroups) {
        this.jsonGroups = jsonGroups;
    }


    public void setCampBannersDomain(String domain) {
        campBannersDomain = domain;
    }

    public String getCampBannersDomain() {
        return campBannersDomain;
    }

    public String getFromNewCamp() {
        return fromNewCamp;
    }

    public void setFromNewCamp(String fromNewCamp) {
        this.fromNewCamp = fromNewCamp;
    }

    public String getNewGroup() {
        return newGroup;
    }

    public void setNewGroup(String newGroup) {
        this.newGroup = newGroup;
    }

    public String getIsGroupsCopyAction() {
        return isGroupsCopyAction;
    }

    public void setIsGroupsCopyAction(String isGroupsCopyAction) {
        this.isGroupsCopyAction = isGroupsCopyAction;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public void setBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
    }

    public String getIsLite() {
        return isLite;
    }

    public void setIsLite(String isLite) {
        this.isLite = isLite;
    }
}
