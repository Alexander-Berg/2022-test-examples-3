package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.dbutil.model.ClientId;

public class DealInfo {

    private ClientInfo clientInfo = new ClientInfo();
    private Deal deal;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public DealInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public Deal getDeal() {
        return deal;
    }

    public DealInfo withDeal(Deal deal) {
        this.deal = deal;
        return this;
    }

    public Long getDealId() {
        return getDeal().getId();
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
