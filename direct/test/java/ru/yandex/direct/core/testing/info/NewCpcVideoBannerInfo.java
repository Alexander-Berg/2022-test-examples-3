package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;

public class NewCpcVideoBannerInfo extends NewBannerInfo {

    private Creative creative;

    public Creative getCreative() {
        return creative;
    }

    public NewCpcVideoBannerInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    @Override
    public NewCpcVideoBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewCpcVideoBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewCpcVideoBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewCpcVideoBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
