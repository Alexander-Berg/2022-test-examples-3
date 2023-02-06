package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;

public class NewCpmIndoorBannerInfo extends NewBannerInfo {

    private Creative creative;

    public Creative getCreative() {
        return creative;
    }

    public NewCpmIndoorBannerInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    @Override
    public NewCpmIndoorBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewCpmIndoorBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewCpmIndoorBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewCpmIndoorBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
