package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmOutdoorBanner;

public class CpmOutdoorBannerInfo extends AbstractBannerInfo<OldCpmOutdoorBanner> {

    @Override
    public CpmOutdoorBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpmOutdoorBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpmOutdoorBannerInfo withBanner(OldCpmOutdoorBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpmOutdoorBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
