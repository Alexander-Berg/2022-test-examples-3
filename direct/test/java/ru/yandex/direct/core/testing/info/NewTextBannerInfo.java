package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;

public class NewTextBannerInfo extends NewBannerInfo {

    private VcardInfo vcardInfo;

    private SitelinkSetInfo sitelinkSetInfo;

    private BannerImageFormatInfo bannerImageFormatInfo;

    public VcardInfo getVcardInfo() {
        return vcardInfo;
    }

    public NewTextBannerInfo withVcardInfo(VcardInfo vcardInfo) {
        this.vcardInfo = vcardInfo;
        return this;
    }

    public SitelinkSetInfo getSitelinkSetInfo() {
        return sitelinkSetInfo;
    }

    public NewTextBannerInfo withSitelinkSetInfo(SitelinkSetInfo sitelinkSetInfo) {
        this.sitelinkSetInfo = sitelinkSetInfo;
        return this;
    }

    public BannerImageFormatInfo getBannerImageFormatInfo() {
        return bannerImageFormatInfo;
    }

    public NewTextBannerInfo withBannerImageFormatInfo(BannerImageFormatInfo bannerImageFormatInfo) {
        this.bannerImageFormatInfo = bannerImageFormatInfo;
        return this;
    }

    @Override
    public NewTextBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewTextBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewTextBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewTextBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
