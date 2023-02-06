package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;

public class NewCpmBannerInfo extends NewBannerInfo {

    private Creative creative;

    public Creative getCreative() {
        return creative;
    }

    public NewCpmBannerInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    @Override
    public NewCpmBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewCpmBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewCpmBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewCpmBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
