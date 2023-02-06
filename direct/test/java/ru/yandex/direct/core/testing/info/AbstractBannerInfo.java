package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.dbutil.model.ClientId;

public abstract class AbstractBannerInfo<B extends OldBanner> {

    private AdGroupInfo adGroupInfo = new AdGroupInfo();
    private B banner;

    public boolean isAdGroupExists() {
        return adGroupInfo != null && adGroupInfo.isExists();
    }

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public AbstractBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getAdGroupInfo().getCampaignInfo();
    }

    public AbstractBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        getAdGroupInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public AbstractBannerInfo withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public B getBanner() {
        return banner;
    }

    public AbstractBannerInfo withBanner(B banner) {
        this.banner = banner;
        return this;
    }

    public Long getBannerId() {
        return banner.getId();
    }

    public Long getAdGroupId() {
        return getAdGroupInfo().getAdGroupId();
    }

    public Long getCampaignId() {
        return getAdGroupInfo().getCampaignId();
    }

    public Long getUid() {
        return getAdGroupInfo().getUid();
    }

    public ClientId getClientId() {
        return getAdGroupInfo().getClientId();
    }

    public Integer getShard() {
        return getAdGroupInfo().getShard();
    }

}
