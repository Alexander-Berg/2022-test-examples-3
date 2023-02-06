package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmAudioBanner;

public class CpmAudioBannerInfo extends AbstractBannerInfo<OldCpmAudioBanner> {

    @Override
    public CpmAudioBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public CpmAudioBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public CpmAudioBannerInfo withBanner(OldCpmAudioBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public CpmAudioBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
