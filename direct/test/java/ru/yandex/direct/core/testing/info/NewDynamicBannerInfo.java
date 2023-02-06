package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;

public class NewDynamicBannerInfo extends NewBannerInfo {

    @Override
    public NewDynamicBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewDynamicBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewDynamicBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewDynamicBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
