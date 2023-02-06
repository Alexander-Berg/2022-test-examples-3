package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithCreative;
import ru.yandex.direct.core.entity.creative.model.Creative;

public class BannerCreativeInfo<B extends OldBannerWithCreative> {
    private AbstractBannerInfo<B> bannerInfo;
    private CreativeInfo creativeInfo;

    public BannerCreativeInfo<B> withBannerInfo(final AbstractBannerInfo<B> bannerInfo) {
        this.bannerInfo = bannerInfo;
        return this;
    }

    public BannerCreativeInfo<B> withCreativeInfo(final CreativeInfo creativeInfo) {
        this.creativeInfo = creativeInfo;
        return this;
    }

    public Long getBannerId() {
        return bannerInfo.getBannerId();
    }

    public B getBanner() {
        return bannerInfo.getBanner();
    }

    public ClientInfo getClientInfo() {
        return bannerInfo.getClientInfo();
    }

    public AbstractBannerInfo<B> getBannerInfo() {
        return bannerInfo;
    }

    public CreativeInfo getCreativeInfo() {
        return creativeInfo;
    }

    public Long getCreativeId() {
        return creativeInfo.getCreativeId();
    }

    public Creative getCreative() {
        return creativeInfo.getCreative();
    }

    public int getShard() {
        return bannerInfo.getShard();
    }
}
