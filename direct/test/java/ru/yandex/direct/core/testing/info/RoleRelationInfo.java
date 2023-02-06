package ru.yandex.direct.core.testing.info;

public class RoleRelationInfo {
    private UserInfo operatorUserInfo;
    private UserInfo ownerUserInfo;

    public UserInfo getOperatorUserInfo() {
        return operatorUserInfo;
    }

    public RoleRelationInfo withOperatorInfo(UserInfo operatorUserInfo) {
        this.operatorUserInfo = operatorUserInfo;
        return this;
    }

    public ClientInfo getOperatorClientInfo() {
        return operatorUserInfo.getClientInfo();
    }


    public UserInfo getOwnerUserInfo() {
        return ownerUserInfo;
    }

    public RoleRelationInfo withOwnerInfo(UserInfo ownerUserInfo) {
        this.ownerUserInfo = ownerUserInfo;
        return this;
    }

    public ClientInfo getOwnerClientInfo() {
        return ownerUserInfo.getClientInfo();
    }

}
