package ru.yandex.autotests.direct.cmd.data.editdynamicadgroups;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class EditDynamicAdGroupsRequest {

    @SerializeKey
    private Long cid;
    @SerializeKey
    private String ulogin;
    @SerializeKey("banner_status")
    private String bannerStatus;
    @SerializeKey("adgroup_ids")
    private String adGroupIds;

    public EditDynamicAdGroupsRequest() {}

    public EditDynamicAdGroupsRequest(Long cid, String ulogin, String bannerStatus, String adGroupIds) {
        this.cid = cid;
        this.ulogin = ulogin;
        this.bannerStatus = bannerStatus;
        this.adGroupIds = adGroupIds;
    }

    public EditDynamicAdGroupsRequest(Long cid, String ulogin) {
        this.cid = cid;
        this.ulogin = ulogin;
    }

    public String getAdGroupIds() {
        return adGroupIds;
    }

    public void setAdGroupIds(String adGroupIds) {
        this.adGroupIds = adGroupIds;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public void setBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
    }

    public String getUlogin() {
        return ulogin;
    }

    public void setUlogin(String ulogin) {
        this.ulogin = ulogin;
    }

    public Long getCid() {
        return cid;
    }

    public void setCid(Long cid) {
        this.cid = cid;
    }

}
