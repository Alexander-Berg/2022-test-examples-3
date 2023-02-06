package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.dbutil.model.ClientId;

public class NewBannerInfo {

    private AdGroupInfo adGroupInfo = new AdGroupInfo();
    private Banner banner;

    @SuppressWarnings("unchecked")
    public <B extends Banner> B getBanner() {
        return (B) banner;
    }

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public NewBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getAdGroupInfo().getCampaignInfo();
    }

    public NewBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        getAdGroupInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public NewBannerInfo withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public NewBannerInfo withBanner(Banner banner) {
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
