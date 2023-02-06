package ru.yandex.direct.core.testing.info;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;

@ParametersAreNonnullByDefault
public class MobileAppBannerInfo extends AbstractBannerInfo<OldMobileAppBanner> {

    @Override
    public MobileAppBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public MobileAppBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public MobileAppBannerInfo withBanner(OldMobileAppBanner banner) {
        super.withBanner(banner);
        return this;
    }

    @Override
    public MobileAppBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

}
