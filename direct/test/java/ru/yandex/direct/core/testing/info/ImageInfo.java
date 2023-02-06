package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.Image;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithImage;
import ru.yandex.direct.dbutil.model.ClientId;

public class ImageInfo<BI extends AbstractBannerInfo<? extends OldBannerWithImage>> {

    private BI bannerInfo;
    private Image image;

    public ImageInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
    }

    public BI getBannerInfo() {
        return bannerInfo;
    }

    public ImageInfo<BI> withBannerInfo(BI bannerInfo) {
        this.bannerInfo = bannerInfo;
        return this;
    }

    public CampaignInfo getCampaignInfo() {
        return getBannerInfo().getAdGroupInfo().getCampaignInfo();
    }

    public ImageInfo<BI> withCampaignInfo(CampaignInfo campaignInfo) {
        getBannerInfo().withCampaignInfo(campaignInfo);
        return this;
    }

    public ClientInfo getClientInfo() {
        return getCampaignInfo().getClientInfo();
    }

    public ImageInfo<BI> withClientInfo(ClientInfo clientInfo) {
        getCampaignInfo().withClientInfo(clientInfo);
        return this;
    }

    public Image getImage() {
        return image;
    }

    public ImageInfo<BI> withImage(Image image) {
        this.image = image;
        return this;
    }

    public Long getBannerImageId() {
        return getImage().getId();
    }

    public Long getCampaignId() {
        return getCampaignInfo().getCampaignId();
    }

    public Long getUid() {
        return getCampaignInfo().getUid();
    }

    public ClientId getClientId() {
        return getCampaignInfo().getClientId();
    }

    public Integer getShard() {
        return getCampaignInfo().getShard();
    }
}
