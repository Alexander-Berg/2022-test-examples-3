package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;

public class NewPerformanceMainBannerInfo extends NewBannerInfo {
    @Override
    public NewPerformanceMainBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewPerformanceMainBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewPerformanceMainBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewPerformanceMainBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
