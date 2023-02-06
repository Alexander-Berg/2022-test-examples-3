package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.testing.info.banner.BannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;

public class BannerBannerInfoUpdateOperationTestBase extends BannerUpdateOperationTestBase {
    protected BannerInfo bannerInfo;

    @Override
    int getShard() {
        return bannerInfo.getShard();
    }

    @Override
    ClientId getClientId() {
        return bannerInfo.getClientId();
    }

    @Override
    protected Long getUid() {
        return bannerInfo.getUid();
    }
}
