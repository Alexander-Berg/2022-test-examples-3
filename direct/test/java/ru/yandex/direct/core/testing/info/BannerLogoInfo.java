package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.dbutil.model.ClientId;

public class BannerLogoInfo<BI extends AbstractBannerInfo<? extends OldBanner>> {

    private BI bannerInfo;
    private BannerImageFormat bannerImageFormat;

    public BannerLogoInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
    }

    public BI getBannerInfo() {
        return bannerInfo;
    }

    public BannerLogoInfo<BI> withBannerInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getBannerInfo().getAdGroupInfo().getCampaignInfo();
    }

    public BannerLogoInfo<BI> withCampaignInfo(CampaignInfo campaignInfo) {
        getBannerInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public BannerLogoInfo<BI> withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public BannerImageFormat getBannerImageFormat() {
        return bannerImageFormat;
    }

    public BannerLogoInfo<BI> withBannerImageFormat(BannerImageFormat bannerImageFormat) {
        this.bannerImageFormat = bannerImageFormat;
        return this;
    }

    public Long getCampaignId() {
        return getCampaignInfo().getCampaignId();
    }

    public Long getUid() {
        return getCampaignInfo().getUid();
    }

    public ClientId getClientId() {
        return getCampaignInfo().getClientId();
    }

    public Integer getShard() {
        return getCampaignInfo().getShard();
    }
}
