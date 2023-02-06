package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;

@Deprecated
//use banner.ContentPromotionBannerInfo
public class NewContentPromoBannerInfo extends NewBannerInfo {

    private ContentPromotionContent content;

    public ContentPromotionContent getContent() {
        return content;
    }

    public NewContentPromoBannerInfo withContent(ContentPromotionContent content) {
        this.content = content;
        return this;
    }

    @Override
    public NewContentPromoBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewContentPromoBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewContentPromoBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewContentPromoBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
