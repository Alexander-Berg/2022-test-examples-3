package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.dbutil.model.ClientId;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class IdmGroupRoleInfo {

    private IdmGroup idmGroup;
    private IdmGroupRole idmGroupRole;
    private ClientInfo clientInfo;


    public IdmGroupRoleInfo withIdmGroupRole(IdmGroupRole idmGroupRole) {
        this.idmGroupRole = idmGroupRole;
        return this;
    }

    public IdmGroup getIdmGroup() {
        return idmGroup;
    }

    public IdmGroupRoleInfo withIdmGroup(IdmGroup idmGroup) {
        this.idmGroup = idmGroup;
        return this;
    }

    public Long getIdmGroupId() {
        return getIdmGroup().getIdmGroupId();
    }

    public ClientId getClientId() {
        return idmGroupRole.getClientId();
    }

    public IdmGroupRole getIdmGroupRole() {
        return idmGroupRole;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public IdmGroupRoleInfo withClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        return this;
    }

    public Integer getShard() {
        return clientInfo.getShard();
    }


}
