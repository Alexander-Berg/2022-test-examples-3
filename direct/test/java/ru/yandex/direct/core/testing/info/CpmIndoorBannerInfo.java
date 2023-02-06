package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;

public class CpmIndoorBannerInfo extends AbstractBannerInfo<OldCpmIndoorBanner> {

    @Override
    public CpmIndoorBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpmIndoorBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpmIndoorBannerInfo withBanner(OldCpmIndoorBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpmIndoorBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
