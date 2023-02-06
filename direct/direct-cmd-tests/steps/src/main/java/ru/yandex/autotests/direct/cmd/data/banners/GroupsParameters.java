package ru.yandex.autotests.direct.cmd.data.banners;

import java.util.Collections;
import java.util.List;

import com.google.gson.GsonBuilder;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ListToCommaSeparatedStringSerializer;
import ru.yandex.autotests.httpclientlite.core.support.gson.NullValuesAdapterFactory;
public class GroupsParameters extends BasicDirectRequest {

    public static GroupsParameters forNewCamp(String client, Long cid, Group group) {
        return forCamp(client, cid, Collections.singletonList(group), "1");
    }

    public static GroupsParameters forNewCamp(String client, Long cid, List<Group> groups) {
        return forCamp(client, cid, groups, "1");
    }

    public static GroupsParameters forExistingCamp(String client, Long cid, Group group) {
        return forExistingCamp(client, cid, Collections.singletonList(group));
    }

    public static GroupsParameters forExistingCamp(String client, Long cid, List<Group> groups) {
        return forCamp(client, cid, groups, "0");
    }

    public static GroupsParameters forCamp(String client, Long cid, List<Group> groups, String newGroup) {
        GroupsParameters groupsParameters = new GroupsParameters();
        groupsParameters.setNewGroup(newGroup);
        groupsParameters.setIsGroupsCopyAction("0");
        groupsParameters.setSaveDraft("0");
        groupsParameters.setCid(String.valueOf(cid));
        groupsParameters.setUlogin(client);
        groupsParameters.setJsonGroups(new GsonBuilder()
                .registerTypeAdapterFactory(new NullValuesAdapterFactory())
                .create().toJson(groups));
        return groupsParameters;
    }


    public static GroupsParameters forCamp(String client, Long cid, Group groups, String newGroup) {
        GroupsParameters groupsParameters = new GroupsParameters();
        groupsParameters.setNewGroup(newGroup);
        groupsParameters.setIsGroupsCopyAction("0");
        groupsParameters.setSaveDraft("0");
        groupsParameters.setCid(String.valueOf(cid));
        groupsParameters.setUlogin(client);
        groupsParameters.setJsonGroups(new GsonBuilder()
                .registerTypeAdapterFactory(new NullValuesAdapterFactory())
                .create().toJson(Collections.singletonList(groups)));
        return groupsParameters;
    }

    @SerializeKey("bids")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> bids;

    @SerializeKey("cid")
    private String cid;

    @SerializeKey("adgroup_ids")
    @SerializeBy(ListToCommaSeparatedStringSerializer.class)
    private List<Long> adgroupIds;

    @SerializeKey("json_groups")
    private String jsonGroups;

    @SerializeKey("camp_banners_domain")
    private String campBannersDomain;

    @SerializeKey("from_newCamp")
    private String fromNewCamp;

    @SerializeKey("new_group")
    private String newGroup;

    @SerializeKey("is_groups_copy_action")
    private String isGroupsCopyAction;

    @SerializeKey("banner_status")
    private String bannerStatus;

    @SerializeKey("is_light")
    private String isLight;

    @SerializeKey("save_draft")
    private String saveDraft;

    @SerializeKey("auto_price")
    private String autoPrice;

    public String getSaveDraft() {
        return saveDraft;
    }

    public void setSaveDraft(String saveDraft) {
        this.saveDraft = saveDraft;
    }

    public List<Long> getBids() {
        return bids;
    }

    public void setBids(List<Long> bids) {
        this.bids = bids;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public List<Long> getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(List<Long> adgroupIds) {
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

    public String getIsLight() {
        return isLight;
    }

    public void setIsLight(String isLight) {
        this.isLight = isLight;
    }

    public GroupsParameters withIsLight(String isLight) {
        this.isLight = isLight;
        return this;
    }

    public String getAutoPrice() {
        return autoPrice;
    }

    public void setAutoPrice(String autoPrice) {
        this.autoPrice = autoPrice;
    }

    public GroupsParameters withAutoPrice(String autoPrice) {
        this.autoPrice = autoPrice;
        return this;
    }
}
