package ru.yandex.autotests.direct.cmd.data.campaigns;

import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class SendMediaCampaignRequest {

    @SerializeKey("ulogin")
    private String ulogin;

    @SerializeKey("cid")
    private Long cid;

    @SerializeKey("mgid")
    private Long mgid;

    @SerializeKey("tab")
    private String tab;

    public String getUlogin() {
        return ulogin;
    }

    public SendMediaCampaignRequest withUlogin(String ulogin) {
        this.ulogin = ulogin;
        return this;
    }

    public Long getCid() {
        return cid;
    }

    public SendMediaCampaignRequest withCid(Long cid) {
        this.cid = cid;
        return this;
    }

    public Long getMgid() {
        return mgid;
    }

    public SendMediaCampaignRequest withMgid(Long mgid) {
        this.mgid = mgid;
        return this;
    }

    public String getTab() {
        return tab;
    }

    public SendMediaCampaignRequest withTab(String tab) {
        this.tab = tab;
        return this;
    }
}
