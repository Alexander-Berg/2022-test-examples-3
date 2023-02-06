package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithBannerImage;
import ru.yandex.direct.dbutil.model.ClientId;

public class BannerImageInfo<BI extends AbstractBannerInfo<? extends OldBannerWithBannerImage>> {

    private BI bannerInfo;
    private OldBannerImage bannerImage;
    private BannerImageFormat bannerImageFormat;

    public BannerImageInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
    }

    public BI getBannerInfo() {
        return bannerInfo;
    }

    public BannerImageInfo<BI> withBannerInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getBannerInfo().getAdGroupInfo().getCampaignInfo();
    }

    public BannerImageInfo<BI> withCampaignInfo(CampaignInfo campaignInfo) {
        getBannerInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public BannerImageInfo<BI> withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public BannerImageFormat getBannerImageFormat() {
        return bannerImageFormat;
    }

    public BannerImageInfo<BI> withBannerImageFormat(BannerImageFormat bannerImageFormat) {
        this.bannerImageFormat = bannerImageFormat;
        return this;
    }

    public OldBannerImage getBannerImage() {
        return bannerImage;
    }

    public BannerImageInfo<BI> withBannerImage(OldBannerImage bannerImage) {
        this.bannerImage = bannerImage;
        return this;
    }

    public Long getBannerImageId() {
        return getBannerImage().getId();
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
