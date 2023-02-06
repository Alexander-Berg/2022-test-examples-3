package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldImageCreativeBanner;

public class ImageCreativeBannerInfo extends AbstractBannerInfo<OldImageCreativeBanner> {

    @Override
    public ImageCreativeBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public ImageCreativeBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public ImageCreativeBannerInfo withBanner(OldImageCreativeBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public ImageCreativeBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
