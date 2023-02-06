package ru.yandex.direct.core.testing.info;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;

@ParametersAreNonnullByDefault
public class NewImageBannerInfo extends NewBannerInfo {

    private Creative creative;
    private BannerImageFormat imageFormat;

    public Creative getCreative() {
        return creative;
    }

    public NewImageBannerInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    public BannerImageFormat getImageFormat() {
        return imageFormat;
    }

    public NewImageBannerInfo withImageFormat(BannerImageFormat imageFormat) {
        this.imageFormat = imageFormat;
        return this;
    }

    @Override
    public NewImageBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewImageBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewImageBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewImageBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }

}
