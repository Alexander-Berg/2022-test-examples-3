package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.organization.model.Organization;

public class NewCpmGeoPinBannerInfo extends NewBannerInfo {

    private Creative creative;

    private Organization organization;

    public Organization getOrganization() {
        return organization;
    }

    public NewCpmGeoPinBannerInfo withOrganization(Organization organization) {
        this.organization = organization;
        return this;
    }

    public Creative getCreative() {
        return creative;
    }

    public NewCpmGeoPinBannerInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    @Override
    public NewCpmGeoPinBannerInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        super.withAdGroupInfo(adGroupInfo);
        return this;
    }

    @Override
    public NewCpmGeoPinBannerInfo withCampaignInfo(CampaignInfo campaignInfo) {
        super.withCampaignInfo(campaignInfo);
        return this;
    }

    @Override
    public NewCpmGeoPinBannerInfo withClientInfo(ClientInfo clientInfo) {
        super.withClientInfo(clientInfo);
        return this;
    }

    @Override
    public NewCpmGeoPinBannerInfo withBanner(Banner banner) {
        super.withBanner(banner);
        return this;
    }
}
