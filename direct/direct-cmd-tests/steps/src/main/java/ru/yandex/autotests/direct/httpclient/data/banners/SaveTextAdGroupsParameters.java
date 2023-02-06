package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class SaveTextAdGroupsParameters extends BasicDirectFormParameters {
    @FormParameter("bids")
    private String bids;
    @FormParameter("cid")
    private String cid;
    @FormParameter("from_newCamp")
    private String fromNewCamp;
    @FormParameter("is_copy")
    private String isCopy;
    @FormParameter("new_banner")
    private String newBanner;
    @FormParameter("new_phrases_were_added")
    private String newPhrasesWereAdded;
    @FormParameter("json_groups")
    private String jsonGroups;
    @FormParameter("adgroup_ids")
    private String adGroupIDs;
    @FormParameter("new_group")
    private String newGroup;
    @FormParameter("is_groups_copy_action")
    private String isGroupsCopyAction;

    public List<BannerSaveParameters> getBannerParametersList() {
        if(bannerParametersList == null) {
            bannerParametersList = new ArrayList<BannerSaveParameters>();
        }
        return bannerParametersList;
    }

    private List<BannerSaveParameters> bannerParametersList;

    public void addBanner(BannerSaveParameters banner) {
        getBannerParametersList().add(banner);
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

    public String getFromNewCamp() {
        return fromNewCamp;
    }

    public void setFromNewCamp(String fromNewCamp) {
        this.fromNewCamp = fromNewCamp;
    }

    public String getIsCopy() {
        return isCopy;
    }

    public void setIsCopy(String isCopy) {
        this.isCopy = isCopy;
    }

    public String getNewBanner() {
        return newBanner;
    }

    public void setNewBanner(String newBanner) {
        this.newBanner = newBanner;
    }

    public String getNewPhrasesWereAdded() {
        return newPhrasesWereAdded;
    }

    public void setNewPhrasesWereAdded(String newPhrasesWereAdded) {
        this.newPhrasesWereAdded = newPhrasesWereAdded;
    }

    public String getJsonGroups() {
        return jsonGroups;
    }

    public void setJsonGroups(String jsonGroups) {
        this.jsonGroups = jsonGroups;
    }

    public String getAdGroupIDs() {
        return adGroupIDs;
    }

    public void setAdGroupIDs(String adGroupIDs) {
        this.adGroupIDs = adGroupIDs;
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
}
