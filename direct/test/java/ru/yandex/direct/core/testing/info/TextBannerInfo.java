package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;

public class TextBannerInfo extends AbstractBannerInfo<OldTextBanner> {

    @Override
    public TextBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public TextBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public TextBannerInfo withBanner(OldTextBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public TextBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
