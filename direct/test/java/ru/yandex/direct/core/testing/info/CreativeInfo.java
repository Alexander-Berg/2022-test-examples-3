package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.dbutil.model.ClientId;

public class CreativeInfo {
    private Creative creative;
    private ClientInfo clientInfo;

    public Creative getCreative() {
        return creative;
    }

    public CreativeInfo withCreative(Creative creative) {
        this.creative = creative;
        return this;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public CreativeInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public Integer getShard() {
        return clientInfo.getShard();
    }

    public Long getCreativeId() {
        return creative.getId();
    }

    public ClientId getClientId() {
        return getClientInfo().getClientId();
    }
}
