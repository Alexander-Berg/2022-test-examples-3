package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.dbutil.model.ClientId;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class IdmGroupMemberInfo {

    private IdmGroup idmGroup;
    private IdmGroupMember idmGroupMember;
    private UserInfo userInfo;


    public IdmGroupMemberInfo withIdmGroupMember(IdmGroupMember idmGroupMember) {
        this.idmGroupMember = idmGroupMember;
        return this;
    }

    public IdmGroup getIdmGroup() {
        return idmGroup;
    }

    public IdmGroupMemberInfo withIdmGroup(IdmGroup idmGroup) {
        this.idmGroup = idmGroup;
        return this;
    }

    public Long getIdmGroupId() {
        return getIdmGroup().getIdmGroupId();
    }

    public ClientId getClientId() {
        return idmGroupMember.getClientId();
    }

    public IdmGroupMember getIdmGroupMember() {
        return idmGroupMember;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public IdmGroupMemberInfo withUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        return this;
    }

    public Integer getShard() {
        return userInfo.getShard();
    }

    public ClientInfo getClientInfo() {
        return userInfo.getClientInfo();
    }

    public String getPassportLogin() {
        return idmGroupMember.getLogin();
    }

    public String getDomainLogin() {
        return idmGroupMember.getDomainLogin();
    }

    public Long getUid() {
        return idmGroupMember.getUid();
    }
}
