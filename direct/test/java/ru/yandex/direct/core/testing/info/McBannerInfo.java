package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldMcBanner;

public class McBannerInfo extends AbstractBannerInfo<OldMcBanner> {

    @Override
    public McBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public McBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public McBannerInfo withBanner(OldMcBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public McBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }
}
