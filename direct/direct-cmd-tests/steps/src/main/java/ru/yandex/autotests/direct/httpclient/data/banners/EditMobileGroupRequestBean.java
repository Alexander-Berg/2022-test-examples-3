package ru.yandex.autotests.direct.httpclient.data.banners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by aleran on 29.09.2015.
 */
public class EditMobileGroupRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "adgroup_ids")
    private String adgroupIds;

    @JsonPath(requestPath = "cid")
    private String cid;

    @JsonPath(requestPath = "banner_status")
    private String bannerStatus;

    public String getAdgroupIds() {
        return adgroupIds;
    }

    public void setAdgroupIds(String adgroupIds) {
        this.adgroupIds = adgroupIds;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getBannerStatus() {
        return bannerStatus;
    }

    public void setBannerStatus(String bannerStatus) {
        this.bannerStatus = bannerStatus;
    }
}
