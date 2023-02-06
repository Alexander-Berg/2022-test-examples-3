package ru.yandex.direct.core.testing.info;


import ru.yandex.direct.core.entity.banner.model.old.OldInternalBanner;

public class InternalBannerInfo extends AbstractBannerInfo<OldInternalBanner> {

    @Override
    public InternalBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public InternalBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public InternalBannerInfo withBanner(OldInternalBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public InternalBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
