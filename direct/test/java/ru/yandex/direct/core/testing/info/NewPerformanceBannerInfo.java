package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;

public class NewPerformanceBannerInfo extends NewBannerInfo {

    private CreativeInfo creativeInfo = new CreativeInfo();

    public CreativeInfo getCreativeInfo() {
        return creativeInfo;
    }

    public Creative getCreative() {
        return creativeInfo.getCreative();
    }

    public Long getCreativeId() {
        return getCreative().getId();
    }

    public NewPerformanceBannerInfo withCreativeInfo(CreativeInfo creativeInfo) {
        this.creativeInfo = creativeInfo;
        return this;
    }

    @Override
    public NewPerformanceBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewPerformanceBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewPerformanceBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewPerformanceBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
