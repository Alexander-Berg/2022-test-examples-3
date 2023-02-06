package ru.yandex.direct.core.testing.info;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.Banner;

@ParametersAreNonnullByDefault
public class NewMobileAppBannerInfo extends NewBannerInfo {

    private BannerImageFormat imageFormat;

    public BannerImageFormat getImageFormat() {
        return imageFormat;
    }

    public NewMobileAppBannerInfo withImageFormat(BannerImageFormat imageFormat) {
        this.imageFormat = imageFormat;
        return this;
    }

    @Override
    public NewMobileAppBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewMobileAppBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewMobileAppBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewMobileAppBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }

}
