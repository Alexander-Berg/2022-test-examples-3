package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.dbutil.model.ClientId;

public class SitelinkSetInfo {

    private ClientInfo clientInfo = new ClientInfo();
    private SitelinkSet sitelinkSet;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public SitelinkSetInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public SitelinkSet getSitelinkSet() {
        return sitelinkSet;
    }

    public SitelinkSetInfo withSitelinkSet(SitelinkSet sitelinkSet) {
        this.sitelinkSet = sitelinkSet;
        return this;
    }

    public Long getSitelinkSetId() {
        return sitelinkSet.getId();
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
