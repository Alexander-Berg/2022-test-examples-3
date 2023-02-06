package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;

@Deprecated // используй BannerNewBannerInfoUpdateOperationTestBase
public class BannerOldBannerInfoUpdateOperationTestBase<T extends OldBanner> extends BannerUpdateOperationTestBase {

    public AbstractBannerInfo<? extends T> bannerInfo;

    @Override
    int getShard() {
        return bannerInfo.getShard();
    }

    @Override
    ClientId getClientId() {
        return bannerInfo.getClientId();
    }

    @Override
    Long getUid() {
        return bannerInfo.getUid();
    }
}
