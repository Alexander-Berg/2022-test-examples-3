package ru.yandex.direct.core.entity.banner.type;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.dbutil.model.ClientId;

public class BannerClientInfoAddOperationTestBase extends BannerAddOperationTestBase {

    protected ClientInfo clientInfo;

    @Override
    int getShard() {
        return clientInfo.getShard();
    }

    @Override
    ClientId getClientId() {
        return clientInfo.getClientId();
    }

    @Override
    Long getUid() {
        return clientInfo.getUid();
    }
}
