package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.dbutil.model.ClientId;

public class ClientPrimaryManagerInfo {

    private ClientInfo subjectClientInfo;
    private UserInfo managerInfo;
    private ClientPrimaryManager clientPrimaryManager;

    public ClientInfo getSubjectClientInfo() {
        return subjectClientInfo;
    }

    public ClientPrimaryManagerInfo withSubjectClientInfo(ClientInfo subjectClientInfo) {
        this.subjectClientInfo = subjectClientInfo;
        return this;
    }

    public UserInfo getManagerInfo() {
        return managerInfo;
    }

    public ClientPrimaryManagerInfo withManagerInfo(UserInfo managerInfo) {
        this.managerInfo = managerInfo;
        return this;
    }

    public ClientPrimaryManager getClientPrimaryManager() {
        return clientPrimaryManager;
    }

    public ClientPrimaryManagerInfo withClientPrimaryManager(ClientPrimaryManager clientPrimaryManager) {
        this.clientPrimaryManager = clientPrimaryManager;
        return this;
    }

    public ClientId getSubjectClientId() {
        return clientPrimaryManager.getSubjectClientId();
    }

    public Long getManagerUid() {
        return clientPrimaryManager.getPrimaryManagerUid();
    }

}
