package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;

public class CpmBannerInfo extends AbstractBannerInfo<OldCpmBanner> {

    @Override
    public CpmBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpmBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpmBannerInfo withBanner(OldCpmBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpmBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
