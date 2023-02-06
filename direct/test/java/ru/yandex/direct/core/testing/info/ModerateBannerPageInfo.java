package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.banner.model.ModerateBannerPage;
import ru.yandex.direct.dbutil.model.ClientId;

public class ModerateBannerPageInfo {

    private AbstractBannerInfo bannerInfo;

    private Long bannerVersion;

    private ModerateBannerPage moderateBannerPage;

    public AbstractBannerInfo getBannerInfo() {
        return bannerInfo;
    }

    public ModerateBannerPageInfo withBannerInfo(AbstractBannerInfo bannerInfo) {
        this.bannerInfo = bannerInfo;
        return this;
    }

    public Long getBannerVersion() {
        return bannerVersion;
    }

    public ModerateBannerPageInfo withBannerVersion(Long bannerVersion) {
        this.bannerVersion = bannerVersion;
        return this;
    }

    public ModerateBannerPage getModerateBannerPage() {
        return moderateBannerPage;
    }

    public ModerateBannerPageInfo withModerateBannerPage(ModerateBannerPage moderateBannerPage) {
        this.moderateBannerPage = moderateBannerPage;
        return this;
    }

    public Long getModerateBannerPageId() {
        return getModerateBannerPage().getId();
    }

    public ClientInfo getClientInfo() {
        return bannerInfo.getClientInfo();
    }

    public Long getUid() {
        return getClientInfo().getUid();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }

    public Integer getShard() {
        return getClientInfo().getShard();
    }
}
