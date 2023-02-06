package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldContentPromotionBanner;

public class ContentPromotionBannerInfo extends AbstractBannerInfo<OldContentPromotionBanner> {

    @Override
    public ContentPromotionBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public ContentPromotionBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public ContentPromotionBannerInfo withBanner(OldContentPromotionBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public ContentPromotionBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }
}
