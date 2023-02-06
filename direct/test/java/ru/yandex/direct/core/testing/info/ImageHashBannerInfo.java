package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldImageHashBanner;

public class ImageHashBannerInfo extends AbstractBannerInfo<OldImageHashBanner> {

    @Override
    public ImageHashBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public ImageHashBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public ImageHashBannerInfo withBanner(OldImageHashBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public ImageHashBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
