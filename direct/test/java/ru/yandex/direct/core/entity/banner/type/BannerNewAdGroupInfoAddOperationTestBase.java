package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo;
import ru.yandex.direct.dbutil.model.ClientId;

public class BannerNewAdGroupInfoAddOperationTestBase extends BannerAddOperationTestBase {

    protected AdGroupInfo adGroupInfo;

    @Override
    int getShard() {
        return adGroupInfo.getShard();
    }

    @Override
    ClientId getClientId() {
        return adGroupInfo.getClientId();
    }

    @Override
    Long getUid() {
        return adGroupInfo.getUid();
    }
}
