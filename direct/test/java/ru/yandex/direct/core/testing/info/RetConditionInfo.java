package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.dbutil.model.ClientId;

public class RetConditionInfo {

    private ClientInfo clientInfo = new ClientInfo();
    private RetargetingCondition retCondition;

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public RetConditionInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public RetargetingCondition getRetCondition() {
        return retCondition;
    }

    public RetConditionInfo withRetCondition(
            RetargetingCondition retCondition) {
        this.retCondition = retCondition;
        return this;
    }

    public Long getRetConditionId() {
        return retCondition.getId();
    }

    public String getRetConditionName() {
        return retCondition.getName();
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
