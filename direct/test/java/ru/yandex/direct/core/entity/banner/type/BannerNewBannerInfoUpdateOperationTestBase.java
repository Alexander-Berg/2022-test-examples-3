package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.dbutil.model.ClientId;

@Deprecated
//use BannerBannerInfoUpdateOperationTestBase
public class BannerNewBannerInfoUpdateOperationTestBase extends BannerUpdateOperationTestBase {

    protected NewBannerInfo bannerInfo;

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
