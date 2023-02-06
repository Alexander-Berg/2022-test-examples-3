package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldContentPromotionVideoBanner;

public class ContentPromotionVideoBannerInfo extends AbstractBannerInfo<OldContentPromotionVideoBanner> {

    @Override
    public ContentPromotionVideoBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public ContentPromotionVideoBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public ContentPromotionVideoBannerInfo withBanner(OldContentPromotionVideoBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public ContentPromotionVideoBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }
}
