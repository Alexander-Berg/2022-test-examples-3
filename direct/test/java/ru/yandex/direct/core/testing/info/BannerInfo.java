package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;

public class BannerInfo extends AbstractBannerInfo<OldBanner> {

    @Override
    public BannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public BannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public BannerInfo withBanner(OldBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public BannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
