package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;

public class DynamicBannerInfo extends AbstractBannerInfo<OldDynamicBanner> {

    @Override
    public DynamicBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public DynamicBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public DynamicBannerInfo withBanner(OldDynamicBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public DynamicBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
