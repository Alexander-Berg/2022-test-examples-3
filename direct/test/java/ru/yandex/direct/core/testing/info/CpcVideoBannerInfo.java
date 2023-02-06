package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpcVideoBanner;

public class CpcVideoBannerInfo extends AbstractBannerInfo<OldCpcVideoBanner> {

    @Override
    public CpcVideoBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpcVideoBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpcVideoBannerInfo withBanner(OldCpcVideoBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpcVideoBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
