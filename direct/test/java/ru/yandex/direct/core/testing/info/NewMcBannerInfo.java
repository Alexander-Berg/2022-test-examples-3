package ru.yandex.direct.core.testing.info;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.Banner;

@ParametersAreNonnullByDefault
public class NewMcBannerInfo extends NewBannerInfo {

    private BannerImageFormat imageFormat;

    public BannerImageFormat getImageFormat() {
        return imageFormat;
    }

    public NewMcBannerInfo withImageFormat(BannerImageFormat imageFormat) {
        this.imageFormat = imageFormat;
        return this;
    }

    @Override
    public NewMcBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewMcBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewMcBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewMcBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }

}
