package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;

import static com.google.common.base.Preconditions.checkArgument;

public class PerformanceBannerInfo extends AbstractBannerInfo<OldPerformanceBanner> {

    @Override
    public PerformanceBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        checkArgument(adGroupInfo instanceof PerformanceAdGroupInfo);
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public PerformanceBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public PerformanceBannerInfo withBanner(OldPerformanceBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public PerformanceBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }
}
