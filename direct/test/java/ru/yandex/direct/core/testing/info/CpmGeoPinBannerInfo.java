package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmGeoPinBanner;

public class CpmGeoPinBannerInfo extends AbstractBannerInfo<OldCpmGeoPinBanner> {

    @Override
    public CpmGeoPinBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpmGeoPinBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpmGeoPinBannerInfo withBanner(OldCpmGeoPinBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpmGeoPinBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
